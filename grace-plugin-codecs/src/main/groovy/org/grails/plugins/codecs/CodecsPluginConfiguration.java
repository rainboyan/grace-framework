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
package org.grails.plugins.codecs;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import grails.core.GrailsApplication;

import org.grails.encoder.CodecLookup;

/**
 * Beans for Codecs
 *
 * @author graemerocher
 * @author Michael Yan
 * @since 2022.0.0
 */
@AutoConfiguration
public class CodecsPluginConfiguration {

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public CodecLookup codecLookup(ObjectProvider<GrailsApplication> grailsApplication) {
        DefaultCodecLookup defaultCodecLookup = new DefaultCodecLookup(grailsApplication.getIfAvailable());
        defaultCodecLookup.reInitialize();
        return defaultCodecLookup;
    }

}
