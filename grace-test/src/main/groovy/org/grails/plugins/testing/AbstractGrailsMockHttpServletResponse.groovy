/*
 * Copyright 2008-2023 the original author or authors.
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
package org.grails.plugins.testing

import java.lang.reflect.Field

import jakarta.servlet.http.HttpServletRequest

import groovy.xml.slurpersupport.GPathResult
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.util.ReflectionUtils

import grails.converters.JSON

import org.grails.io.support.SpringIOUtils
import org.grails.web.json.JSONElement
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes

/**
 * Simple sub-class of Spring's MockHttpServletResponse that adds the
 * left-shift operator, "<<".
 */
abstract class AbstractGrailsMockHttpServletResponse extends MockHttpServletResponse {

    /**
     * Sets the response format
     *
     * @param format The format of the response
     */
    void setFormat(String format) {
        HttpServletRequest request = GrailsWebRequest.lookup().getCurrentRequest()

        request.setAttribute(GrailsApplicationAttributes.RESPONSE_FORMAT, format)
        // remove so that is can be repopulated
        request.setAttribute(GrailsApplicationAttributes.RESPONSE_MIME_TYPE, null)
    }

    /**
     * Appends the given content string to the response's output stream.
     */
    void leftShift(String content) {
        writer << content
    }

    /**
     * Return the primary value for the given header as a String, if any.
     * Will return the first value in case of multiple values.
     *
     * @param name the name of the header
     * @return the associated header value, or <code>null<code> if none
     */
    String header(String name) {
        super.getHeader(name)
    }

    /**
     * Return all values for the given header as a List of Strings.
     * @param name the name of the header
     * @return the associated header values, or an empty List if none
     */
    List<String> headers(String name) {
        super.getHeaders(name)
    }

    /**
     * Get the response XML
     *
     * @return The response XML
     */
    GPathResult getXml() {
        SpringIOUtils.createXmlSlurper().parseText(contentAsString)
    }

    /**
     * Get the response JSON
     *
     * @return  The JSON response
     */
    JSONElement getJson() {
        JSON.parse(contentAsString)
    }

    /**
     * The response body as text
     *
     * @return The text within the response body
     */
    String getText() {
        contentAsString
    }

    @Override
    void reset() {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup()
        webRequest?.currentRequest?.removeAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)
        setCommitted(false)
        Field field = ReflectionUtils.findField(MockHttpServletResponse, 'writer')
        ReflectionUtils.makeAccessible(field)
        field.set(this, null)
        webRequest.setOut(getWriter())
        super.reset()
    }

    String getRedirectUrl() {
        getRedirectedUrl()
    }

    @Override
    String getRedirectedUrl() {
        GrailsWebRequest webRequest = GrailsWebRequest.lookup()
        Object redirectURI = webRequest?.currentRequest?.getAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)

        if (redirectURI != null) {
            return redirectURI
        }

        if (getStatus() in [301, 302]) {
            return super.getHeader('Location')
        }

        super.getRedirectedUrl()
    }

}
