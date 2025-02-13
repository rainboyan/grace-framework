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
package grails.web.mime

import groovy.transform.CompileStatic
import org.springframework.context.ApplicationContext

import grails.util.Holders

import org.grails.web.servlet.mvc.GrailsWebRequest

/**
 * @author Graeme Rocher
 * @since 1.0
 */
@CompileStatic
class MimeType {

    /**
     * The bean name used to store the mime type definitions
     */
    public static final String BEAN_NAME = 'mimeTypes'

    public static final MimeType ALL = new MimeType('*/*', 'all')
    public static final MimeType FORM = new MimeType('application/x-www-form-urlencoded', 'form')
    public static final MimeType MULTIPART_FORM = new MimeType('multipart/form-data', 'multipartForm')
    public static final MimeType HTML = new MimeType('text/html', 'html')
    public static final MimeType XHTML = new MimeType('application/xhtml+xml', 'html')
    public static final MimeType XML = new MimeType('application/xml', 'xml')
    public static final MimeType JSON = new MimeType('application/json', 'json')
    public static final MimeType TEXT_XML = new MimeType('text/xml', 'xml')
    public static final MimeType TEXT_JSON = new MimeType('text/json', 'json')
    public static final MimeType HAL_JSON = new MimeType('application/hal+json', 'json')
    public static final MimeType HAL_XML = new MimeType('application/hal+xml', 'xml')
    public static final MimeType ATOM_XML = new MimeType('application/atom+xml', 'xml')
    public static final MimeType JSON_API = new MimeType('application/vnd.api+json', 'json')

    private static final DEFAULTS = createDefaults()
    public static final String QUALITY_RATING = '1.0'
    public static final BigDecimal QUALITY_RATING_NUMBER = 1.0

    String name
    String extension
    Map<String, String> parameters = [q: QUALITY_RATING]

    private BigDecimal qualityNumberField

    MimeType(String name, Map params = [:]) {
        this(name, null, params)
    }

    MimeType(String name, String extension, Map<String, String> params = [:]) {
        if (name && name.contains(';')) {
            List tokenWithArgs = name.split(';').toList()
            name = tokenWithArgs[0]
            List<String> paramsList = tokenWithArgs[1..-1]
            paramsList.each { String it ->
                int i = it.indexOf('=')
                if (i > -1) {
                    this.parameters[it[0..i - 1].trim()] = it[i + 1..-1].trim()
                }
            }
        }
        this.name = name
        this.extension = extension
        this.parameters.putAll(params)
    }

    /**
     * @return The quality of the Mime type
     */
    String getQuality() {
        this.parameters.q ?: QUALITY_RATING
    }

    /**
     * @return The quality in BigDecimal form
     */
    BigDecimal getQualityAsNumber() {
        if (this.qualityNumberField == null) {
            this.qualityNumberField = getOrConvertQualityParameterToBigDecimal(this)
        }
        this.qualityNumberField
    }

    /**
     * @return The version of the Mime type
     */
    String getVersion() {
        this.parameters.v ?: null
    }

    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }

        MimeType mimeType = (MimeType) o

        if (this.name != mimeType.name) {
            return false
        }

        if (this.version && this.version != mimeType.version) {
            return false
        }

        true
    }

    int hashCode() {
        int result = this.name.hashCode()
        result = 31 * result + (this.version != null ? this.version.hashCode() : 0)
        result
    }

    String toString() {
        "MimeType { name=${this.name},extension=${this.extension},parameters=${this.parameters} }"
    }

    /**
     * @return An array of MimeTypes
     */
    static MimeType[] getConfiguredMimeTypes() {
        ApplicationContext ctx = Holders.findApplicationContext()
        if (ctx == null) {
            ctx = GrailsWebRequest.lookup()?.getApplicationContext()
        }
        (MimeType[]) (ctx?.containsBean(BEAN_NAME) ? ctx?.getBean(BEAN_NAME, MimeType[]) : DEFAULTS)
    }

    /**
     * Creates the default MimeType configuration if none exists in Config.groovy
     */
    static MimeType[] createDefaults() {
        def mimes = []
        mimes << XHTML
        mimes << HTML
        mimes << XML
        mimes << TEXT_XML
        mimes << JSON
        mimes << TEXT_JSON
        mimes as MimeType[]
    }

    private BigDecimal getOrConvertQualityParameterToBigDecimal(MimeType mt) {
        BigDecimal bd
        try {
            String q = mt.parameters.q
            if (q == null) {
                return QUALITY_RATING_NUMBER
            }
            bd = q.toString().toBigDecimal()
            // replace to avoid expensive conversion again
            mt.parameters.q = bd
            return bd
        }
        catch (NumberFormatException ignored) {
            bd = QUALITY_RATING_NUMBER
            return bd
        }
    }

}
