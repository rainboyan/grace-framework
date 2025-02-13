/*
 * Copyright 2011-2023 the original author or authors.
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

import groovy.ant.AntBuilder;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.MagicNames;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.LogLevel;

import grails.build.logging.GrailsConsole;

/**
 * Silences ant builder output.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
public class GrailsConsoleAntBuilder extends AntBuilder {

    public GrailsConsoleAntBuilder(Project project) {
        super(project);
    }

    public GrailsConsoleAntBuilder() {
        super(createAntProject());
    }

    /**
     * @return Factory method to create new Project instances
     */
    protected static Project createAntProject() {
        Project project = new Project();

        ProjectHelper helper = ProjectHelper.getProjectHelper();
        project.addReference(MagicNames.REFID_PROJECT_HELPER, helper);
        helper.getImportStack().addElement("AntBuilder"); // import checks that stack is not empty

        addGrailsConsoleBuildListener(project);

        project.init();
        project.getBaseDir();
        return project;
    }

    public static void addGrailsConsoleBuildListener(Project project) {
        BuildLogger logger = new GrailsConsoleLogger();

        logger.setMessageOutputLevel(Project.MSG_INFO);
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);

        project.addBuildListener(logger);

        GrailsConsole instance = GrailsConsole.getInstance();
        project.addBuildListener(new GrailsConsoleBuildListener(instance));

        if (!instance.isVerbose()) {
            for (Object buildListener : project.getBuildListeners()) {
                if (buildListener instanceof BuildLogger) {
                    ((BuildLogger) buildListener).setMessageOutputLevel(LogLevel.ERR.getLevel());
                }
            }
        }
    }

    private static class GrailsConsoleLogger extends DefaultLogger {

        /**
         * Name of the current target, if it should
         * be displayed on the next message. This is
         * set when a target starts building, and reset
         * to <code>null</code> after the first message for
         * the target is logged.
         */
        protected String targetName;

        protected GrailsConsole console = GrailsConsole.getInstance();

        /**
         * Notes the name of the target so it can be logged
         * if it generates any messages.
         *
         * @param event A BuildEvent containing target information.
         *              Must not be <code>null</code>.
         */
        @Override
        public void targetStarted(BuildEvent event) {
            this.targetName = event.getTarget().getName();
        }

        /**
         * Resets the current target name to <code>null</code>.
         *
         * @param event Ignored in this implementation.
         */
        @Override
        public void targetFinished(BuildEvent event) {
            this.targetName = null;
        }

        /**
         * Logs a message for a target if it is of an appropriate
         * priority, also logging the name of the target if this
         * is the first message which needs to be logged for the
         * target.
         *
         * @param event A BuildEvent containing message information.
         *              Must not be <code>null</code>.
         */
        @Override
        public void messageLogged(BuildEvent event) {
            if (event.getPriority() > this.msgOutputLevel ||
                    null == event.getMessage() ||
                    "".equals(event.getMessage().trim())) {
                return;
            }

            if (this.targetName != null) {
                this.console.verbose(System.lineSeparator() + this.targetName + ":");
                this.targetName = null;
            }
        }

    }

}
