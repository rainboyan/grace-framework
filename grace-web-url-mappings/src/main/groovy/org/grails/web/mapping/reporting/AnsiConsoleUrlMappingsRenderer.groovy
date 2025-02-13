/*
 * Copyright 2013-2023 the original author or authors.
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
package org.grails.web.mapping.reporting

import groovy.transform.CompileStatic
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.Color

import grails.build.logging.GrailsConsole
import grails.gorm.validation.Constrained
import grails.gorm.validation.ConstrainedProperty
import grails.web.mapping.UrlMapping
import grails.web.mapping.reporting.UrlMappingsRenderer

import org.grails.web.mapping.ResponseCodeMappingData
import org.grails.web.mapping.ResponseCodeUrlMapping

/**
 * Renders URL mappings to the console
 *
 * @author Graeme Rocher
 * @since 2.3
 */
@CompileStatic
class AnsiConsoleUrlMappingsRenderer implements UrlMappingsRenderer {

    public static final String DEFAULT_ACTION = '(default action)'
    PrintStream targetStream = System.out
    boolean isAnsiEnabled = GrailsConsole.getInstance().isAnsiEnabled()

    AnsiConsoleUrlMappingsRenderer(PrintStream targetStream) {
        this.targetStream = targetStream
    }

    AnsiConsoleUrlMappingsRenderer() {
    }

    @Override
    void render(List<UrlMapping> urlMappings) {
        Map<Object, List<UrlMapping>> mappingsByController = urlMappings.groupBy { UrlMapping mapping -> mapping.controllerName }
        int longestMapping = establishUrlPattern(urlMappings.max {
            UrlMapping mapping -> establishUrlPattern(mapping, false).length()
        }, false).length() + 5
        UrlMapping longestActionName = urlMappings.max { UrlMapping mapping ->
            (mapping?.actionName) ? mapping?.actionName?.toString()?.length() : 0
        }
        int longestAction = (longestActionName.actionName ? longestActionName.actionName?.toString()?.length() : 0) + 10
        longestAction = longestAction < DEFAULT_ACTION.length() ? DEFAULT_ACTION.length() : longestAction
        List<Object> controllerNames = mappingsByController.keySet().sort()

        for (controller in controllerNames) {
            if (controller == null) {
                targetStream.println(header('Dynamic Mappings'))
            }
            else {
                targetStream.println(header('Controller', controller.toString()))
            }
            List<UrlMapping> controllerUrlMappings = mappingsByController.get(controller)
            for (UrlMapping urlMapping in controllerUrlMappings) {
                String urlPattern = establishUrlPattern(urlMapping, isAnsiEnabled, longestMapping)

                String actionName = urlMapping.actionName?.toString() ?: DEFAULT_ACTION
                if (actionName && !urlMapping.viewName) {
                    targetStream.println("${yellowBar()}${urlMapping.httpMethod.center(8)}${yellowBar()}${urlPattern}" +
                            "${yellowBar()}${bold('Action: ')}${actionName.padRight(longestAction)}${endBar()}")
                }
                else if (urlMapping.viewName) {
                    targetStream.println("${yellowBar()}${urlMapping.httpMethod.center(8)}${yellowBar()}${urlPattern}" +
                            "${yellowBar()}${bold('View:   ')}${urlMapping.viewName.toString().padRight(longestAction)}${endBar()}")
                }
            }
            targetStream.println()
        }
    }

    String bold(String text) {
        if (isAnsiEnabled) {
            return Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(text).a(Ansi.Attribute.INTENSITY_BOLD_OFF)
        }
        text
    }

