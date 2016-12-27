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
package com.zero_x_baadf00d.ebean.converter;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;

/**
 * Converter for {@code DateTime}.
 *
 * @author Thibault Meyer
 * @version 16.04.25
 * @since 16.04.22
 */
public final class DateTimeEbeanTypeConverter implements EbeanTypeConverter<DateTime> {

    /**
     * Handle to the datetime formatter.
     *
     * @since 16.04.25
     */
    private final DateTimeFormatter dateTimeFormatter;

    /**
     * Build a default instance.
     *
     * @since 16.04.25
     */
    public DateTimeEbeanTypeConverter() {
        final DateTimeParser[] formatPatterns = {
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH").getParser(),
            DateTimeFormat.forPattern("yyyy-MM-dd").getParser(),
        };
        this.dateTimeFormatter = new DateTimeFormatterBuilder().append(null, formatPatterns).toFormatter().withZoneUTC();
    }

    @Override
    public Object convert(final String obj) {
        try {
            final DateTime dateTime = this.dateTimeFormatter.parseDateTime(obj);
            return dateTime.minusMillis(dateTime.getMillisOfSecond());
        } catch (IllegalArgumentException ignore) {
            return null;
        }
    }

    @Override
    public Class<DateTime> getManagedObjectClass() {
        return DateTime.class;
    }
}
