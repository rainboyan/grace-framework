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

import java.nio.charset.StandardCharsets

import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import jakarta.servlet.DispatcherType
import jakarta.servlet.ServletContext
import jakarta.servlet.ServletInputStream
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Part

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.util.Assert
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest

import grails.converters.JSON
import grails.converters.XML
import grails.web.mime.MimeType

import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.grails.web.util.WebUtils

/**
 * A custom mock HTTP servlet request that provides the extra properties
 * and methods normally injected by the "servlets" plugin.
 */
class GrailsMockHttpServletRequest extends MockHttpServletRequest implements MultipartHttpServletRequest {

    boolean invalidToken
    MultiValueMap multipartFiles = new LinkedMultiValueMap<String, MultipartFile>()

    private final Map<String, String> multipartContentTypes = Collections.emptyMap()
    private final Map<String, HttpHeaders> multipartHeaders = Collections.emptyMap()
    private final HttpHeaders httpHeaders = new HttpHeaders()

    HttpHeaders requestHeaders
    HttpMethod requestMethod = HttpMethod.GET

    private cachedJson
    private cachedXml
    DispatcherType dispatcherType
    AsyncContext asyncContext
    private ServletInputStream cachedInputStream

    public GrailsMockHttpServletRequest() {
        super()
        method = 'GET'
    }

    public GrailsMockHttpServletRequest(ServletContext servletContext) {
        super(servletContext)
        method = 'GET'
    }

    /**
     * Sets the request format to use
     * @param format The request format
     */
    void setFormat(String format) {
        setAttribute(GrailsApplicationAttributes.CONTENT_FORMAT, format)
    }

    @Override
    void setContentType(String newContentType) {
        super.setContentType(newContentType)
        def webRequest = getAttribute(GrailsApplicationAttributes.WEB_REQUEST)
        def mimeType = MimeType.configuredMimeTypes?.find { mt ->
            mt?.name == newContentType
        }
        mimeType = mimeType ?: new MimeType(newContentType)

        setAttribute(GrailsApplicationAttributes.REQUEST_FORMATS, [mimeType] as MimeType[])
    }

    /**
     * Sets the body of the request to be a json packet
     *
     * @param sourceJson The source json
     */
    void setJson(Object sourceJson) {
        setContentType('application/json; charset=UTF-8')
        setFormat('json')
        if (sourceJson instanceof String) {
            setContent(sourceJson.getBytes(StandardCharsets.UTF_8))
        }
        else if (sourceJson instanceof JSON) {
            setContent(sourceJson.toString().getBytes(StandardCharsets.UTF_8))
        }
        else {
            setContent(new JSON(sourceJson).toString().getBytes(StandardCharsets.UTF_8))
        }
        getAttribute('org.codehaus.groovy.grails.WEB_REQUEST')?.informParameterCreationListeners()
    }

    /**
     * Sets the body of the request to be an XML packet
     *
     * @param sourceXml
     */
    void setXml(Object sourceXml) {
        setContentType('text/xml; charset=UTF-8')
        setFormat('xml')

        if (sourceXml instanceof String) {
            setContent(sourceXml.getBytes(StandardCharsets.UTF_8))
        }
        else {
            XML xml
            if (sourceXml instanceof XML) {
                xml = (XML) sourceXml
            }
            else {
                xml = new XML(sourceXml)
            }
            setContent(xml.toString().getBytes(StandardCharsets.UTF_8))
        }

        getAttribute('org.codehaus.groovy.grails.WEB_REQUEST')?.informParameterCreationListeners()
    }

    void setXML(Object sourceXml) {
        setXml(sourceXml)
    }

    void setJSON(Object sourceJson) {
        setJson(sourceJson)
    }

