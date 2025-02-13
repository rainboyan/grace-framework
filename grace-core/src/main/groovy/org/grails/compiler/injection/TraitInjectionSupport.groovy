/*
 * Copyright 2015-2023 the original author or authors.
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
package org.grails.compiler.injection

import groovy.transform.CompileStatic

import grails.compiler.traits.TraitInjector

/**
 * Utility methods used by {@link TraitInjectionUtils}
 *
 * @author Graeme Rocher
 * @since 3.0.3
 */
@CompileStatic
class TraitInjectionSupport {

    static List<TraitInjector> resolveTraitInjectors(List<TraitInjector> injectors) {
        injectors = new ArrayList<>(injectors)
        injectors.sort { TraitInjector o1, TraitInjector o2 ->
            Class t1 = o1.trait
            Class t2 = o2.trait
            (t1 == t2) ? 0 : (o1.getClass().name.startsWith('grails.compiler.traits') ? -1 : 1)
        }
        injectors
    }

}
