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
package org.grails.web.converters.configuration;

import java.util.Collections;
import java.util.List;

import grails.core.support.proxy.DefaultProxyHandler;
import grails.core.support.proxy.ProxyHandler;

import org.grails.web.converters.Converter;
import org.grails.web.converters.marshaller.ObjectMarshaller;

/**
 * Immutable Converter Configuration.
 *
 * @author Siegfried Puchbauer
 * @see org.grails.web.converters.configuration.ChainedConverterConfiguration
 */
@SuppressWarnings("rawtypes")
public class ImmutableConverterConfiguration<C extends Converter> implements ConverterConfiguration<C> {

    protected final List<ObjectMarshaller<C>> marshallers;

    private final String encoding;

    private final Converter.CircularReferenceBehaviour circularReferenceBehaviour;

    private final boolean prettyPrint;

    private ProxyHandler proxyHandler;

    private final boolean cacheObjectMarshallerByClass;

    public ImmutableConverterConfiguration(ConverterConfiguration<C> cfg) {
        this(cfg, new DefaultProxyHandler());
    }

    public ImmutableConverterConfiguration(ConverterConfiguration<C> cfg, ProxyHandler proxyHandler) {
        this.marshallers = Collections.unmodifiableList(cfg.getOrderedObjectMarshallers());
        this.encoding = cfg.getEncoding();
        this.prettyPrint = cfg.isPrettyPrint();
        this.cacheObjectMarshallerByClass = cfg.isCacheObjectMarshallerByClass();
        this.circularReferenceBehaviour = cfg.getCircularReferenceBehaviour();
        this.proxyHandler = proxyHandler;
    }

    /**
     * @see ConverterConfiguration#getMarshaller(Object)
     */
    public ObjectMarshaller<C> getMarshaller(Object o) {
        for (ObjectMarshaller<C> om : this.marshallers) {
            if (om.supports(o)) {
                return om;
            }
        }
        return null;
    }

    /**
     * @see ConverterConfiguration#getEncoding()
     */
    public String getEncoding() {
        return this.encoding;
    }

    /**
     * @see ConverterConfiguration#getCircularReferenceBehaviour()
     */
    public Converter.CircularReferenceBehaviour getCircularReferenceBehaviour() {
        return this.circularReferenceBehaviour;
    }

    /**
     * @see ConverterConfiguration#isPrettyPrint()
     */
    public boolean isPrettyPrint() {
        return this.prettyPrint;
    }

    public List<ObjectMarshaller<C>> getOrderedObjectMarshallers() {
        return this.marshallers;
    }

    public ProxyHandler getProxyHandler() {
        return this.proxyHandler;
    }

    public boolean isCacheObjectMarshallerByClass() {
        return this.cacheObjectMarshallerByClass;
    }

}
