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
package org.grails.cli.interactive.completers

import groovy.transform.CompileStatic
import jline.console.completer.Completer

/**
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class ClosureCompleter implements Completer {

    private final Closure<List<String>> closure
    private Completer completer

    ClosureCompleter(Closure<List<String>> closure) {
        this.closure = closure
    }

    Completer getCompleter() {
        if (completer == null) {
            completer = new jline.console.completer.StringsCompleter(closure.call())
        }
        completer
    }

    @Override
    int complete(String buffer, int cursor, List<CharSequence> candidates) {
        getCompleter().complete(buffer, cursor, candidates)
    }

}
