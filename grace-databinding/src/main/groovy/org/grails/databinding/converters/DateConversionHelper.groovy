/*
 * Copyright 2013-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.databinding.converters

import java.text.DateFormat
import java.text.SimpleDateFormat

import groovy.transform.CompileStatic

import grails.databinding.converters.ValueConverter

/**
 * @author Jeff Brown
 * @since 2.3
 */
@CompileStatic
class DateConversionHelper implements ValueConverter {

    /**
     * A List of String which represent date formats compatible with {@link SimpleDateFormat}.  When
     * This converter attempts to convert a String to a Date, these formats will be tried in
     * the order in which they appear in the List.
     */
    List<String> formatStrings = []

    /**
     * Whether data parsing is lenient
     */
    boolean dateParsingLenient = false

    @Override
    Object convert(Object value) {
        Date dateValue
        if (value instanceof String) {
            if (!value) {
                return null
            }
            Exception firstException
            formatStrings.each { String format ->
                if (dateValue == null) {
                    DateFormat formatter = new SimpleDateFormat(format)
                    try {
                        formatter.lenient = dateParsingLenient
                        dateValue = formatter.parse((String) value)
                    }
                    catch (Exception e) {
                        firstException = firstException ?: e
                    }
                }
            }
            if (dateValue == null && firstException) {
                throw firstException
            }
        }
        dateValue
    }

    @Override
    Class<?> getTargetType() {
        Date
    }

    @Override
    boolean canConvert(Object value) {
        value instanceof String
    }

}
