<!--
  Copyright 2017 Hippo B.V. (http://www.onehippo.com)

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->
## Configuration Options
To enable background processing:

At the node ``/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService`` set:

+ ``plugin.class = org.onehippo.forge.gallery.BackgroundScalingGalleryProcessorPlugin``


If needed change the module configuration. At node ``/hippo:configuration/hippo:modules/gallery-background-module/hippo:moduleconfig`` set:

+ ``maxRetry`` (default 5): number of retries to recreate an variant in case original image was not available yet
+ ``delay`` (default 1000): initial delay in milliseconds, which is used to calculate retry, exponential backoff time
