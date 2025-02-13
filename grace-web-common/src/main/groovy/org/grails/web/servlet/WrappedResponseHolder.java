/*
 * Copyright 2004-2023 the original author or authors.
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
package org.grails.web.servlet;

import jakarta.servlet.http.HttpServletResponse;

import org.grails.core.lifecycle.ShutdownOperations;

/**
 * A holder for the original Wrapped response for use when using includes.
 *
 * @author Graeme Rocher
 * @since 0.5
 */
public final class WrappedResponseHolder {

    private static ThreadLocal<HttpServletResponse> wrappedResponseHolder = new ThreadLocal<>();

    static {
        ShutdownOperations.addOperation(() -> wrappedResponseHolder = new ThreadLocal<>(), true);
    }

    private WrappedResponseHolder() {
    }

    /**
     * Bind the given HttpServletResponse to the current thread.
     * @param response the HttpServletResponse to expose
     */
    public static void setWrappedResponse(HttpServletResponse response) {
        WrappedResponseHolder.wrappedResponseHolder.set(response);
    }

    /**
     * Return the HttpServletResponse currently bound to the thread.
     * @return the HttpServletResponse currently bound to the thread, or <code>null</code>
     */
    public static HttpServletResponse getWrappedResponse() {
        return wrappedResponseHolder.get();
    }

}
