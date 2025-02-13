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
package org.grails.io.support;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import grails.util.Environment;

/**
 * Used to locate resources at build / development time
 *
 * @author Graeme Rocher
 * @since 3.0
 */
public class ResourceLocator {

    public static final String WILDCARD = "*";

    public static final String FILE_SEPARATOR = File.separator;

    public static final String CLOSURE_MARKER = "$";

    public static final String WEB_APP_DIR = "web-app";

    protected static final Resource NULL_RESOURCE = new ByteArrayResource("null".getBytes());

    protected PathMatchingResourcePatternResolver patchMatchingResolver;

    protected List<String> classSearchDirectories = new ArrayList<>();

    protected List<String> resourceSearchDirectories = new ArrayList<>();

    protected Map<String, Resource> classNameToResourceCache = new ConcurrentHashMap<>();

    protected Map<String, Resource> uriToResourceCache = new ConcurrentHashMap<>();

    protected ResourceLoader defaultResourceLoader = new FileSystemResourceLoader();

    protected boolean warDeployed = Environment.isWarDeployed();

    public void setSearchLocation(String searchLocation) {
        ResourceLoader resourceLoader = getDefaultResourceLoader();
        this.patchMatchingResolver = new PathMatchingResourcePatternResolver(resourceLoader);
        initializeForSearchLocation(searchLocation);
    }

    protected ResourceLoader getDefaultResourceLoader() {
        return this.defaultResourceLoader;
    }

    public void setSearchLocations(Collection<String> searchLocations) {
        this.patchMatchingResolver = new PathMatchingResourcePatternResolver(getDefaultResourceLoader());
        for (String searchLocation : searchLocations) {
            initializeForSearchLocation(searchLocation);
        }
    }

    private void initializeForSearchLocation(String searchLocation) {
        String searchLocationPlusSlash = searchLocation.endsWith("/") ? searchLocation : searchLocation + FILE_SEPARATOR;
        try {
            File[] directories = new File(searchLocationPlusSlash + GrailsResourceUtils.GRAILS_APP_DIR)
                    .listFiles(file -> file.isDirectory() && !file.isHidden());

            if (directories != null) {
                for (File directory : directories) {
                    this.classSearchDirectories.add(directory.getCanonicalPath());
                }
            }
            File[] appDirectories = new File(searchLocationPlusSlash + "app")
                    .listFiles(file -> file.isDirectory() && !file.isHidden());

            if (appDirectories != null) {
                for (File directory : appDirectories) {
                    this.classSearchDirectories.add(directory.getCanonicalPath());
                }
            }
        }
        catch (IOException ignored) {
        }

        this.classSearchDirectories.add(searchLocationPlusSlash + "src/main/java");
        this.classSearchDirectories.add(searchLocationPlusSlash + "src/main/groovy");
        this.resourceSearchDirectories.add(searchLocationPlusSlash);
    }

    public Resource findResourceForURI(String uri) {
        Resource resource = this.uriToResourceCache.get(uri);
        if (resource == null) {
            PluginResourceInfo info = inferPluginNameFromURI(uri);
            if (this.warDeployed) {
                Resource defaultResource = this.defaultResourceLoader.getResource(uri);
                if (defaultResource != null && defaultResource.exists()) {
                    resource = defaultResource;
                }
            }
            else {
                String uriWebAppRelative = WEB_APP_DIR + uri;

                for (String resourceSearchDirectory : this.resourceSearchDirectories) {
                    Resource res = resolveExceptionSafe(resourceSearchDirectory + uriWebAppRelative);
                    if (res != null && res.exists()) {
                        resource = res;
                    }
                    else if (!this.warDeployed) {
                        Resource dir = resolveExceptionSafe(resourceSearchDirectory);
                        if (dir != null && dir.exists() && info != null) {
                            String filename = dir.getFilename();
                            if (filename != null && filename.equals(info.pluginName)) {
                                Resource pluginFile = dir.createRelative(WEB_APP_DIR + info.uri);
                                if (pluginFile.exists()) {
                                    resource = pluginFile;
                                }
                            }
                        }
                    }
                }
            }

            if (resource == null || !resource.exists()) {
                Resource tmp = this.defaultResourceLoader != null ? this.defaultResourceLoader.getResource(uri) : null;
                if (tmp != null && tmp.exists()) {
                    resource = tmp;
                }
            }

            if (resource != null) {
                this.uriToResourceCache.put(uri, resource);
            }
            else if (this.warDeployed) {
                this.uriToResourceCache.put(uri, NULL_RESOURCE);
            }
        }
        return resource == NULL_RESOURCE ? null : resource;
    }

    private PluginResourceInfo inferPluginNameFromURI(String uri) {
        if (uri.startsWith("/plugins/")) {
            String withoutPluginsPath = uri.substring("/plugins/".length(), uri.length());
            int i = withoutPluginsPath.indexOf('/');
            if (i > -1) {
                PluginResourceInfo info = new PluginResourceInfo();
                info.pluginName = withoutPluginsPath.substring(0, i);
                info.uri = withoutPluginsPath.substring(i);
                return info;
            }
        }
        return null;
    }

    public Resource findResourceForClassName(String className) {
        if (className.contains(CLOSURE_MARKER)) {
            className = className.substring(0, className.indexOf(CLOSURE_MARKER));
        }

        Resource resource = this.classNameToResourceCache.get(className);
        if (resource == null) {
            String classNameWithPathSeparator = className.replace(".", FILE_SEPARATOR);
            for (String pathPattern : getSearchPatternForExtension(classNameWithPathSeparator, ".groovy", ".java")) {
                resource = resolveExceptionSafe(pathPattern);
                if (resource != null && resource.exists()) {
                    this.classNameToResourceCache.put(className, resource);
                    break;
                }
            }
            if (resource == null || !resource.exists()) {
                for (String ext : new String[] {".groovy", ".java"}) {
                    resource = resolveExceptionSafe(GrailsResourceUtils.DOMAIN_DIR_PATH + "**/" + className + ext);
                    if (resource != null && resource.exists()) {
                        this.classNameToResourceCache.put(className, resource);
                        break;
                    }
                    resource = resolveExceptionSafe("app/domain/**/" + className + ext);
                    if (resource != null && resource.exists()) {
                        this.classNameToResourceCache.put(className, resource);
                        break;
                    }
                }
            }
        }

        return resource != null && resource.exists() ? resource : null;
    }

    private List<String> getSearchPatternForExtension(String classNameWithPathSeparator, String... extensions) {
        List<String> searchPatterns = new ArrayList<>();
        for (String extension : extensions) {
            String filename = classNameWithPathSeparator + extension;
            for (String classSearchDirectory : this.classSearchDirectories) {
                searchPatterns.add(classSearchDirectory + FILE_SEPARATOR + filename);
            }
        }

        return searchPatterns;
    }

    private Resource resolveExceptionSafe(String pathPattern) {
        try {
            Resource[] resources = this.patchMatchingResolver.getResources("file:" + pathPattern);
            if (resources != null && resources.length > 0) {
                return resources[0];
            }
        }
        catch (IOException ignored) {
        }

        return null;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.defaultResourceLoader = resourceLoader;
    }

    class PluginResourceInfo {

        String pluginName;

        String uri;

    }

}
