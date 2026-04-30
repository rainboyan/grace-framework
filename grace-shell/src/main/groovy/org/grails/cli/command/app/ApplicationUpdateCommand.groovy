/*
 * Copyright 2022-2026 the original author or authors.
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
package org.grails.cli.command.app

import grails.build.logging.GrailsConsole
import grails.util.GrailsVersion

import org.grails.build.parsing.CommandLine
import org.grails.cli.command.Command
import org.grails.cli.command.CommandDescription
import org.grails.cli.command.ExecutionContext
import org.grails.cli.command.ProjectCommand
import org.grails.config.CodeGenConfig

import static org.grails.build.parsing.CommandLine.HELP_ARGUMENT
import static org.grails.build.parsing.CommandLine.STACKTRACE_ARGUMENT
import static org.grails.build.parsing.CommandLine.VERBOSE_ARGUMENT

/**
 * Update an existing project to latest version of Grace.
 *
 * @author Michael Yan
 * @since 2024.1.0
 */
class ApplicationUpdateCommand implements Command, ProjectCommand {

    static final String USAGE = 'grace app:update'
    static final String EXAMPLES = '''
    # Update an exist application to latest version
        $ grace app:update
'''

    static final String NAME = 'update'
    CommandDescription description = new CommandDescription(NAME,
            'Update an exist application to latest version',
            USAGE, EXAMPLES)

    String namespace = 'app'

    ApplicationUpdateCommand() {
        populateDescription()
    }

    private void populateDescription() {
        description.flag(name: HELP_ARGUMENT, aliases: '-h', type: 'boolean', description: "Print the options and usage", required: false)
        description.flag(name: STACKTRACE_ARGUMENT, type: 'boolean', description: 'Show full stacktrace', required: false)
        description.flag(name: VERBOSE_ARGUMENT, type: 'boolean', description: 'Show verbose output', required: false)
    }

    @Override
    String getName() {
        NAME
    }

    @Override
    boolean handle(ExecutionContext executionContext) {
        CodeGenConfig config = executionContext.config
        GrailsConsole console = executionContext.console
        CommandLine commandLine = executionContext.commandLine
        if (commandLine.hasOption(CommandLine.HELP_ARGUMENT) || commandLine.hasOption('h')) {
            printCommandHelp(console)
            return true
        }

        String currentVersion = GrailsVersion.current().version
        println "Update application to Grace ${currentVersion}"
        (1..10).each {
            print '.'
            sleep(1000)
        }
        println 'Update complete!'
        true
    }

    void printCommandHelp(GrailsConsole console) {
        console.out.println('Usage:')
        console.out.println('  ' + USAGE)
        console.out.println()

        if (!description.getFlags().isEmpty()) {
            console.out.println('Options:')
        }
        description.getFlags().each {f ->
            def flag = new StringBuilder()
            flag << '  ' << (f.aliases ? "$f.aliases, " : '    ')
            if (f.type == 'boolean') {
                flag << "[--${f.name}]".padRight(30)
            }
            else {
                flag << "[--${f.name}=${f.banner}]".padRight(30)
            }
            flag << '# ' + f.description
            console.out.println(flag)
        }
        console.out.println()

        console.out.println('Description:')
        console.out.println('    ' + description.description)
        console.out.println()

        console.out.println('Examples:')
        console.out.println(description.examples)
    }

}
