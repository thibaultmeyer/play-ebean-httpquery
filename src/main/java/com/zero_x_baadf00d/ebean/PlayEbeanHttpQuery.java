/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 - 2017 Thibault Meyer
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

import com.zero_x_baadf00d.ebean.converter.EbeanTypeConverter;
import io.ebean.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import play.mvc.Http;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Helper to map flat query strings to Ebean filters.
 *
 * @author Thibault Meyer
 * @version 18.07.24
 * @since 16.04.22
 */
public class PlayEbeanHttpQuery implements Cloneable {

    /**
     * Keys to ignore.
     *
     * @since 16.04.28
     */
    private final ConcurrentLinkedQueue<String> ignoredPattern;

    /**
     * Keys aliasing.
     *
     * @since 16.09.30
     */
    private final Map<String, String> aliasPattern;

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
        this.ignoredPattern = new ConcurrentLinkedQueue<>();
        this.aliasPattern = new HashMap<>();
    }

    /**
     * Build an instance with specific class loader.
     *
     * @param classLoader The class loader to use
     * @since 16.05.05
     */
    public PlayEbeanHttpQuery(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        this.ignoredPattern = new ConcurrentLinkedQueue<>();
        this.aliasPattern = new HashMap<>();
    }

    /**
     * Try to resolve the primary key field. Primary key is the field annotated with @Id.
     *
     * @param clazz The class to inspect
     * @return The {@code Field} object
     * @since 18.06.18
     */
    private Field resolvePrimaryKeyField(final Class<?> clazz) {
        Class<?> currentClazz = clazz;
        while (currentClazz.getSuperclass() != null && currentClazz.getSuperclass() != Object.class) {
            for (final Field field : currentClazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field;
                }
            }
            currentClazz = currentClazz.getSuperclass();
        }
        return null;
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
     * Try to transform given Datetime to a range of datetime. The transformation is specific to PostgreSQL.
     *
     * @param rawValue The raw value
     * @param dateTime The converted datetime
     * @return A range of Datetime
     * @since 18.07.23
     */
    private Pair<DateTime, DateTime> transformSpecificDateTimeToRange(final String rawValue, final DateTime dateTime) {
        DateTime lowerDateTime = dateTime;
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
            case 7: /* yyyy-MM */
                lowerDateTime = lowerDateTime
                    .withDayOfMonth(1)
                    .minusHours(lowerDateTime.getHourOfDay())
                    .minusMinutes(lowerDateTime.getMinuteOfHour())
                    .minusSeconds(lowerDateTime.getSecondOfMinute());
                upperDateTime = upperDateTime
                    .withDayOfMonth(lowerDateTime.dayOfMonth().withMaximumValue().getDayOfMonth())
                    .plusHours(23)
                    .plusMinutes(59)
                    .plusSeconds(59);
                break;
            case 4: /* yyyy */
                lowerDateTime = lowerDateTime
                    .withMonthOfYear(1)
                    .withDayOfMonth(1)
                    .minusHours(lowerDateTime.getHourOfDay())
                    .minusMinutes(lowerDateTime.getMinuteOfHour())
                    .minusSeconds(lowerDateTime.getSecondOfMinute());
                upperDateTime = upperDateTime
                    .withMonthOfYear(12)
                    .withDayOfMonth(lowerDateTime.dayOfMonth().withMaximumValue().getDayOfMonth())
                    .plusHours(23)
                    .plusMinutes(59)
                    .plusSeconds(59);
                break;
            default:
                break;
        }
        return Pair.of(lowerDateTime, upperDateTime);
    }

    /**
     * Checks if the given instruction is ignored.
     *
     * @param instruction The instruction to test
     * @return {@code true} if the instruction is ignored
     */
    private boolean isInstructionIgnored(final String instruction) {
        final Iterator<String> it = this.ignoredPattern.iterator();
        while (it.hasNext()) {
            if (instruction.matches(it.next())) {
                return true;
            }
        }
        return false;
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
     * Add new alias. <pre>addAlias("article.album.displayName", "firstName")</pre> will
     * create a something like: <pre>article.album.firstName</pre>
     *
     * @param pattern The pattern to match
     * @param alias   The alias to use
     * @since 16.09.30
     */
    public void addAlias(final String pattern, final String alias) {
        this.aliasPattern.put(pattern, alias);
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
    public <T extends Model> Query<T> buildQuery(final Class<T> c,
                                                 final Map<String, String[]> args,
                                                 final Query<T> query) {
        final ExpressionList<T> predicates = query.where();
        final List<String> orderByPredicates = new ArrayList<>();

        // Iterates overs all instructions
        for (final Map.Entry<String, String[]> queryString : args.entrySet()) {

            // Check if current instruction is not allowed
            if (this.isInstructionIgnored(queryString.getKey())) {
                continue;
            }

            Class<?> currentClazz = c;
            final String[] keys = queryString.getKey().split("__");
            String foreignKeys = "";

            // Resolves existing aliases
            final List<String> newQueryWithAliasResolved = new ArrayList<>();
            for (final String word : keys[0].split("\\.")) {
                if (!this.aliasPattern.isEmpty()) {
                    final String aliasKeyToTry = c.getSimpleName()
                        + ">"
                        + (foreignKeys.isEmpty() ? "" : foreignKeys + ".")
                        + word;
                    String alias = null;
                    for (final Map.Entry<String, String> entry : this.aliasPattern.entrySet()) {
                        if (aliasKeyToTry.matches(entry.getKey())) {
                            alias = entry.getValue();
                            break;
                        }
                    }
                    if (alias != null) {
                        newQueryWithAliasResolved.addAll(Arrays.asList(alias.split("\\.")));
                    } else {
                        newQueryWithAliasResolved.add(word);
                    }
                } else {
                    newQueryWithAliasResolved.add(word);
                }
            }

            // Resolves right field (path + class) on the Model class
            for (final String word : newQueryWithAliasResolved) {
                final Field field = this.resolveField(currentClazz, word);
                if (field != null) {
                    currentClazz = this.resolveClazz(field);
                    if (currentClazz == null) {
                        currentClazz = c;
                        break;
                    }
                    foreignKeys += (foreignKeys.isEmpty() ? "" : ".") + word;
                }
            }
            final String rawValue = queryString.getValue() == null ? "" : queryString.getValue()[0];
            if (Model.class.isAssignableFrom(currentClazz) && !foreignKeys.isEmpty()) {
                final Field field = this.resolvePrimaryKeyField(currentClazz);
                if (field != null) {
                    foreignKeys += "." + field.getName();
                    currentClazz = field.getType();
                }
            }

            // Continue operation only if a field has been identified
            if (!foreignKeys.isEmpty()) {

                // Check if "not" flag is present. In this case, the "context predicates"
                // will be set to use the following statement as NOT statement
                final boolean notFlag = keys.length >= 3 && keys[1].compareToIgnoreCase("not") == 0;
                final ExpressionList<T> ctxPredicates = notFlag ? predicates.not() : predicates;

                // Find the right operator and create the statement
                switch (keys.length >= 3 ? keys[2] : keys.length >= 2 ? keys[1] : "eq") {
                    case "eq":
                        final Object eqValue = EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue);
                        if (eqValue instanceof DateTime) {
                            final Pair<DateTime, DateTime> dtRange = this.transformSpecificDateTimeToRange(
                                rawValue,
                                (DateTime) eqValue
                            );
                            ctxPredicates.ge(foreignKeys, dtRange.getLeft());
                            ctxPredicates.le(foreignKeys, dtRange.getRight());
                        } else {
                            ctxPredicates.eq(foreignKeys, eqValue);
                        }
                        break;
                    case "ne":
                        final Object neValue = EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue);
                        if (neValue instanceof DateTime) {
                            final Pair<DateTime, DateTime> dtRange = this.transformSpecificDateTimeToRange(
                                rawValue,
                                (DateTime) neValue
                            );
                            ctxPredicates.not(
                                Expr.and(
                                    Expr.ge(foreignKeys, dtRange.getLeft()),
                                    Expr.le(foreignKeys, dtRange.getRight())
                                )
                            );
                            ctxPredicates.endNot();
                        } else {
                            ctxPredicates.ne(foreignKeys, neValue);
                        }
                        break;
                    case "gt":
                        ctxPredicates.gt(
                            foreignKeys,
                            EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue)
                        );
                        break;
                    case "gte":
                        ctxPredicates.ge(
                            foreignKeys,
                            EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue)
                        );
                        break;
                    case "lt":
                        ctxPredicates.lt(
                            foreignKeys,
                            EbeanTypeConverterManager.getInstance().convert(currentClazz, rawValue)
                        );
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
                            ctxPredicates.le(foreignKeys, upperDateTime);
                        } else {
                            ctxPredicates.le(foreignKeys, lteValue);
                        }
                        break;
                    case "like":
                        ctxPredicates.like(foreignKeys, rawValue);
                        break;
                    case "ilike":
                        ctxPredicates.ilike(foreignKeys, rawValue);
                        break;
                    case "contains":
                        ctxPredicates.contains(foreignKeys, rawValue);
                        break;
                    case "icontains":
                        ctxPredicates.icontains(foreignKeys, rawValue);
                        break;
                    case "startswith":
                        ctxPredicates.startsWith(foreignKeys, rawValue);
                        break;
                    case "endswith":
                        ctxPredicates.endsWith(foreignKeys, rawValue);
                        break;
                    case "istartswith":
                        ctxPredicates.istartsWith(foreignKeys, rawValue);
                        break;
                    case "iendswith":
                        ctxPredicates.iendsWith(foreignKeys, rawValue);
                        break;
                    case "in":
                        if (rawValue.isEmpty()) {
                            ctxPredicates.in(foreignKeys, Collections.EMPTY_LIST);
                        } else {
                            final EbeanTypeConverter convertIn = EbeanTypeConverterManager.getInstance()
                                .getConverter(currentClazz);
                            ctxPredicates.in(
                                foreignKeys,
                                Arrays.stream(rawValue.split(",")).map(convertIn::convert).toArray()
                            );
                        }
                        break;
                    case "notin":
                        if (rawValue.isEmpty()) {
                            ctxPredicates.in(foreignKeys, Collections.EMPTY_LIST);
                        } else {
                            final EbeanTypeConverter convertNotIn = EbeanTypeConverterManager.getInstance()
                                .getConverter(currentClazz);
                            ctxPredicates.not(
                                Expr.in(
                                    foreignKeys,
                                    Arrays.stream(rawValue.split(",")).map(convertNotIn::convert).toArray()
                                )
                            );
                        }
                        break;
                    case "between":
                        if (rawValue.isEmpty()) {
                            ctxPredicates.between(foreignKeys, null, null);
                        } else {
                            final EbeanTypeConverter convertNotIn = EbeanTypeConverterManager.getInstance()
                                .getConverter(currentClazz);
                            final String[] betweenArgs = rawValue.split(",");
                            ctxPredicates.between(
                                foreignKeys,
                                betweenArgs.length >= 1 ? convertNotIn.convert(betweenArgs[0]) : null,
                                betweenArgs.length >= 2 ? convertNotIn.convert(betweenArgs[1]) : null
                            );
                        }
                        break;
                    case "isnull":
                        ctxPredicates.isNull(foreignKeys);
                        break;
                    case "isnotnull":
                        ctxPredicates.isNotNull(foreignKeys);
                        break;
                    case "isempty":
                        ctxPredicates.isEmpty(StringUtils.substringBeforeLast(foreignKeys, "."));
                        break;
                    case "isnotempty":
                        ctxPredicates.isNotEmpty(StringUtils.substringBeforeLast(foreignKeys, "."));
                        break;
                    case "orderby":
                        if (rawValue != null
                            && (rawValue.compareToIgnoreCase("asc") == 0 || rawValue.compareToIgnoreCase("desc") == 0)) {
                            orderByPredicates.add(foreignKeys + " " + rawValue);
                        }
                        break;
                    default:
                        break;
                }

                // Close the NOT statement if needed
                if (notFlag) {
                    ctxPredicates.endJunction();
                }
            }
        }

        // Build "Order by" predicates
        if (!orderByPredicates.isEmpty()) {
            predicates.orderBy(String.join(", ", orderByPredicates));
        }

        // Return the prepared query
        return query;
    }

    /**
     * Clone object.
     *
     * @return A new instance
     * @since 16.09.06
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ignore) {
        }
        return null;
    }
}
