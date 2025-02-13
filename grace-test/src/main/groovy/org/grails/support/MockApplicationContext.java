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
package org.grails.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.ServletContext;

import groovy.lang.GroovyObjectSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StandardServletEnvironment;

import grails.util.GrailsStringUtils;

@SuppressWarnings("unchecked")
public class MockApplicationContext extends GroovyObjectSupport implements WebApplicationContext {

    Date startupDate = new Date();

    Map<String, Object> beans = new HashMap<>();

    List<Resource> resources = new ArrayList<>();

    List<String> ignoredClassLocations = new ArrayList<>();

    PathMatcher pathMatcher = new AntPathMatcher();

    ServletContext servletContext = new MockServletContext();

    public void registerMockBean(String name, Object instance) {
        this.beans.put(name, instance);
    }

    /**
     * Registers a mock resource. Path separator: "/"
     * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
     */
    public void registerMockResource(String location) {
        this.resources.add(new ClassPathResource(GrailsStringUtils.trimStart(location, "/")));
    }

    /**
     * Registers a mock resource. Path separator: "/"
     * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
     */
    public void registerMockResource(String location, String contents) {
        this.resources.add(new MockResource(GrailsStringUtils.trimStart(location, "/"), contents));
    }

    /**
     * Unregisters a mock resource. Path separator: "/"
     * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
     */
    public void unregisterMockResource(String location) {
        for (Iterator<Resource> it = this.resources.iterator(); it.hasNext(); ) {
            MockResource mockResource = (MockResource) it.next();
            if (mockResource.location.equals(location)) {
                it.remove();
            }
        }
    }

    /**
     * Registers a resource that should not be found on the classpath. Path separator: "/"
     * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
     */
    public void registerIgnoredClassPathLocation(String location) {
        this.ignoredClassLocations.add(location);
    }

    /**
     * Unregisters a resource that should not be found on the classpath. Path separator: "/"
     * @param location the location of the resource. Example: /WEB-INF/grails-app/i18n/messages.properties
     */
    public void unregisterIgnoredClassPathLocation(String location) {
        this.ignoredClassLocations.remove(location);
    }

    public ApplicationContext getParent() {
        throw new UnsupportedOperationException("Method not supported by implementation");
    }

    public String getId() {
        return "MockApplicationContext";
    }

    public String getApplicationName() {
        return getId();
    }

    public String getDisplayName() {
        return getId();
    }

    public long getStartupDate() {
        return this.startupDate.getTime();
    }

    public void publishEvent(ApplicationEvent event) {
        // do nothing
    }

    @Override
    public void publishEvent(Object event) {

    }

    public boolean containsBeanDefinition(String beanName) {
        return this.beans.containsKey(beanName);
    }

    public int getBeanDefinitionCount() {
        return this.beans.size();
    }

    public String[] getBeanDefinitionNames() {
        return this.beans.keySet().toArray(new String[0]);
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
        return getBeanProvider(requiredType);
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
        return getBeanProvider(requiredType);
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type) {
        return new String[0];
    }

