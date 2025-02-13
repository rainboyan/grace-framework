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
package org.grails.web.servlet.view;

import java.util.Locale;

import jakarta.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import org.grails.web.sitemesh.GrailsLayoutView;
import org.grails.web.sitemesh.GroovyPageLayoutFinder;

public class GrailsLayoutViewResolver implements LayoutViewResolver, Ordered, ServletContextAware, ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(GrailsLayoutViewResolver.class);

    protected ViewResolver innerViewResolver;

    protected GroovyPageLayoutFinder groovyPageLayoutFinder;

    private int order = Ordered.LOWEST_PRECEDENCE - 30;

    protected ServletContext servletContext;

    public GrailsLayoutViewResolver(ViewResolver innerViewResolver, GroovyPageLayoutFinder groovyPageLayoutFinder) {
        this.innerViewResolver = innerViewResolver;
        this.groovyPageLayoutFinder = groovyPageLayoutFinder;
    }

    public GrailsLayoutViewResolver() {

    }

    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolving view for name [{}] and locale [{}]", viewName, locale);
        }
        View innerView = this.innerViewResolver.resolveViewName(viewName, locale);
        if (innerView == null) {
            return null;
        }
        else if (innerView instanceof SmartView && ((SmartView) innerView).isRedirectView()) {
            return innerView;
        }
        else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Creating layout view name [{}] and locale [{}]", viewName, locale);
            }
            return createLayoutView(innerView);
        }
    }

    protected View createLayoutView(View innerView) {
        return new GrailsLayoutView(this.groovyPageLayoutFinder, innerView);
    }

    @Override
    public ViewResolver getInnerViewResolver() {
        return this.innerViewResolver;
    }

    public int getOrder() {
        return this.order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        if (this.innerViewResolver instanceof ServletContextAware) {
            ((ServletContextAware) this.innerViewResolver).setServletContext(servletContext);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (this.innerViewResolver instanceof ApplicationContextAware) {
            ((ApplicationContextAware) this.innerViewResolver).setApplicationContext(applicationContext);
        }
    }

    public void setInnerViewResolver(ViewResolver innerViewResolver) {
        this.innerViewResolver = innerViewResolver;
    }

    public void setGroovyPageLayoutFinder(GroovyPageLayoutFinder groovyPageLayoutFinder) {
        this.groovyPageLayoutFinder = groovyPageLayoutFinder;
    }

}
