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
package org.grails.web.mapping.mvc

import java.util.concurrent.ConcurrentHashMap

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

import groovy.transform.CompileStatic
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.web.servlet.HandlerAdapter
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.InternalResourceView

import grails.core.GrailsControllerClass
import grails.util.Environment
import grails.web.mapping.LinkGenerator
import grails.web.mapping.ResponseRedirector
import grails.web.mapping.UrlMappingInfo
import grails.web.mvc.FlashScope

import org.grails.web.servlet.mvc.ActionResultTransformer
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.util.WebUtils

/**
 * A {@link HandlerAdapter} that takes a matched {@link UrlMappingInfo} and executes the underlying controller producing an appropriate model
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 3.0
 */
@CompileStatic
class UrlMappingsInfoHandlerAdapter implements HandlerAdapter, ApplicationContextAware {

    ApplicationContext applicationContext

    protected Collection<ActionResultTransformer> actionResultTransformers = []
    protected Map<String, Object> controllerCache = new ConcurrentHashMap<>()
    protected ResponseRedirector redirector

    void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    void setActionResultTransformers(Collection<ActionResultTransformer> actionResultTransformers) {
        this.actionResultTransformers.clear()
        this.actionResultTransformers.addAll(actionResultTransformers)
    }

    void setLinkGenerator(LinkGenerator linkGenerator) {
        this.redirector = new ResponseRedirector(linkGenerator)
    }

    @Override
    boolean supports(Object handler) { handler instanceof UrlMappingInfo }

    @Override
    ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UrlMappingInfo info = (UrlMappingInfo) handler

        GrailsWebRequest webRequest = GrailsWebRequest.lookup(request)

        boolean isAsyncRequest = WebUtils.isAsync(request) && !WebUtils.isError(request)
        if (isAsyncRequest) {
            Object modelAndView = request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
            if (modelAndView instanceof ModelAndView) {
                return (ModelAndView) modelAndView
            }
        }
        else {
            if (info instanceof GrailsControllerUrlMappingInfo) {
                GrailsControllerUrlMappingInfo controllerUrlMappingInfo = (GrailsControllerUrlMappingInfo) info
                GrailsControllerClass controllerClass = controllerUrlMappingInfo.controllerClass
                Object controller

                String fullName = controllerClass.fullName
                if (controllerClass.isSingleton()) {
                    controller = controllerCache.get(fullName)
                    if (controller == null) {
                        controller = applicationContext ? applicationContext.getBean(fullName) : controllerClass.newInstance()
                        if (!Environment.isReloadingAgentEnabled()) {
                            // don't cache when reloading active
                            controllerCache.put(fullName, controller)
                        }
                    }
                }
                else {
                    controller = applicationContext ? applicationContext.getBean(fullName) : controllerClass.newInstance()
                }

                String action = controllerUrlMappingInfo.actionName ?: controllerClass.defaultAction
                webRequest.actionName = webRequest.actionName ?: action
                webRequest.controllerNamespace = controllerClass.namespace
                request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)
                Object result = controllerClass.invoke(controller, action)

                if (actionResultTransformers) {
                    for (transformer in actionResultTransformers) {
                        result = transformer.transformActionResult(webRequest, action, result)
                    }
                }

                Object modelAndView = request.getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW)
                if (modelAndView instanceof ModelAndView) {
                    return (ModelAndView) modelAndView
                }
                else if (result instanceof Map) {
                    String viewName = getControllerViewName(controllerClass, action)
                    Map<String, Object> finalModel = new HashMap<String, Object>()
                    FlashScope flashScope = webRequest.getFlashScope()
                    if (!flashScope.isEmpty()) {
                        Object chainModel = flashScope.get(FlashScope.CHAIN_MODEL)
                        if (chainModel instanceof Map) {
                            finalModel.putAll((Map) chainModel)
                        }
                    }
                    finalModel.putAll((Map) result)

                    return new ModelAndView(viewName, finalModel)
                }
                else if (result instanceof ModelAndView) {
                    return (ModelAndView) result
                }
                else if (result == null && webRequest.renderView) {
                    String viewName = getControllerViewName(controllerClass, action)
                    return new ModelAndView(viewName)
                }
            }
            else if (info.viewName) {
                return new ModelAndView(info.viewName)
            }
            else if (info.redirectInfo) {
                Object i = info.redirectInfo
                if (i instanceof Map) {
                    redirector?.redirect((Map) i)
                }
                else {
                    redirector?.redirect(uri: i.toString())
                }
            }
            else if (info.getURI()) {
                return new ModelAndView(new InternalResourceView(info.getURI()))
            }
        }
        null
    }

    private String getControllerViewName(GrailsControllerClass controllerClass, String action) {
        String viewName = controllerClass.actionUriToViewName(action)
        if (controllerClass.namespace) {
            viewName = controllerClass.namespace + '/' + controllerClass.logicalPropertyName + '/' + viewName
        }
        else {
            viewName = controllerClass.logicalPropertyName + '/' + viewName
        }
        viewName
    }

    @Override
    long getLastModified(HttpServletRequest request, Object handler) {
        -1
    }

}
