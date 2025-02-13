/*
 * Copyright 2021-2022 the original author or authors.
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
package org.grails.plugins.i18n

import groovy.transform.CompileStatic
import org.springframework.context.MessageSource
import org.springframework.lang.Nullable

/**
 * An extension that adds methods to the {@link MessageSource} object.
 *
 * @author Michael Yan
 * @since 2022.0.0
 */
@CompileStatic
class GrailsMessageSourceExtension {

    static String message(MessageSource messageSource, String code, String lang = 'en') {
        messageSource.getMessage(code, new Object[] {}, Locale.forLanguageTag(lang.replace('_', '-')))
    }

    static String message(MessageSource messageSource, String code, @Nullable List<Object> args, String lang = 'en') {
        messageSource.getMessage(code, args?.toArray(), Locale.forLanguageTag(lang.replace('_', '-')))
    }

}
