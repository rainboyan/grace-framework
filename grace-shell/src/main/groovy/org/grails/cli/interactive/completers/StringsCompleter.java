/*
 * Copyright 2012-2023 the original author or authors.
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
package org.grails.cli.interactive.completers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import jline.console.completer.Completer;
import jline.internal.Preconditions;

/**
 * A completer that completes based on a collection of Strings
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public class StringsCompleter implements Completer {

    private SortedSet<String> strings = new TreeSet<>();

    public StringsCompleter() {
        // empty
    }

    public StringsCompleter(final Collection<String> strings) {
        Preconditions.checkNotNull(strings);
        getStrings().addAll(strings);
    }

    public StringsCompleter(final String... strings) {
        this(Arrays.asList(strings));
    }

    public SortedSet<String> getStrings() {
        return this.strings;
    }

    public void setStrings(SortedSet<String> strings) {
        this.strings = strings;
    }

    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        // buffer could be null
        Preconditions.checkNotNull(candidates);

        if (buffer == null) {
            candidates.addAll(getStrings());
        }
        else {
            for (String match : getStrings().tailSet(buffer)) {
                if (!match.startsWith(buffer)) {
                    break;
                }

                candidates.add(match);
            }
        }

        return candidates.isEmpty() ? -1 : 0;
    }

}
