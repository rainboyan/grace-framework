/*
 * Copyright 2004-2022 the original author or authors.
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
package grails.plugins;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationStartupAware;
import org.springframework.core.metrics.ApplicationStartup;

import grails.core.GrailsApplication;
import grails.plugins.exceptions.PluginException;

import org.grails.spring.RuntimeSpringConfiguration;

/**
 * <p>Handles the loading and management of plug-ins in the Grails system.
 * A plugin a just like a normal Grails application except that it contains a file ending
 * in *Plugin.groovy  in the root of the directory.
 *
 * <p>A Plugin class is a Groovy class that has a version and optionally closures
 * called doWithSpring, doWithContext and doWithWebDescriptor
 *
 * <p>The doWithSpring closure uses the BeanBuilder syntax (@see grails.spring.BeanBuilder) to
 * provide runtime configuration of Grails via Spring
 *
 * <p>The doWithContext closure is called after the Spring ApplicationContext is built and accepts
 * a single argument (the ApplicationContext)
 *
 * <p>The doWithWebDescriptor uses mark-up building to provide additional functionality to the web.xml
 * file
 *
 *<p> Example:
 * <pre>
 * class ClassEditorGrailsPlugin {
 *      def version = 1.1
 *      def doWithSpring = { application -gt;
 *          classEditor(org.springframework.beans.propertyeditors.ClassEditor, application.classLoader)
 *      }
 * }
 * </pre>
 *
 * <p>A plugin can also define "dependsOn" and "evict" properties that specify what plugins the plugin
 * depends on and which ones it is incompatable with and should evict
 *
 * @author Graeme Rocher
 * @author Michael Yan
 * @since 0.4
 */
public interface GrailsPluginManager extends ApplicationContextAware, ApplicationStartupAware {

    String BEAN_NAME = "pluginManager";

    /**
     * Returns the list of all the plug-ins, included failed and unenabled
     * @return the list of plug-ins
     * @since 2022.0.0
     */
    List<GrailsPlugin> getPluginList();

    /**
     * Returns an array of all the loaded plug-ins
     * @return An array of plug-ins
     */
    GrailsPlugin[] getAllPlugins();

    /**
     * Gets plugin installed by the user and not provided by the framework
     * @return A list of user plugins
     */
    GrailsPlugin[] getUserPlugins();

    /**
     * @return An array of plugins that failed to load due to dependency resolution errors
     */
    GrailsPlugin[] getFailedLoadPlugins();

    /**
     * Performs the initial load of plug-ins throwing an exception if any dependencies
     * don't resolve
     *
     * @throws PluginException Thrown when an error occurs loading the plugins
     */
    void loadPlugins() throws PluginException;

    /**
     * Executes the runtime configuration phase of plug-ins
     *
     * @param springConfig The RuntimeSpringConfiguration instance
     */
    void doRuntimeConfiguration(RuntimeSpringConfiguration springConfig);

    /**
     * Performs post initialization configuration for each plug-in, passing
     * the built application context
     *
     * @param applicationContext The ApplicationContext instance
     */
    void doPostProcessing(ApplicationContext applicationContext);

    /**
     * Called on all plugins so that they can add new methods/properties/constructors etc.
     */
    void doDynamicMethods();

    /**
     * Executes the {@link Plugin#onStartup(Map)} hook for all plugins
     *
     * @param event the Event
     */
    void onStartup(Map<String, Object> event);

    /**
     * Retrieves a name Grails plugin instance
     *
     * @param name The name of the plugin
     * @return The GrailsPlugin instance or null if it doesn't exist
     */
    GrailsPlugin getGrailsPlugin(String name);

    /**
     * Obtains a GrailsPlugin for the given classname
     * @param name The name of the plugin
     * @return The instance
     */
    GrailsPlugin getGrailsPluginForClassName(String name);

    /**
     *
     * @param name The name of the plugin
     * @return true if the the manager has a loaded plugin with the given name
     */
    boolean hasGrailsPlugin(String name);

    /**
     * Retrieves a plug-in that failed to load, or null if it doesn't exist
     *
     * @param name The name of the plugin
     * @return A GrailsPlugin or null
     */
    GrailsPlugin getFailedPlugin(String name);

    /**
     * Retrieves a plug-in for its name and version
     *
     * @param name The name of the plugin
     * @param version The version of the plugin
     * @return The GrailsPlugin instance or null if it doesn't exist
     */
    GrailsPlugin getGrailsPlugin(String name, Object version);

    /**
     * Executes the runtime configuration for a specific plugin AND all its dependencies
     *
     * @param pluginName The name of he plugin
     * @param springConfig The runtime spring config instance
     */
    void doRuntimeConfiguration(String pluginName, RuntimeSpringConfiguration springConfig);

    /**
     * Checks all the plugins to see whether they have any changes
     */
    @Deprecated
    void checkForChanges();

    /**
     * Sets the GrailsApplication used be this plugin manager
     * @param application The GrailsApplication instance
     */
    void setApplication(GrailsApplication application);

    /**
     * Get the GrailsApplication used be this plugin manager
     */
    GrailsApplication getApplication();

    /**
     * Set the {@link ApplicationStartup} for this plugin manager.
     * <p>This allows the plugin manager to record metrics
     * during startup.
     * @param applicationStartup the new context event factory
     * @since 2022.0.0
     */
    void setApplicationStartup(ApplicationStartup applicationStartup);

