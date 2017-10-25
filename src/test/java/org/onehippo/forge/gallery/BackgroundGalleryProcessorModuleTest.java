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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class BackgroundGalleryProcessorModuleTest {

    private static final Logger log = LoggerFactory.getLogger(BackgroundGalleryProcessorModuleTest.class);

    @Test
    public void testBackoff() throws Exception {
        final BackgroundGalleryProcessorModule backgroundGalleryProcessorModule = new BackgroundGalleryProcessorModule();
        long oldBackoff = 0;
        for (int i = 0; i < 50; i++) {
            long backoff = backgroundGalleryProcessorModule.backoff(i);
            log.info("backoff {} = {}", i, backoff);
            assertTrue(backoff > oldBackoff);
            oldBackoff = backoff;
        }
    }
}
