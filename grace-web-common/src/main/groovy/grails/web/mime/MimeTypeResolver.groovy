/*
 * Copyright 2013-2022 the original author or authors.
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

import org.grails.web.servlet.mvc.GrailsWebRequest

/**
 *
 * Resolves the response format
 *
 * @author Graeme Rocher
 * @since 2.3
 */
interface MimeTypeResolver {

    String BEAN_NAME = 'mimeTypeResolver'

    /**
     * @return The response format requested by the client
     */
    MimeType resolveResponseMimeType()

    /**
     * @return The response format requested by the client
     */
    MimeType resolveResponseMimeType(GrailsWebRequest request)

    /**
     * @return The request format sent by the client
     */
    MimeType resolveRequestMimeType()

    /**
     * @return The request format sent by the client
     */
    MimeType resolveRequestMimeType(GrailsWebRequest request)

}
