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
package org.grails.web.mapping;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import jakarta.servlet.ServletContext;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import grails.core.GrailsApplication;
import grails.gorm.validation.Constrained;
import grails.gorm.validation.ConstrainedProperty;
import grails.web.mapping.UrlMapping;

/**
 * Abstract UrlMapping implementation that provides common basic functionality.
 *
 * @author Graeme Rocher
 * @since 0.5.5
 */
public abstract class AbstractUrlMapping implements UrlMapping {

    protected final ConstrainedProperty[] constraints;

    protected Object controllerName;

    protected Object actionName;

    protected Object namespace;

    protected Object pluginName;

    protected Object viewName;

    protected Object forwardURI;

    protected Object redirectInfo;

    protected ServletContext servletContext;

    protected GrailsApplication grailsApplication;

    @SuppressWarnings("rawtypes")
    protected Map parameterValues = Collections.emptyMap();

    protected boolean parseRequest;

    protected String mappingName;

    protected String httpMethod = ANY_HTTP_METHOD;

    protected String version = ANY_VERSION;

    protected Integer pluginIndex;

    /**
     * Base constructor required to construct a UrlMapping instance
     *
     * @param controllerName The name of the controller
     * @param actionName The name of the action
     * @param constraints Any constraints that apply to the mapping
     * @param grailsApplication The GrailsApplication instance
     */
    public AbstractUrlMapping(Object redirectInfo, Object controllerName, Object actionName, Object namespace,
            Object pluginName, Object viewName, ConstrainedProperty[] constraints, GrailsApplication grailsApplication) {
        this.controllerName = controllerName;
        this.actionName = actionName;
        this.namespace = namespace;
        this.pluginName = pluginName;
        this.constraints = constraints;
        this.viewName = viewName;
        setGrailsApplication(grailsApplication);
        this.redirectInfo = redirectInfo;
    }

    private void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
        if (grailsApplication != null) {

            ApplicationContext applicationContext = grailsApplication.getMainContext();
            if (applicationContext instanceof WebApplicationContext) {
                this.servletContext = ((WebApplicationContext) applicationContext).getServletContext();
            }
        }
    }

    protected AbstractUrlMapping(Object viewName, ConstrainedProperty[] constraints, GrailsApplication grailsApplication) {
        this.viewName = viewName;
        this.constraints = constraints;
        this.grailsApplication = grailsApplication;
        setGrailsApplication(grailsApplication);
    }

    protected AbstractUrlMapping(URI uri, ConstrainedProperty[] constraints, GrailsApplication grailsApplication) {
        this.forwardURI = uri;
        this.constraints = constraints;
        this.grailsApplication = grailsApplication;
        setGrailsApplication(grailsApplication);
    }

    @Override
    public String getHttpMethod() {
        return this.httpMethod;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    /**
     * @see UrlMapping#getConstraints()
     */
    public Constrained[] getConstraints() {
        return this.constraints;
    }

    /**
     * @see UrlMapping#getControllerName()
     */
    public Object getControllerName() {
        return this.controllerName;
    }

    /**
     * @see grails.web.mapping.UrlMapping#getActionName()
     */
    public Object getActionName() {
        return this.actionName;
    }

    public Object getPluginName() {
        return this.pluginName;
    }

    public Object getNamespace() {
        return this.namespace;
    }

    /**
     * @see grails.web.mapping.UrlMapping#getViewName()
     */
    public Object getViewName() {
        return this.viewName;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setParameterValues(Map parameterValues) {
        this.parameterValues = Collections.unmodifiableMap(parameterValues);
    }

    public void setParseRequest(boolean shouldParse) {
        this.parseRequest = shouldParse;
    }

    public String getMappingName() {
        return this.mappingName;
    }

    public void setMappingName(String name) {
        this.mappingName = name;
    }

    public boolean hasRuntimeVariable(String name) {
        if (this.constraints != null) {
            for (ConstrainedProperty cp : this.constraints) {
                if (cp.getPropertyName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Object getRedirectInfo() {
        return this.redirectInfo;
    }

    public void setPluginIndex(int pluginIndex) {
        this.pluginIndex = pluginIndex;
    }

    public Integer getPluginIndex() {
        return this.pluginIndex;
    }

    public boolean isDefinedInPlugin() {
        return this.pluginIndex != null;
    }

}
