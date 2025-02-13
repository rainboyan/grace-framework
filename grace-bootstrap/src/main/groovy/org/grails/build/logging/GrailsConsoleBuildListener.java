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

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;

import grails.build.logging.GrailsConsole;

/**
 * Mainly silences a lot of redundant Ant output.
 */
public class GrailsConsoleBuildListener implements BuildListener {

    private final GrailsConsole ui;

    public GrailsConsoleBuildListener() {
        this(GrailsConsole.getInstance());
    }

    public GrailsConsoleBuildListener(GrailsConsole ui) {
        this.ui = ui;
    }

    /**
     * <p>Signals that a build has started. This event
     * is fired before any targets have started.</p>
     *
     * @param start An event with any relevant extra information.
     *              Must not be <code>null</code>.
     */
    public final void buildStarted(final BuildEvent start) {
        // ignore
    }

    /**
     * <p>Signals that the last target has finished. This event
     * will still be fired if an error occurred during the build.</p>
     *
     * @param finish An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getException()
     */
    public final void buildFinished(final BuildEvent finish) {
        // ignore
    }

    /**
     * <p>Signals that a target is starting.</p>
     *
     * @param start An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getTarget()
     */
    public final void targetStarted(final BuildEvent start) {
        // ignore
    }

    /**
     * <p>Signals that a target has finished. This event will
     * still be fired if an error occurred during the build.</p>
     *
     * @param finish An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getException()
     */
    public final void targetFinished(final BuildEvent finish) {
        // ignore
    }

    /**
     * <p>Signals that a task is starting.</p>
     *
     * @param start An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getTask()
     */
    public final void taskStarted(final BuildEvent start) {
        // ignore
    }

    /**
     * <p>Signals that a task has finished. This event will still
     * be fired if an error occurred during the build.</p>
     *
     * @param finish An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getException()
     */
    public final void taskFinished(final BuildEvent finish) {
        this.ui.indicateProgress();
    }

    /** <p>When a message is sent to this logger, Ant calls this method.</p>
     * @param event An event with any relevant extra information.
     *              Must not be <code>null</code>.
     *
     * @see BuildEvent#getMessage()
     * @see BuildEvent#getPriority()
     */
    public void messageLogged(final BuildEvent event) {
        // empty
    }

}
