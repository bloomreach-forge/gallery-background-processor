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

package org.onehippo.cms7.gallery;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

public class ImageScalingModuleTest {

    private static final Logger log = LoggerFactory.getLogger(ImageScalingModuleTest.class);
    @Test
    public void testBackoff() throws Exception {
        final ImageScalingModule imageScalingModule = new ImageScalingModule();
        long oldBackoff = 0;
        for (int i = 0; i < 50; i++) {
            long backoff = imageScalingModule.backoff(i);
            log.info("backoff {} = {}",i, backoff);
            assertTrue(backoff > oldBackoff);
            oldBackoff = backoff;
        }

    }
}