/*
 * Copyright 2011-2022 the original author or authors.
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
package org.grails.core.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;

/**
 *
 * A static resource locator that uses an internal map to locate resources. Used largely for testing.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class StaticResourceLocator implements ResourceLocator {

    private Map<String, Resource> classToResourceMap = new HashMap<>();

    public void setSearchLocation(String searchLocation) {
        // do nothing
    }

    public void setSearchLocations(Collection<String> searchLocations) {
        // do nothing
    }

    public Resource findResourceForURI(String uri) {
        return null; // TODO implement static resource location
    }

    public Resource findResourceForClassName(String className) {
        return this.classToResourceMap.get(className);
    }

    public void addClassResource(String className, Resource res) {
        this.classToResourceMap.put(className, res);
    }

}
