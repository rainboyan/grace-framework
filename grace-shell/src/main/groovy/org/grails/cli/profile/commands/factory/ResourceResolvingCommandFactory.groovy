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
package org.grails.cli.profile.commands.factory

import java.util.regex.Pattern

import groovy.transform.CompileStatic

import grails.util.BuildSettings

import org.grails.cli.profile.Command
import org.grails.cli.profile.Profile
import org.grails.io.support.FileSystemResource
import org.grails.io.support.Resource

/**
 * A abstract {@link CommandFactory} that reads from the file system
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
abstract class ResourceResolvingCommandFactory<T> implements CommandFactory {

    @Override
    Collection<Command> findCommands(Profile profile, boolean inherited) {
        Collection<Resource> resources = findCommandResources(profile, inherited)
        Collection<Command> commands = []
        for (Resource resource in resources) {
            String commandName = evaluateFileName(resource.filename)
            T data = readCommandFile(resource)

            Command command = createCommand(profile, commandName, resource, data)
            if (command) {
                commands << command
            }
        }
        commands
    }

    protected String evaluateFileName(String fileName) {
        fileName - Pattern.compile(/\.(${getMatchingFileExtensions().join('|')})$/)
    }

    protected Collection<Resource> findCommandResources(Profile profile, boolean inherited) {
        Collection<Resource> allResources = []
        for (CommandResourceResolver resolver in getCommandResolvers(inherited)) {
            allResources.addAll resolver.findCommandResources(profile)
        }
        allResources
    }

    protected Collection<CommandResourceResolver> getCommandResolvers(boolean inherited) {
        def profileCommandsResolver = new FileSystemCommandResourceResolver(matchingFileExtensions)
        Collection<CommandResourceResolver> commandResolvers = []
        if (inherited) {
            commandResolvers.add(profileCommandsResolver)
            return commandResolvers
        }

        def localCommandsResolver1 = new FileSystemCommandResourceResolver(matchingFileExtensions) {

            @Override
            protected Resource getCommandsDirectory(Profile profile) {
                new FileSystemResource("${BuildSettings.BASE_DIR}/src/main/scripts/")
            }

        }
        def localCommandsResolver2 = new FileSystemCommandResourceResolver(matchingFileExtensions) {

            @Override
            protected Resource getCommandsDirectory(Profile profile) {
                new FileSystemResource("${BuildSettings.BASE_DIR}/commands/")
            }

        }
        commandResolvers.addAll([profileCommandsResolver, localCommandsResolver1, localCommandsResolver2,
                                 new ClasspathCommandResourceResolver(matchingFileExtensions)])
        commandResolvers
    }

    protected abstract T readCommandFile(Resource resource)

    protected abstract Command createCommand(Profile profile, String commandName, Resource resource, T data)

    protected abstract Collection<String> getMatchingFileExtensions()

}
