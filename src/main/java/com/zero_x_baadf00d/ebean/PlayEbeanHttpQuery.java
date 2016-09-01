/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Thibault Meyer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.zero_x_baadf00d.ebean;

import com.avaje.ebean.*;
import com.zero_x_baadf00d.ebean.converter.EbeanTypeConverter;
import org.joda.time.DateTime;
import play.mvc.Http;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * Helper to map flat query strings to Ebean filters.
 *
 * @author Thibault Meyer
 * @version 16.09.01
 * @since 16.04.22
 */
public class PlayEbeanHttpQuery {

    /**
     * Keys to ignore.
     *
     * @since 16.04.28
     */
    private final List<String> ignoredPattern;

    /**
     * Handle to the class loader in use.
     *
     * @since 16.05.05
     */
    private final ClassLoader classLoader;

    /**
     * Build a default instance.
     *
     * @since 16.04.28
     */
    public PlayEbeanHttpQuery() {
        this.classLoader = this.getClass().getClassLoader();
        this.ignoredPattern = new ArrayList<>();
    }

    /**
     * Build an instance with specific class loader.
     *
     * @param classLoader The class loader to use
     * @since 16.05.05
     */
    public PlayEbeanHttpQuery(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.ignoredPattern = new ArrayList<>();
    }

    /**
     * Try to resolve the field object the class and the field name.
     *
     * @param clazz         The class to inspect
     * @param newObjectName The field name to find
     * @return The {@code Field} object
     * @since 16.04.22
     */
    private Field resolveField(final Class<?> clazz, final String newObjectName) {
        Class<?> currentClazz = clazz;
        while (currentClazz.getSuperclass() != null && currentClazz.getSuperclass() != Object.class) {
            try {
                return currentClazz.getDeclaredField(newObjectName);
            } catch (NoSuchFieldException ignore) {
            }
            currentClazz = currentClazz.getSuperclass();
        }
        return null;
    }

