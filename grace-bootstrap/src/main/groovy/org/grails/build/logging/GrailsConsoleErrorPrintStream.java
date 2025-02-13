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
package org.grails.build.logging;

import java.io.PrintStream;

import grails.build.logging.GrailsConsole;

/**
 * Used to replace default System.err with one that routes calls through GrailsConsole.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsConsoleErrorPrintStream extends PrintStream {

    public GrailsConsoleErrorPrintStream(PrintStream out) {
        super(out, true);
    }

    public PrintStream getTargetOut() {
        return (PrintStream) out;
    }

    @Override
    public void print(Object o) {
        if (o != null) {
            GrailsConsole.getInstance().error(o.toString());
        }
    }

    @Override
    public void print(String s) {
        GrailsConsole.getInstance().error(s);
    }

    @Override
    public void println(String s) {
        GrailsConsole.getInstance().error(s);
    }

    @Override
    public void println(Object o) {
        if (o != null) {
            GrailsConsole.getInstance().error(o.toString());
        }
    }

}
