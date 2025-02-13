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
package org.grails.plugins;

import grails.core.DefaultGrailsApplication;
import grails.core.GrailsApplication;
import grails.plugins.GrailsPlugin;

import org.grails.config.PropertySourcesConfig;
import org.grails.spring.DefaultRuntimeSpringConfiguration;
import org.grails.spring.RuntimeSpringConfiguration;
import org.grails.support.MockApplicationContext;

import grails.plugins.exceptions.PluginException;

import groovy.lang.ExpandoMetaClass;
import groovy.lang.GroovyClassLoader;
import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ClassUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for the DefaultGrailsPlugin class
 *
 * @author Graeme Rocher
 * @author Michael Yan
 */
public class DefaultGrailsPluginTests {
    GroovyClassLoader gcl = new GroovyClassLoader();
    DefaultGrailsApplication ga;
    MockApplicationContext ctx;

    private Class<?> versioned;
    private Class<?> notVersion;
    private Class<?> notPluginClass;
    private Class<?> disabled;
    private Class<?> observed;
    private Class<?> camelCased;
    private Class<?> versioned2;
    private Class<?> versioned3;
    private Class<?> versioned4;

    @BeforeEach
    @SuppressWarnings("unchecked")
    protected void setUp() throws Exception {
        ExpandoMetaClass.enableGlobally();

        ctx = new MockApplicationContext();
        ctx.registerMockBean(GrailsApplication.CLASS_LOADER_BEAN, gcl);
        onSetUp();
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(), gcl);
        if(ClassUtils.isPresent("Config", gcl)) {
            ConfigObject config = new ConfigSlurper().parse(gcl.loadClass("Config"));
            ga.setConfig(new PropertySourcesConfig(config));
        }
        ga.setApplicationContext(ctx);
        ga.initialise();
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga);
    }

    @AfterEach
    protected void tearDown() throws Exception {
        ExpandoMetaClass.disableGlobally();
    }

    protected void onSetUp() {
        versioned = gcl.parseClass("class MyGrailsPlugin {\n" +
                        "def version = 1.1;" +
                        "def watchedResources = [\"file:./grails-app/taglib/**/*TagLib.groovy\", \"./grails-app/controller/**/*.groovy\", \"file:/absolutePath/**/*.gsp\" ]\n" +
                        "def doWithSpring = {" +
                        "classEditor(org.springframework.beans.propertyeditors.ClassEditor,application.classLoader)" +
                        "}\n" +
                        "def doWithApplicationContext = { ctx ->" +
                        "assert ctx != null" +
                        "}\n" +
                        "def onChange = { event ->" +
                        "assert event != null" +
                        "}" +
                        "}");
        notVersion = gcl.parseClass("class AnotherGrailsPlugin {\n" +
                        "}");
        notPluginClass = gcl.parseClass("class SomeOtherPlugin {\n" +
                                        "def version = 1.4;" +
                                        "}");

        disabled = gcl.parseClass("class DisabledGrailsPlugin {" +
                                  "def version = 1.1; " +
                                  "def status = 'disabled'; }");

        observed = gcl.parseClass("class ObservingGrailsPlugin {" +
                                  "def observe = ['another'];" +
                                  "def version = 1.1; " +
                                  "def status = 'disabled'; }");

        camelCased = gcl.parseClass("class CamelCasedGrailsPlugin {" +
                                    "def version = 2.1; " +
                                    "def status = 'disabled'; }");
        versioned2 = gcl.parseClass("class MyTwoGrailsPlugin extends grails.plugins.Plugin {\n" +
                "def version = 1.1;" +
                "Closure doWithSpring() { {->" +
                "classEditor(org.springframework.beans.propertyeditors.ClassEditor,application.classLoader)" +
                "}}\n" +
                "}");
        versioned3 = gcl.parseClass("class MyThreeGrailsPlugin extends grails.plugins.DynamicPlugin {\n" +
                "def version = 1.1;" +
                "Closure doWithSpring() { {->" +
                "classEditor(org.springframework.beans.propertyeditors.ClassEditor,application.classLoader)" +
                "}}\n" +
                "}");
        versioned4 = gcl.parseClass("class MyFourthGrailsPlugin {\n" +
                "def version = 1.1;" +
                "Closure doWithSpring() { {->\n" +
                "Properties properties = loadProperties()\n" +
                "classEditor(org.springframework.beans.propertyeditors.ClassEditor,application.classLoader)\n" +
                "}}\n" +
                "def loadProperties() {\n" +
                "Properties properties = new Properties()\n" +
                "properties['foo'] = 'Foo'\n" +
                "properties\n" +
                "}}");
    }

    @Test
    @SuppressWarnings("unused")
    public void testDefaultGrailsPlugin() {
        GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);

        try {
            GrailsPlugin notVersionPlugin = new DefaultGrailsPlugin(notVersion, ga);
            fail("Should have thrown IllegalArgumentException for unversioned plugin");
        }
        catch (PluginException ignored) {
            // expected
        }

        try {
            GrailsPlugin notPlugin = new DefaultGrailsPlugin(notPluginClass, ga);
            fail("Should have thrown an exception for invalid plugin");
        }
        catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testGetPluginPath() {
        GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);
        assertEquals("/plugins/my-1.1", versionPlugin.getPluginPath());

        GrailsPlugin camelCasedPlugin = new DefaultGrailsPlugin(camelCased, ga);
        assertEquals("/plugins/camel-cased-2.1", camelCasedPlugin.getPluginPath());
    }

    @Test
    public void testDisabledPlugin() {
        GrailsPlugin disabledPlugin = new DefaultGrailsPlugin(disabled, ga);
        GrailsPlugin enabledPlugin = new DefaultGrailsPlugin(versioned, ga);

        assertFalse(disabledPlugin.isEnabled());
        assertTrue(enabledPlugin.isEnabled());
    }

    @Test
    public void testDoWithApplicationContext() {
        GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);

        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
        versionPlugin.doWithRuntimeConfiguration(springConfig);

        ApplicationContext ctx = springConfig.getApplicationContext();

        assertTrue(ctx.containsBean("classEditor"));

        versionPlugin.doWithApplicationContext(ctx);
    }

    @Test
    public void testDoWithRuntimeConfiguration() {
        // Version 1
        GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);

        RuntimeSpringConfiguration springConfig = new DefaultRuntimeSpringConfiguration();
        versionPlugin.doWithRuntimeConfiguration(springConfig);

        ApplicationContext ctx = springConfig.getApplicationContext();

        assertTrue(ctx.containsBean("classEditor"));

        // Version 2
        GrailsPlugin versionPlugin2 = new DefaultGrailsPlugin(versioned2, ga);

        RuntimeSpringConfiguration springConfig2 = new DefaultRuntimeSpringConfiguration();
        versionPlugin2.doWithRuntimeConfiguration(springConfig2);

        ApplicationContext ctx2 = springConfig2.getApplicationContext();

        assertTrue(ctx2.containsBean("classEditor"));

        // Version 3
        // GrailsPlugin versionPlugin3 = new DefaultGrailsPlugin(versioned3, ga);

        // RuntimeSpringConfiguration springConfig3 = new DefaultRuntimeSpringConfiguration();
        // versionPlugin3.doWithRuntimeConfiguration(springConfig3);

        // ApplicationContext ctx3 = springConfig3.getApplicationContext();

        // assertTrue(ctx3.containsBean("classEditor"));

        // Version 4
        // GrailsPlugin versionPlugin4 = new DefaultGrailsPlugin(versioned4, ga);

        // RuntimeSpringConfiguration springConfig4 = new DefaultRuntimeSpringConfiguration();
        // versionPlugin4.doWithRuntimeConfiguration(springConfig4);

        // ApplicationContext ctx4 = springConfig4.getApplicationContext();

        // assertTrue(ctx4.containsBean("classEditor"));
    }

    @Test
    public void testGetName() {
        GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);
        assertEquals("my", versionPlugin.getName());
    }

    @Test
    public void testGetVersion() {
        GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);
        assertEquals("1.1", versionPlugin.getVersion());
    }

    @Test
    public void testObservers() {
        GrailsPlugin observingPlugin = new DefaultGrailsPlugin(observed, ga);

        assertEquals(1, observingPlugin.getObservedPluginNames().length);
        assertEquals("another", observingPlugin.getObservedPluginNames()[0]);
    }

    @Test
    public void testWatchedResources() {
        GrailsPlugin versionPlugin = new DefaultGrailsPlugin(versioned, ga);
        assertEquals(versionPlugin.getWatchedResourcePatterns().get(0).getDirectory().getPath(), "./grails-app/taglib".replace("/", File.separator));
        assertEquals(versionPlugin.getWatchedResourcePatterns().get(1).getDirectory().getPath(), "./grails-app/controller".replace("/", File.separator));
        assertEquals(versionPlugin.getWatchedResourcePatterns().get(2).getDirectory().getPath(), "/absolutePath".replace("/", File.separator));
    }
}