    /**
     * Try to resolve class from {@code Field}.
     *
     * @param field The field to use
     * @return The class
     * @since 16.04.22
     */
    private Class<?> resolveClazz(final Field field) {
        if (field.getType() == List.class) {
            final ParameterizedType aType = (ParameterizedType) field.getGenericType();
            try {
                return this.classLoader.loadClass(aType.getActualTypeArguments()[0].getTypeName());
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return field.getType();
    }

    /**
     * Add patterns to the ignore list.
     *
     * @param patterns The patterns who need to be ignored
     * @since 16.04.28
     */
    public void addIgnoredPatterns(final String... patterns) {
        Collections.addAll(this.ignoredPattern, patterns);
    }

    /**
     * Add patterns to the ignore list.
     *
     * @param patterns The patterns who need to be ignored
     * @since 16.04.28
     */
    public void addIgnoredPatterns(final List<String> patterns) {
        this.ignoredPattern.addAll(patterns);
    }

    /**
     * Build a query for the given model class and arguments. The ending
     * varargs is used to specify rules to allow or deny queries on fields.
     *
     * @param c       The model class that this method will create request for
     * @param request The HTTP request
     * @param <T>     Something that extends Model
     * @return The Query
     * @see Model
     * @see Query
     * @since 16.04.22
     */
    public <T extends Model> Query<T> buildQuery(final Class<T> c, final Http.Request request) {
        return this.buildQuery(c, request.queryString(), Ebean.createQuery(c));
    }

    /**
     * Build a query for the given model class and arguments. The ending
     * varargs is used to specify rules to allow or deny queries on fields.
     *
     * @param c       The model class that this method will create request for
     * @param request The HTTP request
     * @param query   The current query object
     * @param <T>     Something that extends Model
     * @return The Query
     * @see Model
     * @see Query
     * @since 16.04.23
     */
    public <T extends Model> Query<T> buildQuery(final Class<T> c, final Http.Request request, final Query<T> query) {
        return this.buildQuery(c, request.queryString(), query);
    }

    /**
     * Build a query for the given model class and arguments. The ending
     * varargs is used to specify rules to allow or deny queries on fields.
     *
     * @param c    The model class that this method will create request for
     * @param args The arguments taken from request
     * @param <T>  Something that extends Model
     * @return The Query
     * @see Model
     * @see Query
     * @since 16.04.22
     */
    public <T extends Model> Query<T> buildQuery(final Class<T> c, final Map<String, String[]> args) {
        return this.buildQuery(c, args, Ebean.createQuery(c));
    }

    /**
     * Build a query for the given model class and arguments. The ending
     * varargs is used to specify rules to allow or deny queries on fields.
     *
     * @param c     The model class that this method will create request for
     * @param args  The arguments taken from request
     * @param query The current query object
     * @param <T>   Something that extends Model
     * @return The Query
     * @see Model
     * @see Query
     * @since 16.04.22
     */
    public <T extends Model> Query<T> buildQuery(final Class<T> c, final Map<String, String[]> args, final Query<T> query) {
        final ExpressionList<T> predicats = query.where();

        for (final Map.Entry<String, String[]> queryString : args.entrySet()) {
            if (this.ignoredPattern.stream().filter(queryString.getKey()::matches).count() > 0) {
                continue;
            }
            Class<?> currentClazz = c;
            final String[] keys = queryString.getKey().split("__");
            String foreignKeys = "";
            for (final String word : keys[0].split("\\.")) {
                final Field field = this.resolveField(currentClazz, word);
                if (field != null) {
                    currentClazz = this.resolveClazz(field);
                    if (currentClazz == null) {
                        //TODO: Raise exception or (see line below)
                        currentClazz = c;  //TODO: just leave the forloop.
                        break;
                    }
                    foreignKeys += (foreignKeys.isEmpty() ? "" : ".") + word;
                }
            }
            final String rawValue = queryString.getValue() == null ? "" : queryString.getValue()[0];
            if (Model.class.isAssignableFrom(currentClazz) && !foreignKeys.isEmpty()) {
                foreignKeys += ".id";
            }
            if (!foreignKeys.isEmpty()) {
                switch (keys.length >= 2 ? keys[1] : "eq") {
                    case "eq":
                        final Object eqValue = EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue);
                        if (eqValue instanceof DateTime) {
                            DateTime lowerDateTime = (DateTime) eqValue;
                            DateTime upperDateTime = lowerDateTime.plusMillis(999);
                            switch (rawValue.length()) {
                                case 16: /* yyyy-MM-dd'T'HH:mm */
                                    lowerDateTime = lowerDateTime
                                            .minusSeconds(lowerDateTime.getSecondOfMinute());
                                    upperDateTime = upperDateTime
                                            .plusSeconds(59);
                                    break;
                                case 13: /* yyyy-MM-dd'T'HH */
                                    lowerDateTime = lowerDateTime
                                            .minusMinutes(lowerDateTime.getMinuteOfHour())
                                            .minusSeconds(lowerDateTime.getSecondOfMinute());
                                    upperDateTime = upperDateTime
                                            .plusMinutes(59)
                                            .plusSeconds(59);
                                    break;
                                case 10: /* yyyy-MM-dd */
                                    lowerDateTime = lowerDateTime
                                            .minusHours(lowerDateTime.getHourOfDay())
                                            .minusMinutes(lowerDateTime.getMinuteOfHour())
                                            .minusSeconds(lowerDateTime.getSecondOfMinute());
                                    upperDateTime = upperDateTime
                                            .plusHours(23)
                                            .plusMinutes(59)
                                            .plusSeconds(59);
                                    break;
                                default:
                                    break;
                            }
                            predicats.ge(foreignKeys, lowerDateTime);
                            predicats.le(foreignKeys, upperDateTime);
                        } else {
                            predicats.eq(foreignKeys, eqValue);
                        }
                        break;
                    case "gt":
                        predicats.gt(foreignKeys, EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue));
                        break;
                    case "gte":
                        predicats.ge(foreignKeys, EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue));
                        break;
                    case "lt":
                        predicats.lt(foreignKeys, EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue));
                        break;
                    case "lte":
                        final Object lteValue = EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue);
                        if (lteValue instanceof DateTime) {
                            final DateTime dateTime = (DateTime) lteValue;
                            DateTime upperDateTime = dateTime.plusMillis(999);
                            switch (rawValue.length()) {
                                case 16: /* yyyy-MM-dd'T'HH:mm */
                                    upperDateTime = upperDateTime
                                            .plusSeconds(59);
                                    break;
                                case 13: /* yyyy-MM-dd'T'HH */
                                    upperDateTime = upperDateTime
                                            .plusMinutes(59)
                                            .plusSeconds(59);
                                    break;
                                case 10: /* yyyy-MM-dd */
                                    upperDateTime = upperDateTime
                                            .plusHours(23)
                                            .plusMinutes(59)
                                            .plusSeconds(59);
                                    break;
                                default:
                                    break;
                            }
                            predicats.le(foreignKeys, upperDateTime);
                        } else {
                            predicats.le(foreignKeys, lteValue);
                        }
                        break;
                    case "like":
                        predicats.like(foreignKeys, rawValue);
                        break;
                    case "ilike":
                        predicats.ilike(foreignKeys, rawValue);
                        break;
                    case "contains":
                        predicats.contains(foreignKeys, rawValue);
                        break;
                    case "icontains":
                        predicats.icontains(foreignKeys, rawValue);
                        break;
                    case "isnull":
                        predicats.isNull(foreignKeys);
                        break;
                    case "isnotnull":
                        predicats.isNotNull(foreignKeys);
                        break;
                    case "startswith":
                        predicats.startsWith(foreignKeys, rawValue);
                        break;
                    case "endswith":
                        predicats.endsWith(foreignKeys, rawValue);
                        break;
                    case "istartswith":
                        predicats.istartsWith(foreignKeys, rawValue);
                        break;
                    case "iendswith":
                        predicats.iendsWith(foreignKeys, rawValue);
                        break;
                    case "in":
                        final EbeanTypeConverter convertIn = EbeanTypeConverterManager.getInstance().getConverter(currentClazz);
                        predicats.in(foreignKeys, Arrays.stream(rawValue.split(",")).map(convertIn::convert).toArray());
                        break;
                    case "notin":
                        final EbeanTypeConverter convertNotIn = EbeanTypeConverterManager.getInstance().getConverter(currentClazz);
                        predicats.not(Expr.in(foreignKeys, Arrays.stream(rawValue.split(",")).map(convertNotIn::convert).toArray()));
                        break;
                    case "orderby":
                        if (rawValue != null && (rawValue.compareToIgnoreCase("asc") == 0 || rawValue.compareToIgnoreCase("desc") == 0)) {
                            predicats.orderBy(foreignKeys + " " + rawValue);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return query;
    }
}