    /**
     * Implementation of the dynamic "forwardURI" property.
     */
    String getForwardURI() {
        getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) ?: requestURI
    }

    /**
     * Sets the "forwardURI" property for the request.
     */
    void setForwardURI(String uri) {
        setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, uri)
    }

    /**
     * Indicates whether this is an AJAX request or not (as far as
     * Grails is concerned). Returns <code>true</code> if it is an
     * AJAX request, otherwise <code>false</code>.
     */
    boolean isXhr() {
        getHeader('X-Requested-With') == 'XMLHttpRequest'
    }

    /**
     * Makes this request an AJAX request as Grails understands it.
     * This cannot be undone, so if you need a non-AJAX request you
     * will have to create a new instance.
     */
    void makeAjaxRequest() {
        addHeader('X-Requested-With', 'XMLHttpRequest')
    }

    /**
     * Map-like access to request attributes, e.g. request["count"].
     */
    def getAt(String key) {
        getAttribute(key)
    }

    /**
     * Map-like setting of request attributes, e.g. request["count"] = 10.
     */
    void putAt(String key, Object val) {
        setAttribute(key, val)
    }

    /**
     * Property access for request attributes.
     */
    def getProperty(String name) {
        def mp = getClass().metaClass.getMetaProperty(name)
        mp ? mp.getProperty(this) : getAttribute(name)
    }

    /**
     * Property setting of request attributes.
     */
    void setProperty(String name, value) {
        def mp = getClass().metaClass.getMetaProperty(name)
        if (mp) {
            mp.setProperty(this, value)
        }
        else {
            setAttribute(name, value)
        }
    }

    boolean isGet() {
        method == 'GET'
    }

    boolean isPost() {
        method == 'POST'
    }

    /**
     * Parses the request content as XML using XmlSlurper and returns
     * the GPath result object. Throws an exception if there is no
     * content or the content is not valid XML.
     */
    def getXML() {
        cachedXml = cachedXml ?: GrailsMockHttpServletRequest.classLoader.loadClass('grails.converters.XML').parse(this)
    }

    /**
     * Parses the request content as JSON using the JSON converter.
     * Throws an exception if there is no content or the content is
     * not valid JSON.
     */
    def getJSON() {
        cachedJson = cachedJson ?: GrailsMockHttpServletRequest.classLoader.loadClass('grails.converters.JSON').parse(this)
    }

    /**
     * Adds a "find()" method to the request that searches the request's
     * attributes. Returns the first attribute for which the closure
     * returns <code>true</code>, just like the normal Groovy find() method.
     */
    def find(Closure c) {
        def result = [:]
        for (String name in attributeNames) {
            def match = false
            switch (c.parameterTypes.length) {
                case 0:
                    match = c.call()
                    break

                case 1:
                    match = c.call(key: name, value: getAttribute(name))
                    break

                default:
                    match = c.call(name, getAttribute(name))
            }
            if (match) {
                result[name] = getAttribute(name)
                break
            }
        }

        result
    }

    /**
     * Like the {@link #find(Closure)} method, this searches the request
     * attributes. Returns all the attributes that match the closure
     * conditions.
     */
    def findAll(Closure c) {
        def results = [:]
        for (String name in attributeNames) {
            def match = false
            switch (c.parameterTypes.length) {
                case 0:
                    match = c.call()
                    break

                case 1:
                    match = c.call(key: name, value: getAttribute(name))
                    break

                default:
                    match = c.call(name, getAttribute(name))
            }
            if (match) {
                results[name] = getAttribute(name)
            }
        }
        results
    }

    /**
     * Iterates over the request attributes.
     */
    def each(Closure c) {
        for (String name in attributeNames) {
            switch (c.parameterTypes.length) {
                case 0:
                    c.call()
                    break

                case 1:
                    c.call(key: name, value: getAttribute(name))
                    break

                default:
                    c.call(name, getAttribute(name))
            }
        }
    }

    /**
     * {@inheritDoc }
     */
    Iterator<String> getFileNames() {
        multipartFiles.keySet().iterator()
    }

    /**
     * {@inheritDoc }
     */
    MultipartFile getFile(String name) {
        multipartFiles.getFirst(name)
    }

    /**
     * {@inheritDoc }
     */
    List<MultipartFile> getFiles(String name) {
        multipartFiles.get(name)
    }

    /**
     * {@inheritDoc }
     */
    Map<String, MultipartFile> getFileMap() {
        multipartFiles.toSingleValueMap()
    }

    /**
     * {@inheritDoc }
     */
    MultiValueMap<String, MultipartFile> getMultiFileMap() {
        multipartFiles
    }

    /**
     * Add a file to this request. The parameter name from the multipart
     * form is taken from the {@link MultipartFile#getName()}.
     * @param file multipart file to be added
     */
    void addFile(MultipartFile file) {
        setMethod('POST')
        setContentType('multipart/form-data')

        Assert.notNull(file, 'MultipartFile must not be null')
        multipartFiles.add(file.getName(), file)
    }

    /**
     * Add a file for the given location and bytes
     *
     * @param location The location
     * @param contents The bytes
     */
    void addFile(String location, byte[] contents) {
        setMethod('POST')
        setContentType('multipart/form-data')

        multipartFiles.add(location, new GrailsMockMultipartFile(location, contents))
    }

    @Override
    void clearAttributes() {
        super.clearAttributes()
        multipartFiles.clear()
        multipartContentTypes.clear()
        httpHeaders.clear()
    }

    String getMultipartContentType(String paramOrFileName) {
        multipartContentTypes.get(paramOrFileName)
    }

    String setMultipartContentType(String paramOrFileName, String contentType) {
        multipartContentTypes.put(paramOrFileName, contentType)
    }

    HttpHeaders getMultipartHeaders(String paramOrFileName) {
        multipartHeaders.get(paramOrFileName)
    }

    HttpHeaders setMultipartHeaders(String paramOrFileName, HttpHeaders headers) {
        multipartHeaders.put(paramOrFileName, headers)
    }

    Collection<Part> getParts() {
        getMultiFileMap().values().flatten().collect { new MockPart(it) }
    }

    Part getPart(String name) {
        MultipartFile file = getFile(name)
        file ? new MockPart(file) : null
    }

    AsyncContext startAsync() {
        def webRequest = GrailsWebRequest.lookup()
        def response = webRequest?.currentResponse
        if (response == null) {
            response = new GrailsMockHttpServletResponse()
        }
        startAsync(this, response)
    }

    AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
        new MockAsyncContext(servletRequest, servletResponse)
    }

    boolean isAsyncStarted() { asyncContext != null }

    boolean isAsyncSupported() { true }

    @Override
    ServletInputStream getInputStream() {
        if (cachedInputStream == null) {
            cachedInputStream = super.getInputStream()
        }
        cachedInputStream
    }

}

