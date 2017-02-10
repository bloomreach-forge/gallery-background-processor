/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.forge.gallery;

import org.onehippo.cms7.event.HippoEvent;

public class ImageVariantEvent<T extends ImageVariantEvent<T>> extends HippoEvent<T> {

    private static final String VARIANT = "variant";
    private static final String NODE = "node";
    private static final String GALLERY = "gallery";
    private static final String MIME_TYPE = "mimeType";
    private static final String FILE_NAME = "fileName";
    private static final String TYPE = "type";

    public ImageVariantEvent(String application) {
        super(application);
        category(GALLERY);
    }

    public ImageVariantEvent(HippoEvent<?> event) {
        super(event);
    }

    public T variant(final String variant) {
        return put(VARIANT, variant);
    }

    public String variant() {
        return get(VARIANT);
    }

    public T node(final String node) {
        return put(NODE, node);
    }

    public String node() {
        return get(NODE);
    }

    public T type(final String type) {
        return put(TYPE, type);
    }

    public String type() {
        return get(TYPE);
    }

    public T mimeType(final String mimeType) {
        return put(MIME_TYPE, mimeType);
    }


    public String mimeType() {
        return get(MIME_TYPE);
    }

    public T fileName(final String fileName) {
        return put(FILE_NAME, fileName);
    }


    public String fileName() {
        return get(FILE_NAME);
    }


}
