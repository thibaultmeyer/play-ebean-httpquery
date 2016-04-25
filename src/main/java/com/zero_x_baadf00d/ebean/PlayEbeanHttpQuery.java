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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Helper to map flat query strings to Ebean filters.
 *
 * @author Thibault Meyer
 * @version 16.04.23
 * @since 16.04.22
 */
public final class PlayEbeanHttpQuery {

    private static Field resolveField(final Class<?> clazz, final String newObjectName) {
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

    private static Class<?> resolveClazz(final Field field) {
        if (field.getType() == List.class) {
            final ParameterizedType aType = (ParameterizedType) field.getGenericType();
            try {
                return Class.forName(aType.getActualTypeArguments()[0].getTypeName());
            } catch (ClassNotFoundException ex) {
                return null;
            }
        }
        return field.getType();
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
    public static <T extends Model> Query<T> buildQuery(final Class<T> c, final Http.Request request) {
        return PlayEbeanHttpQuery.buildQuery(c, request.queryString(), Ebean.createQuery(c));
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
    public static <T extends Model> Query<T> buildQuery(final Class<T> c, final Http.Request request, final Query<T> query) {
        return PlayEbeanHttpQuery.buildQuery(c, request.queryString(), query);
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
    public static <T extends Model> Query<T> buildQuery(final Class<T> c, final Map<String, String[]> args) {
        return PlayEbeanHttpQuery.buildQuery(c, args, Ebean.createQuery(c));
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
    public static <T extends Model> Query<T> buildQuery(final Class<T> c, final Map<String, String[]> args, final Query<T> query) {
        final ExpressionList<T> predicats = query.where();
        final List<String> tableAlias = new ArrayList<>();

        for (final Map.Entry<String, String[]> queryString : args.entrySet()) {
            if (queryString.getKey().compareTo("fields") == 0) {
                continue;
            }
            Class<?> currentClazz = c;
            final String[] keys = queryString.getKey().split("__");
            String foreignKeys = "";

            for (final String word : keys[0].split("\\.")) {
                final Field field = PlayEbeanHttpQuery.resolveField(currentClazz, word);
                if (field != null) {
                    currentClazz = PlayEbeanHttpQuery.resolveClazz(field);
                    if (currentClazz == null) {
                        //TODO: Raise exception or (see line below)
                        break; //TODO: just leave the forloop.
                    }
                    if (field.isAnnotationPresent(javax.persistence.ManyToMany.class)) {
                        //TODO: This statement is still needed? (Ebean 7.7.1)
                        int idx = tableAlias.indexOf(field.getName());
                        if (idx < 0) {
                            tableAlias.add(field.getName());
                            idx = 0;
                        }
                        foreignKeys += (foreignKeys.isEmpty() ? "t" : ".t") + String.valueOf(idx);
                    } else {
                        foreignKeys += (foreignKeys.isEmpty() ? "" : ".") + word;
                    }
                }
            }
            final String rawValue = queryString.getValue() == null ? "" : queryString.getValue()[0];
            switch (keys.length == 2 ? keys[1] : "eq") {
                case "eq":
                    final Object eqValue = EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue);
                    if (eqValue instanceof DateTime) {
                        final DateTime dateTime = (DateTime) eqValue;
                        predicats.ge(foreignKeys, dateTime);
                        predicats.le(foreignKeys, dateTime.plusMillis(999));
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
                        predicats.le(foreignKeys, dateTime.plusMillis(999));
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
                    predicats.in(foreignKeys, Arrays.asList(rawValue.split(",")).stream().map((Function<String, Object>) convertIn::convert).toArray());
                    break;
                case "notin":
                    final EbeanTypeConverter convertNotIn = EbeanTypeConverterManager.getInstance().getConverter(currentClazz);
                    predicats.not(Expr.in(foreignKeys, Arrays.asList(rawValue.split(",")).stream().map((Function<String, Object>) convertNotIn::convert).toArray()));
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
        return query;
    }
}