    protected String establishUrlPattern(UrlMapping urlMapping, boolean withAnsi = isAnsiEnabled, int padding = -1) {
        if (urlMapping instanceof ResponseCodeUrlMapping) {
            String errorCode = 'ERROR: ' + ((ResponseCodeMappingData) urlMapping.urlData).responseCode
            if (withAnsi) {
                return padAnsi(error(errorCode), errorCode, padding)
            }
            return errorCode.padRight(padding)
        }
        Constrained[] constraints = urlMapping.constraints
        String[] tokens = urlMapping.urlData.tokens
        StringBuilder urlPattern = new StringBuilder(UrlMapping.SLASH)
        int constraintIndex = 0
        tokens.eachWithIndex { String token, int i ->
            boolean hasTokens = token.contains(UrlMapping.CAPTURED_WILDCARD) || token.contains(UrlMapping.CAPTURED_DOUBLE_WILDCARD)
            if (hasTokens) {
                String finalToken = token
                while (hasTokens) {
                    if (finalToken.contains(UrlMapping.CAPTURED_WILDCARD)) {
                        ConstrainedProperty constraint = (ConstrainedProperty) constraints[constraintIndex++]
                        String prop = '\\${' + constraint.propertyName + '}'
                        finalToken = finalToken.replaceFirst(/\(\*\)/, withAnsi ? variable(prop) : prop)
                    }
                    else if (finalToken.contains(UrlMapping.CAPTURED_DOUBLE_WILDCARD)) {
                        ConstrainedProperty constraint = (ConstrainedProperty) constraints[constraintIndex++]
                        String prop = '\\\${' + constraint.propertyName + '}**'
                        finalToken = finalToken.replaceFirst(/\(\*\*\)/, prop)
                    }
                    hasTokens = finalToken.contains(UrlMapping.CAPTURED_WILDCARD) || finalToken.contains(UrlMapping.CAPTURED_DOUBLE_WILDCARD)
                }
                urlPattern << finalToken
            }
            else {
                urlPattern << token
            }

            if (i < (tokens.length - 1)) {
                urlPattern << UrlMapping.SLASH
            }
        }
        if (urlMapping.urlData.hasOptionalExtension()) {
            Constrained[] allConstraints = urlMapping.constraints
            ConstrainedProperty lastConstraint = (ConstrainedProperty) allConstraints[-1]
            urlPattern << "(.\${${lastConstraint.propertyName})?"
        }
        if (padding) {
            if (withAnsi) {
                String nonAnsiPattern = establishUrlPattern(urlMapping, false)
                return padAnsi(urlPattern.toString(), nonAnsiPattern, padding)
            }
            return urlPattern.toString().padRight(padding)
        }

        urlPattern.toString()
    }

    protected String padAnsi(String ansiString, String nonAnsiString, int padding) {
        int toPad = padding - nonAnsiString.length()
        if (toPad > 0) {
            String padText = getPadding(' ', toPad)
            return "${ansiString}$padText".toString()
        }
        ansiString.toString()
    }

    static String getPadding(String padding, int length) {
        if (padding.length() < length) {
            return padding.multiply(length / padding.length() + 1).substring(0, length)
        }
        padding.substring(0, length)
    }

    String error(String errorCode) {
        Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(Color.RED).a(errorCode).a(Ansi.Attribute.INTENSITY_BOLD_OFF).fg(Color.DEFAULT)
    }

    String variable(String name, boolean withAnsi = isAnsiEnabled) {
        Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(Color.CYAN).a(name).a(Ansi.Attribute.INTENSITY_BOLD_OFF).fg(Color.DEFAULT).reset()
    }

    String header(String text) {
        if (isAnsiEnabled) {
            Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(Color.GREEN).a(text).a(Ansi.Attribute.INTENSITY_BOLD_OFF).fg(Color.DEFAULT)
        }
        text
    }

    String header(String text, String description) {
        if (isAnsiEnabled) {
            Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(Color.GREEN).a("$text: ".toString())
                    .fg(Color.DEFAULT).a(description).a(Ansi.Attribute.INTENSITY_BOLD_OFF)
        }

        "$text: $description"
    }

    String yellowBar() {
        if (isAnsiEnabled) {
            return Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(Color.YELLOW).a(' | ').a(Ansi.Attribute.INTENSITY_BOLD_OFF).fg(Color.DEFAULT)
        }
        ' | '
    }

    String endBar() {
        if (isAnsiEnabled) {
            return Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).fg(Color.YELLOW).a(' |').a(Ansi.Attribute.INTENSITY_BOLD_OFF).fg(Color.DEFAULT)
        }
        ' |'
    }

}
