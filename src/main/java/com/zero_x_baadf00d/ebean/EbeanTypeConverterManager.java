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

import com.zero_x_baadf00d.ebean.converter.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This manager handles all registered registered
 * ebean type converters.
 *
 * @author Thibault Meyer
 * @version 16.04.22
 * @since 16.04.22
 */
public final class EbeanTypeConverterManager {

    /**
     * Handle to current enabled registeredConverters.
     *
     * @since 16.04.22
     */
    private final Map<Class, EbeanTypeConverter> registeredConverters;

    /**
     * Build a basic instance.
     *
     * @since 16.04.22
     */
    private EbeanTypeConverterManager() {
        this.registeredConverters = new HashMap<>();
        this.registerConverter(new IntegerEbeanTypeConverter());
        this.registerConverter(new LongEbeanTypeConverter());
        this.registerConverter(new DoubleEbeanTypeConverter());
        this.registerConverter(new BooleanEbeanTypeConverter());
        this.registerConverter(new DateTimeEbeanTypeConverter());
    }

    /**
     * Get current instance.
     *
     * @return The current instance of {@code EbeanTypeConverterManager}
     * @since 16.04.22
     */
    public static EbeanTypeConverterManager getInstance() {
        return EbeanTypeConverterManagerSingletonHolder.INSTANCE;
    }

    /**
     * Register a new ebeanTypeConverter.
     *
     * @param ebeanTypeConverter The ebeanTypeConverter to register
     * @since 16.04.22
     */
    public void registerConverter(final EbeanTypeConverter ebeanTypeConverter) {
        this.registeredConverters.putIfAbsent(ebeanTypeConverter.getManagedObjectClass(), ebeanTypeConverter);
    }

    /**
     * Register new ebeanTypeConverters.
     *
     * @param ebeanTypeConverters A list of registeredConverters to register
     * @since 16.04.22
     */
    public void registerConverters(final EbeanTypeConverter... ebeanTypeConverters) {
        for (final EbeanTypeConverter ebeanTypeConverter : ebeanTypeConverters) {
            this.registeredConverters.putIfAbsent(ebeanTypeConverter.getManagedObjectClass(), ebeanTypeConverter);
        }
    }

    /**
     * Register new ebeanTypeConverters.
     *
     * @param ebeanTypeConverters A list of registeredConverters to register
     * @since 16.04.22
     */
    public void registerConverters(final List<EbeanTypeConverter> ebeanTypeConverters) {
        for (final EbeanTypeConverter ebeanTypeConverter : ebeanTypeConverters) {
            this.registeredConverters.putIfAbsent(ebeanTypeConverter.getManagedObjectClass(), ebeanTypeConverter);
        }
    }

    /**
     * Get a registered converter.
     *
     * @param clazz The class of the object to convert
     * @return The converter, otherwise, {@code null}
     * @since 16.04.22
     */
    public EbeanTypeConverter getConverter(final Class clazz) {
        final EbeanTypeConverter c = this.registeredConverters.get(clazz);
        return c != null ? c : new DummyEbeanTypeConverter();
    }

    /**
     * Convert obj to the needed type.
     *
     * @param clazz The class type
     * @param obj   The object to convert
     * @return The object converted in case of success
     * @since 16.04.22
     */
    public Object convert(final Class clazz, final String obj) {
        final EbeanTypeConverter ebeanTypeConverter = this.getConverter(clazz);
        if (ebeanTypeConverter != null) {
            return ebeanTypeConverter.convert(obj);
        }
        return null;
    }

    /**
     * Single holder for {@code EbeanTypeConverterManager}.
     *
     * @author Thibault Meyer
     * @version 16.04.22
     * @see EbeanTypeConverterManager
     * @since 16.04.22
     */
    private static class EbeanTypeConverterManagerSingletonHolder {
        private final static EbeanTypeConverterManager INSTANCE = new EbeanTypeConverterManager();
    }
}
