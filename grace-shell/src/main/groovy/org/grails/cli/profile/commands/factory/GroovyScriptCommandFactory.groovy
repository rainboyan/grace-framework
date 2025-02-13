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

import java.nio.charset.StandardCharsets

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer

import grails.build.logging.GrailsConsole
import grails.util.GrailsNameUtils

import org.grails.cli.profile.Command
import org.grails.cli.profile.Profile
import org.grails.cli.profile.commands.script.GroovyScriptCommand
import org.grails.cli.profile.commands.script.GroovyScriptCommandTransform
import org.grails.io.support.Resource

/**
 * A {@link CommandFactory} that creates {@link Command} instances from Groovy scripts
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class GroovyScriptCommandFactory extends ResourceResolvingCommandFactory<GroovyScriptCommand> {

    final Collection<String> matchingFileExtensions = ['groovy']
    final String fileNamePattern = /^.*\.(groovy)$/

    @Override
    protected GroovyScriptCommand readCommandFile(Resource resource) {
        GroovyClassLoader classLoader = createGroovyScriptCommandClassLoader()
        try {
            return (GroovyScriptCommand) classLoader.parseClass(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8), resource.filename).newInstance()
        }
        catch (Throwable e) {
            GrailsConsole.getInstance().error("Failed to compile ${resource.filename}: " + e.getMessage(), e)
        }
    }

    @CompileDynamic
    static GroovyClassLoader createGroovyScriptCommandClassLoader() {
        def configuration = new CompilerConfiguration()
        // TODO: Report bug, this fails with @CompileStatic with a ClassCastException
        String baseClassName = GroovyScriptCommand.getName()
        createClassLoaderForBaseClass(configuration, baseClassName)
    }

    private static GroovyClassLoader createClassLoaderForBaseClass(CompilerConfiguration configuration, String baseClassName) {
        configuration.setScriptBaseClass(baseClassName)

        def importCustomizer = new ImportCustomizer()
        importCustomizer.addStarImports('org.grails.cli.interactive.completers')
        importCustomizer.addStarImports('grails.util')
        importCustomizer.addStarImports('grails.codegen.model')
        configuration.addCompilationCustomizers(importCustomizer, new ASTTransformationCustomizer(new GroovyScriptCommandTransform()))
        def classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader, configuration)
        classLoader
    }

    @Override
    protected String evaluateFileName(String fileName) {
        String fn = super.evaluateFileName(fileName)
        fn.contains('-') ? fn.toLowerCase() : GrailsNameUtils.getScriptName(fn)
    }

    @Override
    protected Command createCommand(Profile profile, String commandName, Resource resource, GroovyScriptCommand data) {
        data.setProfile(profile)
        data
    }

}
