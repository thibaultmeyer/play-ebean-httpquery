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
package com.zero_x_baadf00d.play.module.ebean;

import com.typesafe.config.Config;
import com.zero_x_baadf00d.ebean.PlayEbeanHttpQuery;
import io.ebean.Model;
import io.ebean.Query;
import play.api.Environment;
import play.mvc.Http;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@code EbeanHttpQueryModule}.
 *
 * @author Thibault Meyer
 * @version 17.06.26
 * @see com.zero_x_baadf00d.ebean.PlayEbeanHttpQuery
 * @since 16.04.28
 */
@Singleton
public class EbeanHttpQueryImpl implements EbeanHttpQueryModule {

    /**
     * @since 16.03.09
     */
    private static final String EBEAN_HTTP_PARSER_IGNORE = "ebeanHttpQuery.ignorePatterns";

    /**
     * @since 16.03.30
     */
    private static final String EBEAN_HTTP_FIELD_ALIASES = "ebeanHttpQuery.fieldAliases";

    /**
     * Handle to the Ebean HTTP Query parser.
     *
     * @since 16.04.28
     */
    private final PlayEbeanHttpQuery playEbeanHttpQuery;

    /**
     * Build a basic instance with injected dependency.
     *
     * @param configuration The current application configuration
     * @param environment   The current environment
     * @since 16.05.05
     */
    @Inject
    public EbeanHttpQueryImpl(final Config configuration, final Environment environment) {
        final List<String> patterns;
        if (configuration.hasPath(EbeanHttpQueryImpl.EBEAN_HTTP_PARSER_IGNORE)) {
            patterns = configuration.getStringList(
                EbeanHttpQueryImpl.EBEAN_HTTP_PARSER_IGNORE
            );
        } else {
            patterns = new ArrayList<>();
        }
        this.playEbeanHttpQuery = new PlayEbeanHttpQuery(environment.classLoader());
        this.playEbeanHttpQuery.addIgnoredPatterns(patterns);
        if (configuration.hasPath(EbeanHttpQueryImpl.EBEAN_HTTP_FIELD_ALIASES)) {
            final Map<?, ?> map = configuration.getObject(EbeanHttpQueryImpl.EBEAN_HTTP_FIELD_ALIASES);
            map.forEach((key, value) -> playEbeanHttpQuery.addAlias((String) key, (String) value));
        }
    }

    @Override
    public <T extends Model> Query<T> buildQuery(final Class<T> c, final Http.Request request) {
        return this.playEbeanHttpQuery.buildQuery(c, request);
    }

    @Override
    public <T extends Model> Query<T> buildQuery(final Class<T> c, final Http.Request request, final Query<T> query) {
        return this.playEbeanHttpQuery.buildQuery(c, request, query);
    }

    @Override
    public PlayEbeanHttpQuery withNewEbeanHttpQuery() {
        return (PlayEbeanHttpQuery) this.playEbeanHttpQuery.clone();
    }
}
