/*
 * Copyright 2006-2022 the original author or authors.
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
package org.grails.compiler.injection;

import java.security.CodeSource;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;

import grails.compiler.ast.ClassInjector;

/**
 * A class loader that is aware of Groovy sources and injection operations.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
public class GrailsAwareClassLoader extends GroovyClassLoader {

    private ClassInjector[] classInjectors;

    public GrailsAwareClassLoader() {
        // default
    }

    public GrailsAwareClassLoader(ClassLoader loader) {
        super(loader);
    }

    public GrailsAwareClassLoader(GroovyClassLoader parent) {
        super(parent);
    }

    public GrailsAwareClassLoader(ClassLoader parent, CompilerConfiguration config, boolean useConfigurationClasspath) {
        super(parent, config, useConfigurationClasspath);
    }

    public GrailsAwareClassLoader(ClassLoader loader, CompilerConfiguration config) {
        super(loader, config);
    }

    public void setClassInjectors(ClassInjector[] classInjectors) {
        this.classInjectors = classInjectors;
    }

    /**
     * @see groovy.lang.GroovyClassLoader#createCompilationUnit(org.codehaus.groovy.control.CompilerConfiguration, java.security.CodeSource)
     */
    @Override
    protected CompilationUnit createCompilationUnit(CompilerConfiguration config, CodeSource source) {
        CompilationUnit cu = super.createCompilationUnit(config, source);

        GrailsAwareInjectionOperation operation;

        if (this.classInjectors == null) {
            operation = new GrailsAwareInjectionOperation();
        }
        else {
            operation = new GrailsAwareInjectionOperation(this.classInjectors);
        }

        cu.addPhaseOperation(operation, Phases.CANONICALIZATION);

        return cu;
    }

}
