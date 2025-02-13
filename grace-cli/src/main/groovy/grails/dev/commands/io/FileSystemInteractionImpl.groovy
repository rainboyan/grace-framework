/*
 * Copyright 2014-2022 the original author or authors.
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
package grails.dev.commands.io

import groovy.transform.CompileStatic

import grails.util.BuildSettings

import org.grails.io.support.DefaultResourceLoader
import org.grails.io.support.FileSystemResource
import org.grails.io.support.GrailsResourceUtils
import org.grails.io.support.PathMatchingResourcePatternResolver
import org.grails.io.support.Resource
import org.grails.io.support.ResourceLoader
import org.grails.io.support.ResourceLocator
import org.grails.io.support.SpringIOUtils

/**
 * Utility methods exposed to scripts for interacting with resources (found on the file system or jars) and the file system
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
class FileSystemInteractionImpl implements FileSystemInteraction {

    File baseDir

    ResourceLoader resourceLoader

    PathMatchingResourcePatternResolver resourcePatternResolver

    ResourceLocator resourceLocator

    FileSystemInteractionImpl(File baseDir, ResourceLoader resourceLoader = new DefaultResourceLoader()) {
        this.baseDir = baseDir
        this.resourceLoader = resourceLoader
        this.resourceLocator = new ResourceLocator()
        this.resourceLocator.setSearchLocation(baseDir.absolutePath)
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver(resourceLoader)
    }

    /**
     * Makes a directory
     *
     * @param path The path to the directory
     */
    @Override
    FileSystemInteractionImpl mkdir(path) {
        file(path)?.mkdirs()
        this
    }

    /**
     * Deletes a file
     *
     * @param path The path to the file
     */
    @Override
    FileSystemInteractionImpl delete(path) {
        file(path)?.delete()
        this
    }

    /**
     * Allows Gradle style simple copy specs
     *
     * @param callable The callable
     * @return this
     */
    @Override
    FileSystemInteractionImpl copy(@DelegatesTo(FileSystemInteraction.CopySpec) Closure callable) {
        FileSystemInteraction.CopySpec spec = new FileSystemInteraction.CopySpec()
        callable.delegate = spec
        callable.call()
        if (spec.from && spec.into) {
            if (spec.from instanceof Iterable) {
                copyAll((Iterable) spec.from, spec.into)
            }
            else {
                copy(spec.from, spec.into)
            }
        }
        this
    }

    /**
     * Copies a resource to the target destination
     *
     * @param path The path
     * @param destination The destination
     */
    @Override
    FileSystemInteractionImpl copy(path, destination) {
        def from = resource(path)
        def to = file(destination)
        copy(from, to)
        this
    }

    /**
     * Copies resources to the target destination
     *
     * @param path The path
     * @param destination The destination
     */
    @Override
    FileSystemInteractionImpl copyAll(Iterable resources, destination) {
        mkdir(destination)
        for (path in resources) {
            def from = resource(path)
            def to = file(destination)
            copy(from, to)
        }
        this
    }

    /**
     * Copy a Resource from the given location to the given directory or location
     *
     * @param from The resource to copy
     * @param to The location to copy to
     * @return The {@FileSystemInteraction} instance
     */
    @Override
    FileSystemInteractionImpl copy(Resource from, File to) {
        if (!to?.exists()) {
            mkdir(to)
        }
        if (from && to) {
            if (to.isDirectory()) {
                mkdir(to)
                to = new File(to, from.filename)
            }
            SpringIOUtils.copy(from, to)
            println("Copied ${from.filename} to location ${to.canonicalPath}")
        }
        this
    }

    /**
     * Obtain a file for the given path
     *
     * @param path The path
     * @return The file
     */
    @Override
    File file(Object path) {
        if (path instanceof File) {
            return (File) path
        }
        else if (path instanceof Resource) {
            return ((Resource) path).file
        }
        new File(baseDir ?: new File('.'), path.toString())
    }

    /**
     * @return The target build directory
     */
    @Override
    File getBuildDir() {
        BuildSettings.TARGET_DIR
    }

    /**
     * @return The directory where resources are processed to
     */
    @Override
    File getResourcesDir() {
        BuildSettings.RESOURCES_DIR
    }

    /**
     * @return The directory where classes are compiled to
     */
    @Override
    File getClassesDir() {
        BuildSettings.CLASSES_DIR
    }

    /**
     * Finds a source file for the given class name
     * @param className The class name
     * @return The source resource
     */
    @Override
    Resource source(String className) {
        resourceLocator.findResourceForClassName(className)
    }

    /**
     * The class name of the given resource
     *
     * @param resource The resource
     * @return The class name
     */
    String className(Resource resource) {
        GrailsResourceUtils.getClassName(resource)
    }

    /**
     * Obtain a resource for the given path
     * @param path The path
     * @return The resource
     */
    @Override
    Resource resource(Object path) {
        if (!path) {
            return null
        }
        if (path instanceof Resource) {
            return (Resource) path
        }
        def f = file(path)
        if (f?.exists() && f.isFile()) {
            return new FileSystemResource(f)
        }
        def pathStr = path.toString()
        def resource = resourceLoader.getResource(pathStr)
        if (resource.exists()) {
            return resource
        }

        def allResources = resources(pathStr)
        if (allResources) {
            return allResources[0]
        }
        resource
    }

    /**
     * Obtain resources for the given pattern
     *
     * @param pattern The pattern
     * @return The resources
     */
    @Override
    Collection<Resource> resources(String pattern) {
        try {
            return resourcePatternResolver.getResources(pattern).toList()
        }
        catch (ignored) {
            return []
        }
    }

    /**
     * Obtain the path of the resource relative to the current project
     *
     * @param path The path to inspect
     * @return The relative path
     */
    @Override
    String projectPath(Object path) {
        def file = file(path)
        if (file) {
            def basePath = baseDir.canonicalPath
            return (file.canonicalPath - basePath).substring(1)
        }
        ''
    }

    /**
     * Get files matching the given pattern
     *
     * @param pattern The pattern
     * @return the files
     */
    @Override
    Collection<File> files(String pattern) {
        resources(pattern)*.file
    }

}
