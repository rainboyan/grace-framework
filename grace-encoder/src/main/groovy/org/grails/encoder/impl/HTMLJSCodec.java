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
package org.grails.encoder.impl;

import org.grails.encoder.ChainedDecoder;
import org.grails.encoder.ChainedEncoder;
import org.grails.encoder.CodecFactory;
import org.grails.encoder.Decoder;
import org.grails.encoder.Encoder;
import org.grails.encoder.StreamingEncoder;

public class HTMLJSCodec implements CodecFactory {

    protected final StreamingEncoder[] encoders;

    protected final Decoder[] decoders;

    public HTMLJSCodec() {
        this.encoders = new StreamingEncoder[] { (StreamingEncoder) new HTMLEncoder(), (StreamingEncoder) JavaScriptCodec.getENCODER() };
        this.decoders = new Decoder[] { JavaScriptCodec.getDECODER(), new HTML4Decoder() };
    }

    @Override
    public Encoder getEncoder() {
        return ChainedEncoder.createFor(this.encoders);
    }

    @Override
    public Decoder getDecoder() {
        return new ChainedDecoder(this.decoders);
    }

}