class MockPart implements Part {

    MultipartFile file
    MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>()

    MockPart(MultipartFile file) {
        this.file = file
    }

    @Override
    InputStream getInputStream() {
        file.inputStream
    }

    @Override
    String getContentType() {
        file.contentType
    }

    @Override
    String getName() {
        file.name
    }

    @Override
    String getSubmittedFileName() {
        'N/A'
    }

    @Override
    long getSize() {
        file.size
    }

    @Override
    void write(String fileName) {
        file.transferTo(new File(fileName))
    }

    @Override
    void delete() {
        // no-op
    }

    @Override
    String getHeader(String name) {
        headers.getFirst(name)
    }

    @Override
    Collection<String> getHeaders(String name) {
        headers[name]
    }

    @Override
    Collection<String> getHeaderNames() {
        headers.keySet()
    }

}

class MockAsyncContext implements AsyncContext {

    ServletRequest request
    ServletResponse response
    String dispatchUri
    long timeout = -1
    List asyncListeners = []

    MockAsyncContext(HttpServletRequest request, HttpServletResponse response) {
        this.request = request
        this.response = response
    }

    boolean hasOriginalRequestAndResponse() {
        true
    }

    void dispatch() {
        dispatchUri = request.requestURI
    }

    void dispatch(String path) {
        dispatchUri = path
    }

    void dispatch(ServletContext context, String path) {
        dispatchUri = path
    }

    void complete() {
        // no op
    }

    void start(Runnable run) {
        try {
            for (listener in asyncListeners) {
                AsyncListener al = listener.listener
                al.onStartAsync(listener.event)
            }
            run.run()
            for (listener in asyncListeners) {
                AsyncListener al = listener.listener
                al.onComplete(listener.event)
            }
        }
        catch (e) {
            for (listener in asyncListeners) {
                AsyncListener al = listener.listener
                al.onError(new AsyncEvent(this, e))
            }
        }
    }

    void addListener(AsyncListener listener) {
        asyncListeners << [listener: listener, event: new AsyncEvent(this, request, response)]
    }

    void addListener(AsyncListener listener, ServletRequest servletRequest, ServletResponse servletResponse) {
        asyncListeners << [listener: listener, event: new AsyncEvent(this, servletRequest, servletResponse)]
    }

    def <T extends AsyncListener> T createListener(Class<T> clazz) {
        clazz.newInstance()
    }

}
