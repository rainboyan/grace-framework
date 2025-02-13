/*
 * Copyright 2006-2023 the original author or authors.
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

import java.io.Writer;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;

import org.grails.web.servlet.mvc.GrailsWebRequest;
import org.grails.web.util.WebUtils;

/**
 * Delegates calls to a passed GrailsWebRequest instance.
 *
 * @author Graeme Rocher
 * @since 0.6
 */
public class WebRequestDelegatingRequestContext implements GrailsRequestContext {

    private final GrailsWebRequest webRequest;

    public WebRequestDelegatingRequestContext() {
        this.webRequest = (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
    }

    /**
     * Retrieves the webRequest object.
     * @return The webrequest object
     */
    public GrailsWebRequest getWebRequest() {
        return this.webRequest;
    }

    public HttpServletRequest getRequest() {
        return this.webRequest.getCurrentRequest();
    }

    public HttpServletResponse getResponse() {
        return this.webRequest.getCurrentResponse();
    }

    public HttpSession getSession() {
        return this.webRequest.getSession();
    }

    public ServletContext getServletContext() {
        return this.webRequest.getServletContext();
    }

    @SuppressWarnings("rawtypes")
    public Map getParams() {
        return this.webRequest.getParams();
    }

    public ApplicationContext getApplicationContext() {
        ServletContext servletContext = getServletContext();
        return WebApplicationContextUtils.getWebApplicationContext(servletContext);
    }

    public Writer getOut() {
        return this.webRequest.getOut();
    }

    public String getActionName() {
        return this.webRequest.getActionName();
    }

    public String getControllerName() {
        return this.webRequest.getControllerName();
    }

    public String getRequestURI() {
        HttpServletRequest request = getRequest();
        String uri = (String) request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE);
        if (uri == null) {
            uri = request.getRequestURI();
        }

        return uri;
    }

}
