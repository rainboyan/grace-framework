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
package org.grails.cli.profile.commands.events

import groovy.transform.CompileStatic
import groovy.transform.Generated

import org.grails.cli.profile.commands.script.GroovyScriptCommand

/**
 * Allows for listening and reacting to events triggered by other commands
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
trait CommandEvents {

    /**
     * Register to listen for an event
     *
     * @param eventName The name of the event
     * @param callable The closure that is executed when the event is fired
     */
    @Generated
    void on(String eventName, @DelegatesTo(GroovyScriptCommand) Closure callable) {
        EventStorage.registerEvent(eventName, callable)
    }

    /**
     * Register to listen for an event that runs before the given command
     *
     * @param eventName The name of the event
     * @param callable The closure that is executed when the event is fired
     */
    @Generated
    void before(String commandName, @DelegatesTo(GroovyScriptCommand) Closure callable) {
        EventStorage.registerEvent("${commandName}Start", callable)
    }

    /**
     * Register to listen for an event that runs before the given command
     *
     * @param eventName The name of the event
     * @param callable The closure that is executed when the event is fired
     */
    @Generated
    void after(String commandName, @DelegatesTo(GroovyScriptCommand) Closure callable) {
        EventStorage.registerEvent("${commandName}End", callable)
    }

    /**
     * Notify of an event
     *
     * @param eventName The name of the event
     * @param args The arguments to the event
     */
    @Generated
    void notify(String eventName, Object... args) {
        EventStorage.fireEvent(this, eventName, args)
    }

}
