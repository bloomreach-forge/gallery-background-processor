# Image Background Processing Plugin

The Image Background Processing Plugin will move image variant creation process from inside the upload request to a 
background process.

It provides an alternative gallery processor class that only creates the original and thumbnail variants directly and 
posts an event to a repository module, which will create the other variants in the background.   


## Installation
Add following dependency to *cms/pom.xml* 
``` 
<dependency>
    <groupId>org.onehippo.forge</groupId>
    <artifactId>hippo-cms-gallery-background-module</artifactId>
    <version>1.0.0-SNAPSHOT</version>
 </dependency>
```
## Configuration
To enable background processing:

At node ``/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService``
- set ``plugin.class=org.onehippo.forge.gallery.BackgroundScalingGalleryProcessorPlugin``


If needed change the module configuration: 

At node ``/hippo:configuration/hippo:modules/image-scaling-module/hippo:moduleconfig``
- set ``maxRetry`` (default 5): number of retries to recreate an variant in case original image was not available yet
- set ``delay`` (default 1000): initial delay in milliseconds, which is used to calculate retry, exponential backoff time
