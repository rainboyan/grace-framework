/*
 * Copyright 2014-2022 the original author or authors.
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
package org.grails.core.artefact

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassNode

import grails.core.ArtefactHandlerAdapter
import grails.core.DefaultGrailsClass
import grails.core.GrailsClass

/**
 * An {@link grails.core.ArtefactHandler} that identifies the Application class
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ApplicationArtefactHandler extends ArtefactHandlerAdapter {

    public static final String TYPE = 'Application'

    ApplicationArtefactHandler() {
        super(TYPE, GrailsClass, DefaultGrailsClass, null, false)
    }

    @Override
    boolean isArtefact(ClassNode classNode) {
        classNode.nameWithoutPackage.endsWith('Application')
    }

    @Override
    boolean isArtefactClass(Class<?> clazz) {
        clazz.simpleName.endsWith('Application')
    }

}
