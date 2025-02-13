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
package org.grails.web.databinding.bindingsource

import jakarta.annotation.PostConstruct

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired

import grails.databinding.CollectionDataBindingSource
import grails.databinding.DataBindingSource
import grails.web.mime.MimeType

import org.grails.databinding.bindingsource.DataBindingSourceCreator
import org.grails.web.util.ClassAndMimeTypeRegistry

@CompileStatic
class DefaultDataBindingSourceRegistry extends ClassAndMimeTypeRegistry<DataBindingSourceCreator, DataBindingSourceCreatorCacheKey>
        implements DataBindingSourceRegistry {

    @Autowired(required = false)
    void setDataBindingSourceCreators(DataBindingSourceCreator[] dataBindingSourceCreators) {
        for (DataBindingSourceCreator dbsc in dataBindingSourceCreators) {
            addToRegisteredObjects(dbsc.targetType, dbsc)
        }
    }

    @PostConstruct
    void initialize() {
        registerDefault(MimeType.JSON, new JsonDataBindingSourceCreator())
        registerDefault(MimeType.TEXT_JSON, new JsonDataBindingSourceCreator())
        registerDefault(MimeType.XML, new XmlDataBindingSourceCreator())
        registerDefault(MimeType.TEXT_XML, new XmlDataBindingSourceCreator())
    }

    protected DataBindingSourceCreator getDataBindingSourceCreator(MimeType mimeType, Class targetType, Object bindingSource) {
        DataBindingSourceCreator bindingSourceCreator = findMatchingObjectForMimeType(mimeType, targetType)
        if (bindingSourceCreator == null) {
            bindingSourceCreator = new DefaultDataBindingSourceCreator()
        }
        bindingSourceCreator
    }

    @Override
    void addDataBindingSourceCreator(DataBindingSourceCreator creator) {
        addToRegisteredObjects(creator.targetType, creator)
    }

    @Override
    DataBindingSource createDataBindingSource(MimeType mimeType, Class bindingTargetType, bindingSource) {
        DataBindingSourceCreator helper = getDataBindingSourceCreator(mimeType, bindingTargetType, bindingSource)
        helper.createDataBindingSource(mimeType, bindingTargetType, bindingSource)
    }

    @Override
    CollectionDataBindingSource createCollectionDataBindingSource(MimeType mimeType, Class bindingTargetType, bindingSource) {
        DataBindingSourceCreator helper = getDataBindingSourceCreator(mimeType, bindingTargetType, bindingSource)
        helper.createCollectionDataBindingSource(mimeType, bindingTargetType, bindingSource)
    }

    @Override
    DataBindingSourceCreatorCacheKey createCacheKey(Class type, MimeType mimeType) {
        new DataBindingSourceCreatorCacheKey(type, mimeType)
    }

    @Canonical
    static class DataBindingSourceCreatorCacheKey {

        Class type
        MimeType mimeType

    }

}
