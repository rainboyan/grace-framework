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
package grails.util;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import grails.config.Config;
import grails.core.GrailsApplication;
import grails.plugins.GrailsPluginManager;

import org.grails.core.io.support.GrailsFactoriesLoader;
import org.grails.core.support.GrailsApplicationDiscoveryStrategy;

/**
 * Allows looking up key classes in a static context
 *
 * @author Burt Beckwith
 * @author Graeme Rocher
 *
 * @since 2.0
 */
@SuppressWarnings("unchecked")
public final class Holders {

    private static final Log logger = LogFactory.getLog(Holders.class);

    private static final Holder<GrailsPluginManager> PLUGIN_MANAGERS = new Holder<>("PluginManager");

    private static final Holder<Boolean> PLUGIN_MANAGERS_IN_CREATION = new Holder<>("PluginManagers in creation");

    private static final Holder<Config> CONFIGS = new Holder<>("config");

    private static final Holder<Map<?, ?>> FLAT_CONFIGS = new Holder<>("flat config");

    private static final List<GrailsApplicationDiscoveryStrategy> APPLICATION_DISCOVERY_STRATEGIES = GrailsFactoriesLoader.loadFactories(
            GrailsApplicationDiscoveryStrategy.class, Holders.class.getClassLoader());

    private static Holder servletContexts;

    static {
        createServletContextsHolder();
    }

    private static GrailsApplication applicationSingleton; // TODO remove

    private Holders() {
        // static only
    }

    public static void addApplicationDiscoveryStrategy(GrailsApplicationDiscoveryStrategy strategy) {
        APPLICATION_DISCOVERY_STRATEGIES.add(strategy);
    }

    public static void clear() {
        PLUGIN_MANAGERS.set(null);
        PLUGIN_MANAGERS_IN_CREATION.set(null);
        CONFIGS.set(null);
        FLAT_CONFIGS.set(null);
        if (servletContexts != null) {
            servletContexts.set(null);
        }
        APPLICATION_DISCOVERY_STRATEGIES.clear();
        applicationSingleton = null;
    }

    public static void setServletContext(final Object servletContext) {
        if (servletContexts != null) {
            servletContexts.set(servletContext);
        }
    }

    public static Object getServletContext() {
        return get(servletContexts, "servletContext");
    }

    public static ApplicationContext getApplicationContext() {
        for (GrailsApplicationDiscoveryStrategy strategy : APPLICATION_DISCOVERY_STRATEGIES) {
            ApplicationContext applicationContext = strategy.findApplicationContext();
            if (applicationContext != null) {
                boolean running = ((Lifecycle) applicationContext).isRunning();
                if (running) {
                    return applicationContext;
                }
            }
        }
        throw new IllegalStateException("Could not find ApplicationContext, configure Grails correctly first");
    }

    /**
     *
     * @return The ApplicationContext or null if it doesn't exist
     */
    public static ApplicationContext findApplicationContext() {
        for (GrailsApplicationDiscoveryStrategy strategy : APPLICATION_DISCOVERY_STRATEGIES) {
            ApplicationContext applicationContext = strategy.findApplicationContext();
            if (applicationContext != null) {
                return applicationContext;
            }
        }
        return null;
    }

    /**
     *
     * @return The ApplicationContext or null if it doesn't exist
     */
    public static GrailsApplication findApplication() {
        for (GrailsApplicationDiscoveryStrategy strategy : APPLICATION_DISCOVERY_STRATEGIES) {
            GrailsApplication grailsApplication = strategy.findGrailsApplication();
            if (grailsApplication != null) {
                return grailsApplication;
            }
        }
        return applicationSingleton;
    }

    public static GrailsApplication getGrailsApplication() {
        GrailsApplication grailsApplication = findApplication();
        Assert.notNull(grailsApplication, "GrailsApplication not found");
        return grailsApplication;
    }

    public static void setGrailsApplication(GrailsApplication application) {
        applicationSingleton = application;
    }

    public static void setConfig(Config config) {
        CONFIGS.set(config);

        // reset flat config
        FLAT_CONFIGS.set(config == null ? null : config);
    }

    public static Config getConfig() {
        return get(CONFIGS, "config");
    }

    public static Map<?, ?> getFlatConfig() {
        Map<?, ?> flatConfig = get(FLAT_CONFIGS, "flatConfig");
        return flatConfig == null ? Collections.emptyMap() : flatConfig;
    }

    public static void setPluginManagerInCreation(boolean inCreation) {
        PLUGIN_MANAGERS_IN_CREATION.set(inCreation);
    }

    public static void setPluginManager(GrailsPluginManager pluginManager) {
        if (pluginManager != null) {
            PLUGIN_MANAGERS_IN_CREATION.set(false);
        }
        PLUGIN_MANAGERS.set(pluginManager);
    }

    public static GrailsPluginManager getPluginManager() {
        return getPluginManager(false);
    }

    public static GrailsPluginManager getPluginManager(boolean mappedOnly) {
        while (true) {
            Boolean inCreation = get(PLUGIN_MANAGERS_IN_CREATION, "PluginManager in creation", mappedOnly);
            if (inCreation == null) {
                inCreation = false;
            }
            if (!inCreation) {
                break;
            }

            try {
                Thread.sleep(100);
            }
            catch (InterruptedException ignored) {
                break;
            }
        }

        return get(PLUGIN_MANAGERS, "PluginManager", mappedOnly);
    }

    public static GrailsPluginManager currentPluginManager() {
        GrailsPluginManager current = getPluginManager();
        Assert.notNull(current, "No PluginManager set");
        return current;
    }

    public static void reset() {
        setPluginManager(null);
        setGrailsApplication(null);
        setServletContext(null);
        setPluginManager(null);
        setPluginManagerInCreation(false);
        setConfig(null);
    }

    private static <T> T get(Holder<T> holder, String type) {
        return get(holder, type, false);
    }

    private static <T> T get(Holder<T> holder, String type, boolean mappedOnly) {
        return holder.get(mappedOnly);
    }

    private static void createServletContextsHolder() {
        try {
            Class<?> clazz = Holders.class.getClassLoader().loadClass("grails.web.context.WebRequestServletHolder");
            servletContexts = (Holder) ReflectionUtils.accessibleConstructor(clazz).newInstance();
        }
        catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            // shouldn't happen
            logger.debug("Error initializing servlet context holder, not running in Servlet environment: " + e.getMessage(), e);
        }
    }

}
