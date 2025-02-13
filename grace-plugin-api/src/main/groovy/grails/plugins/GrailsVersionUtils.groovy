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
package grails.plugins

import groovy.transform.CompileStatic

import grails.plugins.metadata.GrailsPlugin

/**
 * @author Graeme Rocher
 */
@CompileStatic
class GrailsVersionUtils {

    /**
     * Get the name of the a plugin for a particular class.
     */
    @CompileStatic
    static String getPluginName(Class clazz) {
        GrailsPlugin ann = clazz?.getAnnotation(GrailsPlugin)
        ann?.name()
    }

    /**
     * Get the version of the a plugin for a particular class.
     */
    @CompileStatic
    static String getPluginVersion(Class clazz) {
        GrailsPlugin ann = clazz?.getAnnotation(GrailsPlugin)
        ann?.version()
    }

    /**
     * Check if the required version is a valid for the given plugin version.
     *
     * @param pluginVersion The plugin version
     * @param requiredVersion The required version
     * @return true if it is valid
     */
    static boolean isValidVersion(String pluginVersion, String requiredVersion) {
        VersionComparator vc = new VersionComparator()
        pluginVersion = trimTag(pluginVersion)

        if (requiredVersion.indexOf('>') > -1) {
            List<String> tokens = requiredVersion.split('>')*.trim()
            tokens = tokens.stream().collect({ String it -> trimTag(it) })
            tokens << pluginVersion
            tokens.sort(true, vc)

            if (tokens[1] == pluginVersion) {
                return true
            }
        }
        else if (pluginVersion == trimTag(requiredVersion)) {
            return true
        }

        false
    }

    /**
     * Returns true if rightVersion is greater than leftVersion
     * @param leftVersion
     * @param rightVersion
     * @return
     */
    static boolean isVersionGreaterThan(String leftVersion, String rightVersion) {
        if (leftVersion == rightVersion) {
            return false
        }
        List<String> versions = [leftVersion, rightVersion]
        versions.sort(true, new VersionComparator())
        versions[1] == rightVersion
    }

    /**
     * Returns the upper version of a Grails version number expression in a plugin
     */
    static String getUpperVersion(String pluginVersion) {
        getPluginVersionInternal(pluginVersion, 1)
    }

    /**
     * Returns the lower version of a Grails version number expression in a plugin
     */
    static String getLowerVersion(String pluginVersion) {
        getPluginVersionInternal(pluginVersion, 0)
    }

    static boolean supportsAtLeastVersion(String pluginVersion, String requiredVersion) {
        String lowerVersion = getLowerVersion(pluginVersion)
        lowerVersion != '*' && isValidVersion(lowerVersion, "$requiredVersion > *")
    }

    private static getPluginVersionInternal(String pluginVersion, Integer index) {
        if (pluginVersion.indexOf('>') > -1) {
            List<String> tokens = pluginVersion.split('>')*.trim()
            return tokens[index].trim()
        }

        pluginVersion.trim()
    }

    private static String trimTag(String pluginVersion) {
        int i = pluginVersion.indexOf('-')
        if (i > -1) {
            pluginVersion = pluginVersion[0..i - 1]
        }
        String[] tokens = pluginVersion.split(/\./)

        tokens.findAll { String it -> it ==~ /\d+/ || it == '*' }.join('.')
    }

}
