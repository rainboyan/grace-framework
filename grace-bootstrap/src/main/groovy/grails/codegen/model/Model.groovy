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
package grails.codegen.model

/**
 * The model of the naming conventions of a class used for Codegen
 *
 * @author Graeme Rocher
 * @since 3.0
 */
interface Model {

    /**
     * @return The class name excluding package
     */
    String getClassName()

    /**
     * @return The class name including package
     */
    String getFullName()

    /**
     * @return The package name
     */
    String getPackageName()

    /**
     * @return The package name
     */
    String getPackagePath()

    /**
     * @return The class name without package
     */
    String getSimpleName()

    /**
     * A property name version of the class name. For example 'FooBar' becomes 'fooBar'
     *
     * @return The property name version of the class name
     */
    String getPropertyName()

    /**
     * A property name version of the class name. For example 'FooBar' becomes 'fooBar'
     *
     * @return The property name version of the class name
     */
    String getModelName()

    /**
     * A lower case version of the class name separated by hyphens. For example 'FooBar' becomes 'foo-bar'
     *
     * @return Lower case version of the class name
     */
    String getLowerCaseName()

    /**
     * Returns the convention of this model for the given name.
     * For example given a {@link #getSimpleName()} of "Foo" this method will return "FooController" where the name argument is "Controller"
     * @param conventionName The name
     * @return The convention for the given convention name
     */
    String convention(String conventionName)

    /**
     * @return The model as a map
     */
    Map<String, Object> asMap()

}
