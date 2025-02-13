/*
 * Copyright 2011-2022 the original author or authors.
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
package grails.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Burt Beckwith
 * @since 2.0
 */
public class Holder<T> {

    private final Map<Integer, T> instances = new ConcurrentHashMap<>();

    // TODO remove mappedOnly and singleton
    private T singleton;

    private final String name;

    public Holder(String name) {
        this.name = name;
    }

    public T get() {
        return get(false);
    }

    public T get(boolean mappedOnly) {
        T t = this.instances.get(getClassLoaderId());
        if (t != null) {
            return t;
        }

        t = lookupSecondary();
        if (t != null) {
            return t;
        }

//        t = instances.get(System.identityHashCode(getClass().getClassLoader()));
        if (!mappedOnly) {
            t = this.singleton;
        }
        return t;
    }

    protected T lookupSecondary() {
        // override in subclass if needed
        return null;
    }

    public void set(T t) {
        int id = getClassLoaderId();
        int thisClassLoaderId = System.identityHashCode(getClass().getClassLoader());
        if (t == null) {
            this.instances.remove(id);
            this.instances.remove(thisClassLoaderId);
        }
        else {
            this.instances.put(id, t);
            this.instances.put(thisClassLoaderId, t);
        }
        this.singleton = t;
    }

    private int getClassLoaderId() {
        return Environment.isWarDeployed() ? System.identityHashCode(Thread.currentThread().getContextClassLoader()) :
                System.identityHashCode(getClass().getClassLoader());
    }
}
