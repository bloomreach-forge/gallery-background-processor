/*
 * Copyright 2017 BloomReach Inc (https://www.bloomreach.com)
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

import java.util.Map;

import org.onehippo.cms7.event.HippoEvent;

/**
 * Event for image creation (upload).
 */
public class ImageCreationEvent<T extends ImageCreationEvent<T>> extends HippoEvent<T> {

    private static final String VARIANTS = "variants";
    private static final String NODE_PATH = "nodePath";
    private static final String GALLERY = "gallery";
    private static final String MIME_TYPE = "mimeType";
    private static final String FILE_NAME = "fileName";

    public ImageCreationEvent(String application) {
        super(application);
        category(GALLERY);
    }

    /**
     * Set a mapping from variant name to JCR type.
     */
    public T variants(final Map<String, String> variants) {
        return put(VARIANTS, variants);
    }

    /**
     * Get a mapping from variant name to JCR type.
     */
    public Map<String, String> variants() {
        return get(VARIANTS);
    }

    public T nodePath(final String node) {
        return put(NODE_PATH, node);
    }

    public String nodePath() {
        return get(NODE_PATH);
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
