/*
 * Copyright 2014-2023 the original author or authors.
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
package org.grails.cli.compiler.dependencies;

import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.locator.DefaultModelLocator;

/**
 * DependencyManagement derived from the effective pom of
 * {@code grails-bom}.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
public class GrailsDependenciesDependencyManagement extends MavenModelDependencyManagement {

    public GrailsDependenciesDependencyManagement() {
        super(readModel());
    }

    private static Model readModel() {
        DefaultModelProcessor modelProcessor = new DefaultModelProcessor();
        modelProcessor.setModelLocator(new DefaultModelLocator());
        modelProcessor.setModelReader(new DefaultModelReader());

        try {
            return modelProcessor.read(GrailsDependenciesDependencyManagement.class
                    .getResourceAsStream("grace-bom-effective.xml"), null);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Failed to build model from effective pom", ex);
        }
    }

    public String getGrailsVersion() {
        return find("grace-core").getVersion();
    }

    public String getGroovyVersion() {
        return find("groovy-bom").getVersion();
    }

    public String getGormVersion() {
        return find("grace-datastore-core").getVersion();
    }

    @Override
    public String getSpringBootVersion() {
        return find("spring-boot-dependencies").getVersion();
    }

}
