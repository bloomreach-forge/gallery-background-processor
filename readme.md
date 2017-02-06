Add following dependency to *cms/pom.xml* 
``` 
<dependency>
      <groupId>org.onehippo.cms7</groupId>
      <artifactId>hippo-cms-gallery-background-module</artifactId>
      <version>1.0.0-SNAPSHOT</version>
 </dependency>
```

To enable background processing change:
``plugin.clas`` property to: ``org.onehippo.cms7.gallery.BackgroundScalingGalleryProcessorPlugin``
 [http://localhost:8080/cms/console/?1&path=/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService](http://localhost:8080/cms/console/?1&path=/hippo:configuration/hippo:frontend/cms/cms-services/galleryProcessorService)

If needed change: 
 [http://localhost:8080/cms/console/?1&path=/hippo:configuration/hippo:modules/image-scaling-module/hippo:moduleconfig](http://localhost:8080/cms/console/?1&path=/hippo:configuration/hippo:modules/image-scaling-module/hippo:moduleconfig)

___

 ``maxRetry`` *5* (number of retries to recreate an variant in case original image was not available yet)

___

 ``delay`` *1000* (initial delay in milliseconds, which is used to calculate retry, exponential backoff time)