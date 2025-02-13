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
package org.grails.web.servlet.view;

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.servlet.http.HttpServletRequest;

import groovy.lang.GroovyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import grails.util.CacheEntry;
import grails.util.GrailsStringUtils;
import grails.util.GrailsUtil;

import org.grails.gsp.GroovyPagesTemplateEngine;
import org.grails.gsp.io.GroovyPageScriptSource;
import org.grails.web.gsp.io.GrailsConventionGroovyPageLocator;
import org.grails.web.servlet.mvc.GrailsWebRequest;

/**
 * Evaluates the existence of a view for different extensions choosing which one to delegate to.
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 0.1
 */
public class GroovyPageViewResolver extends InternalResourceViewResolver implements GrailsViewResolver {

    private static final Logger LOG = LoggerFactory.getLogger(GroovyPageViewResolver.class);

    public static final String GSP_SUFFIX = ".gsp";

    public static final String JSP_SUFFIX = ".jsp";

    protected GroovyPagesTemplateEngine templateEngine;

    protected GrailsConventionGroovyPageLocator groovyPageLocator;

    private ConcurrentMap<String, CacheEntry<View>> viewCache = new ConcurrentHashMap<>();

    private boolean allowGrailsViewCaching = !GrailsUtil.isDevelopmentEnv();

    private long cacheTimeout = -1;

    private boolean resolveJspView = false;

    /**
     * Constructor.
     */
    public GroovyPageViewResolver() {
        setCache(false);
        setOrder(Ordered.LOWEST_PRECEDENCE - 20);
    }

    public GroovyPageViewResolver(GroovyPagesTemplateEngine templateEngine,
            GrailsConventionGroovyPageLocator groovyPageLocator) {
        this();
        this.templateEngine = templateEngine;
        this.groovyPageLocator = groovyPageLocator;
    }

    public void setGroovyPageLocator(GrailsConventionGroovyPageLocator groovyPageLocator) {
        this.groovyPageLocator = groovyPageLocator;
    }

    @Override
    protected View loadView(String viewName, Locale locale) throws Exception {
        Assert.notNull(this.templateEngine, "Property [templateEngine] cannot be null");
        if (viewName.endsWith(GSP_SUFFIX)) {
            viewName = viewName.substring(0, viewName.length() - GSP_SUFFIX.length());
        }

        if (!this.allowGrailsViewCaching) {
            return createGrailsView(viewName);
        }

        String viewCacheKey = this.groovyPageLocator.resolveViewFormat(viewName);

        String currentControllerKeyPrefix = resolveCurrentControllerKeyPrefixes(viewName.startsWith("/"));
        if (currentControllerKeyPrefix != null) {
            viewCacheKey = currentControllerKeyPrefix + ':' + viewCacheKey;
        }

        CacheEntry<View> entry = this.viewCache.get(viewCacheKey);

        final String lookupViewName = viewName;
        Callable<View> updater = new Callable<View>() {
            public View call() throws Exception {
                try {
                    return createGrailsView(lookupViewName);
                }
                catch (Exception e) {
                    throw new WrappedInitializationException(e);
                }
            }
        };

        View view = null;
        if (entry == null) {
            try {
                return CacheEntry.getValue(this.viewCache, viewCacheKey, this.cacheTimeout, updater);
            }
            catch (CacheEntry.UpdateException e) {
                e.rethrowCause();
                // make compiler happy
                return null;
            }
        }

        try {
            view = entry.getValue(this.cacheTimeout, updater, true, null);
        }
        catch (WrappedInitializationException e) {
            e.rethrowCause();
        }

        return view;
    }

