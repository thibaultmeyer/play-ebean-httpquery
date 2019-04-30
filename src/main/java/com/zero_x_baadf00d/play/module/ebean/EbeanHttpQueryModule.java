/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 - 2019 Thibault Meyer
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
package com.zero_x_baadf00d.play.module.ebean;

import com.zero_x_baadf00d.ebean.PlayEbeanHttpQuery;
import io.ebean.ExpressionList;
import io.ebean.Model;
import io.ebean.Query;
import play.mvc.Http;

/**
 * Give access to a pre-configured instance of
 * {@code PlayEbeanHttpQuery}.
 *
 * @author Thibault Meyer
 * @version 18.01.15
 * @see com.zero_x_baadf00d.ebean.PlayEbeanHttpQuery
 * @since 16.04.28
 */
public interface EbeanHttpQueryModule {

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
     * @since 16.04.28
     */
    <T extends Model> Query<T> buildQuery(final Class<T> c, final Http.Request request);

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
     * @since 16.04.28
     */
    <T extends Model> Query<T> buildQuery(final Class<T> c, final Http.Request request, final Query<T> query);

    /**
     * Build a query for the given model class and arguments. The ending
     * varargs is used to specify rules to allow or deny queries on fields.
     *
     * @param c       The model class that this method will create request for
     * @param request The HTTP request
     * @param expr    The current list of expressions
     * @param <T>     Something that extends Model
     * @return The Query
     * @see Model
     * @see Query
     * @since 18.01.15
     */
    <T extends Model> Query<T> buildQuery(final Class<T> c, final Http.Request request, final ExpressionList<T> expr);

    /**
     * Return a new instance of {@code PlayEbeanHttpQuery}.
     *
     * @return A new instance of {@code PlayEbeanHttpQuery}
     * @see PlayEbeanHttpQuery
     * @since 16.09.06
     */
    PlayEbeanHttpQuery withNewEbeanHttpQuery();
}
