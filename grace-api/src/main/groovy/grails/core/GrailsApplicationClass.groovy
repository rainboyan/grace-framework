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
package grails.core

/**
 * Marker interface for the GrailsApplicationClass
 *
 * @author Graeme Rocher
 * @since 3.0.10
 */
trait GrailsApplicationClass implements GrailsApplicationLifeCycle {

    @Override
    Closure doWithSpring() { null }

    @Override
    void doWithDynamicMethods() {
        // no-op
    }

    @Override
    void doWithApplicationContext() {
        // no-op
    }

    @Override
    void onConfigChange(Map<String, Object> event) {
        // no-op
    }

    @Override
    void onStartup(Map<String, Object> event) {
        // no-op
    }

    @Override
    void onShutdown(Map<String, Object> event) {
        // no-op
    }

}
