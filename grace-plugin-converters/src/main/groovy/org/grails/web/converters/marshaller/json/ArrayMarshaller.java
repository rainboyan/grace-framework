/*
 * Copyright 2004-2022 the original author or authors.
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
package org.grails.web.converters.marshaller.json;

import java.lang.reflect.Array;

import grails.converters.JSON;

import org.grails.web.converters.exceptions.ConverterException;
import org.grails.web.converters.marshaller.ObjectMarshaller;
import org.grails.web.json.JSONWriter;

/**
 * @author Siegfried Puchbauer
 * @since 1.1
 */
public class ArrayMarshaller implements ObjectMarshaller<JSON> {

    public boolean supports(Object object) {
        return object.getClass().isArray();
    }

    public void marshalObject(Object o, JSON converter) throws ConverterException {
        JSONWriter writer = converter.getWriter();
        int len = Array.getLength(o);
        writer.array();
        for (int i = 0; i < len; i++) {
            converter.convertAnother(Array.get(o, i));
        }
        writer.endArray();
    }

}
