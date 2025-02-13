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
package org.grails.core.artefact;

import grails.core.ArtefactHandlerAdapter;
import grails.core.GrailsUrlMappingsClass;

import org.grails.core.DefaultGrailsUrlMappingsClass;

/**
 * A handler for UrlMappings.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public class UrlMappingsArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = "UrlMappings";

    public UrlMappingsArtefactHandler() {
        super(TYPE, GrailsUrlMappingsClass.class, DefaultGrailsUrlMappingsClass.class,
                DefaultGrailsUrlMappingsClass.URL_MAPPINGS);
    }

}