    /**
     * @return prefix for cache key that contains current controller's context (currently plugin and namespace)
     */
    protected String resolveCurrentControllerKeyPrefixes(boolean absolute) {
        String pluginContextPath;
        String namespace;
        String controller;
        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if (webRequest != null) {
            StringBuilder stringBuilder = new StringBuilder();
            namespace = webRequest.getControllerNamespace();
            controller = webRequest.getControllerName();
            pluginContextPath = (webRequest.getAttributes() != null && webRequest.getCurrentRequest() != null)
                    ? webRequest.getAttributes().getPluginContextPath(webRequest.getCurrentRequest()) : null;

            stringBuilder.append(GrailsStringUtils.isNotEmpty(pluginContextPath) ? pluginContextPath : "-");
            stringBuilder.append(',');
            stringBuilder.append(GrailsStringUtils.isNotEmpty(namespace) ? namespace : "-");
            if (!absolute && GrailsStringUtils.isNotEmpty(controller)) {
                stringBuilder.append(',');
                stringBuilder.append(controller);
            }
            return stringBuilder.toString();
        }
        else {
            return null;
        }
    }

    protected View createGrailsView(String viewName) throws Exception {
        // try GSP if res is null

        GroovyObject controller = null;

        GrailsWebRequest webRequest = GrailsWebRequest.lookup();
        if (webRequest != null) {
            HttpServletRequest request = webRequest.getCurrentRequest();
            controller = webRequest.getAttributes().getController(request);
        }

        GroovyPageScriptSource scriptSource;
        if (controller == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Locating GSP view for path [%s]", viewName));
            }

            String absoluteViewPath = viewName.startsWith("/") ? viewName : "/" + viewName;
            scriptSource = this.groovyPageLocator.findViewByPath(absoluteViewPath);
        }
        else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Locating GSP view for controller [%s] and path [%s]", controller, viewName));
            }
            if (viewName.startsWith("/layouts")) {
                scriptSource = this.groovyPageLocator.findView(controller, viewName);
            }
            else {
                String shortViewName = viewName.substring(viewName.lastIndexOf("/") + 1);
                scriptSource = this.groovyPageLocator.findView(controller, shortViewName);
            }
        }
        if (scriptSource != null) {
            return createGroovyPageView(scriptSource.getURI(), scriptSource);
        }

        return createFallbackView(viewName);
    }


    private View createGroovyPageView(String gspView, ScriptSource scriptSource) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolved GSP view at URI [" + gspView + "]");
        }
        GroovyPageView gspSpringView = new GroovyPageView();
        gspSpringView.setServletContext(getServletContext());
        gspSpringView.setUrl(gspView);
        gspSpringView.setApplicationContext(getApplicationContext());
        gspSpringView.setTemplateEngine(this.templateEngine);
        gspSpringView.setScriptSource(scriptSource);
        try {
            gspSpringView.afterPropertiesSet();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Initialized GSP view for URI [{}]", gspView);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Error initializing GroovyPageView", e);
        }
        return gspSpringView;
    }

    protected View createFallbackView(String viewName) throws Exception {
        if (this.resolveJspView) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No GSP view found, falling back to locating JSTL view for name [{}]", viewName);
            }
            return createJstlView(viewName);
        }
        return null;
    }

    protected View createJstlView(String viewName) throws Exception {
        AbstractUrlBasedView view = buildView(viewName);
        view.setApplicationContext(getApplicationContext());
        view.afterPropertiesSet();
        return view;
    }

    @Autowired(required = true)
    @Qualifier(GroovyPagesTemplateEngine.BEAN_ID)
    public void setTemplateEngine(GroovyPagesTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public long getCacheTimeout() {
        return this.cacheTimeout;
    }

    public void setCacheTimeout(long cacheTimeout) {
        this.cacheTimeout = cacheTimeout;
    }

    @Override
    public void clearCache() {
        super.clearCache();
        this.viewCache.clear();
    }

    public boolean isAllowGrailsViewCaching() {
        return this.allowGrailsViewCaching;
    }

    public void setAllowGrailsViewCaching(boolean allowGrailsViewCaching) {
        this.allowGrailsViewCaching = allowGrailsViewCaching;
    }

    public void setResolveJspView(boolean resolveJspView) {
        this.resolveJspView = resolveJspView;
    }

    private static class WrappedInitializationException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        WrappedInitializationException(Throwable cause) {
            super(cause);
        }

        public void rethrowCause() throws Exception {
            if (getCause() instanceof Exception) {
                throw (Exception) getCause();
            }

            throw this;
        }

    }

}
