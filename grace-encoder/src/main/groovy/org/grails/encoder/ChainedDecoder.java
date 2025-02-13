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
package org.grails.encoder;

public class ChainedDecoder implements Decoder {

    protected final Decoder[] decoders;

    protected final CodecIdentifier codecIdentifier;

    public ChainedDecoder(Decoder[] decoders) {
        this.decoders = decoders;
        this.codecIdentifier = createCodecIdentifier(decoders);
    }

    protected CombinedCodecIdentifier createCodecIdentifier(Decoder[] decoders) {
        return new CombinedCodecIdentifier(decoders, true);
    }

    @Override
    public CodecIdentifier getCodecIdentifier() {
        return this.codecIdentifier;
    }

    @Override
    public Object decode(Object o) {
        if (o == null) {
            return null;
        }
        Object decoded = o;
        for (Decoder decoder : this.decoders) {
            decoded = decoder.decode(decoded);
        }
        return decoded;
    }

}