    /**
     * Return the {@link ApplicationStartup} for this plugin manager.
     * @since 2022.0.0
     */
    ApplicationStartup getApplicationStartup();

    /**
     * @return the initialised
     */
    boolean isInitialised();

    /**
     * Refreshes the specified plugin. A refresh will force to plugin to "touch" each of its watched resources
     * and fire modified events for each
     *
     * @param name The name of the plugin to refresh
     */
    void refreshPlugin(String name);

    /**
     * Retrieves a collection of plugins that are observing the specified plugin
     *
     * @param plugin The plugin to retrieve observers for
     * @return A collection of observers
     */
    Collection<GrailsPlugin> getPluginObservers(GrailsPlugin plugin);

    /**
     * inform the specified plugins observers of the event specified by the passed Map instance
     *
     * @param pluginName The name of the plugin
     * @param event The event
     */
    void informObservers(String pluginName, Map<String, Object> event);

    /**
     * Called prior to the initialisation of the GrailsApplication object to allow registration of additional ArtefactHandler objects
     *
     * @see grails.core.ArtefactHandler
     */
    void doArtefactConfiguration();

    /**
     * Registers pre-compiled artefacts with the GrailsApplication instance, only overriding if the
     * application doesn't already provide an artefact of the same name.
     *
     * @param application The GrailsApplication object
     */
    void registerProvidedArtefacts(GrailsApplication application);

    /**
     * Registers provided {@link ModuleDescriptor} by {@link DynamicGrailsPlugin}.
     *
     * @since 2022.0.0
     */
    void registerProvidedModules();

    /**
     * Shuts down the PluginManager
     */
    void shutdown();

    /**
     * Set whether the core plugins should be loaded
     * @param shouldLoadCorePlugins True if they should
     */
    void setLoadCorePlugins(boolean shouldLoadCorePlugins);

    /**
     * Method for handling changes to a class and triggering on change events etc.
     * @param aClass The class
     */
    void informOfClassChange(Class<?> aClass);

    /**
     * Returns the pluginContextPath for the given plugin
     *
     * @param name The plugin name
     * @return the context path
     */
    String getPluginPath(String name);

    /**
     * Returns the pluginContextPath for the given plugin and will force name to camel case instead of '-' lower case
     *
     * my-plug-web would resolve to myPlugWeb if forceCamelCase is true.
     *
     * @param name The plugin name
     * @param forceCamelCase Force camel case for name
     * @return the context path
     */
    String getPluginPath(String name, boolean forceCamelCase);

    /**
     * Looks up the plugin that defined the given instance. If no plugin
     * defined the instance then null is returned.
     *
     * @param instance The instance
     * @return The plugin that defined the instance or null
     */
    GrailsPlugin getPluginForInstance(Object instance);

    /**
     * Returns the pluginContextPath for the given instance
     * @param instance The instance
     * @return The pluginContextPath
     */
    String getPluginPathForInstance(Object instance);

    /**
     * Returns the plugin path for the given class
     * @param theClass The class
     * @return The pluginContextPath
     */
    String getPluginPathForClass(Class<?> theClass);

    /**
     * Returns the plugin views directory for the given instance
     * @param instance The instance
     * @return The pluginContextPath
     */
    String getPluginViewsPathForInstance(Object instance);

    /**
     * Returns the plugin views directory path for the given class
     * @param theClass The class
     * @return The pluginContextPath
     */
    String getPluginViewsPathForClass(Class<?> theClass);

    /**
     * Obtains the GrailsPlugin for the given class
     *
     * @param theClass The class
     * @return The GrailsPlugin for the given class or null if not related to any plugin
     */
    GrailsPlugin getPluginForClass(Class<?> theClass);

    /**
     * Inform of a change in configuration
     */
    void informPluginsOfConfigChange();

    /**
     * Fire to inform the PluginManager that a particular file changes
     *
     * @param file The file that changed
     * @since 2.0
     */
    void informOfFileChange(File file);

    void informOfClassChange(File file, Class<?> cls);

    /**
     * Indicates whether the manager has been shutdown or not
     * @return True if it was shutdown
     */
    boolean isShutdown();

    /**
     * Sets the filter to use to filter for plugins
     *
     * @param pluginFilter The plugin filter
     */
    void setPluginFilter(PluginFilter pluginFilter);

    /**
     * Add User Plugin from Class
     * @param pluginClass the class of Plugin
     * @since 2022.0.0
     */
    void addUserPlugin(Class<?> pluginClass);

    /**
     * Gets all module descriptors of installed modules.
     * @since 2022.0.0
     */
    Collection<ModuleDescriptor<?>> getModuleDescriptors();

    /**
     * Gets all module descriptors of installed modules that match the given predicate.
     *
     * @param moduleDescriptorPredicate describes which modules to match
     * @return a collection of {@link ModuleDescriptor}s that match the given predicate.
     * @since 2022.0.0
     */
    <M> Collection<ModuleDescriptor<M>> getModuleDescriptors(Predicate<ModuleDescriptor<M>> moduleDescriptorPredicate);

    /**
     * Get all enabled module descriptors that have a specific descriptor class.
     *
     * @param descriptorClazz module descriptor class
     * @return List of {@link ModuleDescriptor}s that implement or extend the given class.
     * @since 2022.0.0
     */
    <D extends ModuleDescriptor<?>> List<D> getEnabledModuleDescriptorsByClass(Class<D> descriptorClazz);

}
