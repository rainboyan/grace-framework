/*
 * Copyright 2015-2022 the original author or authors.
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
package org.grails.plugins.web.interceptors

import grails.artefact.Interceptor
import grails.core.ArtefactHandlerAdapter
import grails.core.DefaultGrailsClass
import grails.core.GrailsClass

/**
 * {@link grails.core.ArtefactHandler} for {@link grails.artefact.Interceptor} instances
 *
 * @author Graeme Rocher
 * @since 3.0
 */
class InterceptorArtefactHandler extends ArtefactHandlerAdapter {

    public static final String MATCH_SUFFIX = '.INTERCEPTOR_MATCHED'

    public static final String TYPE = Interceptor.getSimpleName()
    public static final String PLUGIN_NAME = 'interceptors'

    InterceptorArtefactHandler() {
        super(TYPE, GrailsClass, DefaultGrailsClass, TYPE)
    }

    @Override
    String getPluginName() {
        PLUGIN_NAME
    }

}
