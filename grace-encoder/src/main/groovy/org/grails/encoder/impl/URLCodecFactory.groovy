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
package org.grails.encoder.impl

import groovy.transform.CompileStatic
import org.grails.encoder.CodecFactory
import org.grails.encoder.CodecIdentifier
import org.grails.encoder.Decoder
import org.grails.encoder.DefaultCodecIdentifier
import org.grails.encoder.Encoder

/**
 * Implements the 'www-form-urlencoded' encoding scheme, also misleadingly known as URL encoding.
 *
 * @see <a href="http://www.w3.org/TR/html4/interact/forms.html#h-17.13.4.1">Chapter 17.13.4 Form content types</a>
 *           of the <a href="http://www.w3.org/TR/html4/">HTML 4.01 Specification</a>
 *
 * @since 2.4
 */
@CompileStatic
class URLCodecFactory implements CodecFactory {

    static final CodecIdentifier URL_CODEC_IDENTIFIER = new DefaultCodecIdentifier('URL')

    Encoder encoder = new Encoder() {

        @Override
        CodecIdentifier getCodecIdentifier() {
            URL_CODEC_IDENTIFIER
        }

        Object encode(Object o) {
            if (o == null) {
                return null
            }
            URLEncoder.encode(String.valueOf(o), resolveEncoding())
        }

        boolean isApplyToSafelyEncoded() {
            true
        }

        boolean isSafe() {
            true
        }

        void markEncoded(CharSequence string) {
        }

    }

    Decoder decoder = new Decoder() {

        CodecIdentifier getCodecIdentifier() {
            URL_CODEC_IDENTIFIER
        }

        @Override
        Object decode(Object o) {
            if (o == null) {
                return null
            }
            URLDecoder.decode(String.valueOf(o), resolveEncoding())
        }

    }

    protected String resolveEncoding() {
        'UTF-8'
    }

}