    @Override
    public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
        return new String[0];
    }

    @SuppressWarnings({ "unchecked" })
    public String[] getBeanNamesForType(Class type) {
        List<String> beanNames = new ArrayList<>();
        for (String beanName : this.beans.keySet()) {
            if (type.isAssignableFrom(this.beans.get(beanName).getClass())) {
                beanNames.add(beanName);
            }
        }
        return beanNames.toArray(new String[0]);
    }

    public String[] getBeanNamesForType(Class type, boolean includePrototypes, boolean includeFactoryBeans) {
        return getBeanNamesForType(type);
    }

    @SuppressWarnings("unchecked")
    public <T> Map<String, T> getBeansOfType(Class<T> type) throws BeansException {
        String[] beanNames = getBeanNamesForType(type);
        Map<String, T> newMap = new HashMap<>();
        for (String beanName : beanNames) {
            newMap.put(beanName, (T) getBean(beanName));
        }
        return newMap;
    }

    public <T> Map<String, T> getBeansOfType(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
        return getBeansOfType(type);
    }

    public <A extends Annotation> A findAnnotationOnBean(String name, Class<A> annotation) {
        Object o = getBean(name);
        if (o != null) {
            return o.getClass().getAnnotation(annotation);
        }
        return null;
    }

    @Override
    public <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
            throws NoSuchBeanDefinitionException {
        return findAnnotationOnBean(beanName, annotationType);
    }

    @Override
    public <A extends Annotation> Set<A> findAllAnnotationsOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
        return null;
    }

    public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotation) throws BeansException {
        Map<String, Object> submap = new HashMap<>();
        for (Object beanName : this.beans.keySet()) {
            Object bean = this.beans.get(beanName);
            if (bean != null && bean.getClass().getAnnotation(annotation) != null) {
                submap.put(beanName.toString(), bean);
            }
        }
        return submap;
    }

    /**
     * Find all names of beans whose {@code Class} has the supplied {@link Annotation}
     * type, without creating any bean instances yet.
     * @param annotationType the type of annotation to look for
     * @return the names of all matching beans
     * @since 2.4
     */
    public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        List<String> beanNamesList = new ArrayList<>();
        for (Object beanName : this.beans.keySet()) {
            Object bean = this.beans.get(beanName);
            if (bean != null && bean.getClass().getAnnotation(annotationType) != null) {
                beanNamesList.add(beanName.toString());
            }
        }
        return beanNamesList.toArray(new String[0]);
    }

    public Object getBean(String name) throws BeansException {
        if (!this.beans.containsKey(name)) {
            throw new NoSuchBeanDefinitionException(name);
        }
        return this.beans.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
        if (!this.beans.containsKey(name)) {
            throw new NoSuchBeanDefinitionException(name);
        }

        if (requiredType != null && !requiredType.isAssignableFrom(this.beans.get(name).getClass())) {
            throw new NoSuchBeanDefinitionException(name);
        }

        return (T) this.beans.get(name);
    }

    public <T> T getBean(Class<T> tClass) throws BeansException {
        Map<String, T> map = getBeansOfType(tClass);
        if (map.isEmpty()) {
            throw new NoSuchBeanDefinitionException(tClass, "No bean found for type: " + tClass.getName());
        }
        return map.values().iterator().next();
    }

    public Object getBean(String name, Object... args) throws BeansException {
        return getBean(name);
    }

    @Override
    public Object getProperty(String name) {
        if (this.beans.containsKey(name)) {
            return this.beans.get(name);
        }

        return super.getProperty(name);
    }

    public boolean containsBean(String name) {
        return this.beans.containsKey(name);
    }

    public boolean isSingleton(String name) {
        throw new UnsupportedOperationException("Method not supported by implementation");
    }

    public boolean isPrototype(String s) {
        throw new UnsupportedOperationException("Method not supported by implementation");
    }

    @Override
    public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
        return isTypeMatch(name, typeToMatch.getRawClass());
    }

    public boolean isTypeMatch(String name, Class aClass) {
        return aClass.isInstance(getBean(name));
    }

    @SuppressWarnings({ "rawtypes" })
    public Class getType(String name) throws NoSuchBeanDefinitionException {
        if (!this.beans.containsKey(name)) {
            throw new NoSuchBeanDefinitionException(name);
        }

        return this.beans.get(name).getClass();
    }

    @Override
    public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
        return getType(name);
    }

    public String[] getAliases(String name) {
        return new String[] {};
    }

    public BeanFactory getParentBeanFactory() {
        return null;
    }

    public String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        MessageSource messageSource = (MessageSource) getBean("messageSource");
        if (messageSource == null) {
            throw new BeanCreationException("No bean [messageSource] found in MockApplicationContext");
        }
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

    public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException {
        MessageSource messageSource = (MessageSource) getBean("messageSource");
        if (messageSource == null) {
            throw new BeanCreationException("No bean [messageSource] found in MockApplicationContext");
        }
        return messageSource.getMessage(code, args, locale);
    }

    public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException {
        MessageSource messageSource = (MessageSource) getBean("messageSource");
        if (messageSource == null) {
            throw new BeanCreationException("No bean [messageSource] found in MockApplicationContext");
        }
        return messageSource.getMessage(resolvable, locale);
    }

    public Resource[] getResources(String locationPattern) throws IOException {
        if (locationPattern.startsWith("classpath:") || locationPattern.startsWith("file:")) {
            throw new UnsupportedOperationException("Location patterns 'classpath:' and 'file:' not supported by implementation");
        }

        locationPattern = GrailsStringUtils.trimStart(locationPattern, "/"); // starting with "**/" is OK
        List<Resource> result = new ArrayList<>();
        for (Resource res : this.resources) {
            String path = res instanceof ClassPathResource ? ((ClassPathResource) res).getPath() : res.getDescription();
            if (this.pathMatcher.match(locationPattern, path)) {
                result.add(res);
            }
        }
        return result.toArray(new Resource[0]);
    }

    public Resource getResource(String location) {
        for (Resource mockResource : this.resources) {
            if (this.pathMatcher.match(mockResource.getDescription(), GrailsStringUtils.trimStart(location, "/"))) {
                return mockResource;
            }
        }
        // Check for ignored resources and return null instead of a classpath resource in that case.
        for (String resourceLocation : this.ignoredClassLocations) {
            if (this.pathMatcher.match(
                    GrailsStringUtils.trimStart(location, "/"),
                    GrailsStringUtils.trimStart(resourceLocation, "/"))) {
                return null;
            }
        }

        return new ClassPathResource(location);
    }

    public boolean containsLocalBean(String arg0) {
        throw new UnsupportedOperationException("Method not supported by implementation");
    }

    public AutowireCapableBeanFactory getAutowireCapableBeanFactory() throws IllegalStateException {
        return new DefaultListableBeanFactory();
    }

    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    public ServletContext getServletContext() {
        return this.servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public Environment getEnvironment() {
        return new StandardServletEnvironment();
    }

    @Override
    public <T> T getBean(Class<T> requiredType, Object... args)
            throws BeansException {
        return getBean(requiredType);
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
        return new ObjectProvider<T>() {
            @Override
            public T getObject(Object... args) throws BeansException {
                return getBean(requiredType);
            }

            @Override
            public T getIfAvailable() throws BeansException {
                return getBean(requiredType);
            }

            @Override
            public T getIfUnique() throws BeansException {
                return getBean(requiredType);
            }

            @Override
            public T getObject() throws BeansException {
                return getBean(requiredType);
            }
        };
    }

    @Override
    public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
        return new ObjectProvider<T>() {
            @Override
            public T getObject(Object... args) throws BeansException {
                return (T) getBean(requiredType.toClass());
            }

            @Override
            public T getIfAvailable() throws BeansException {
                return (T) getBean(requiredType.toClass());
            }

            @Override
            public T getIfUnique() throws BeansException {
                return (T) getBean(requiredType.toClass());
            }

            @Override
            public T getObject() throws BeansException {
                return (T) getBean(requiredType.toClass());
            }
        };
    }

    public class MockResource extends AbstractResource {

        private String contents = "";

        private String location;

        public MockResource(String location) {
            this.location = location;
        }

        public MockResource(String location, String contents) {
            this(location);
            this.contents = contents;
        }

        @Override
        public boolean exists() {
            return true;
        }

        public String getDescription() {
            return this.location;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(this.contents.getBytes(StandardCharsets.UTF_8));
        }

    }

}
