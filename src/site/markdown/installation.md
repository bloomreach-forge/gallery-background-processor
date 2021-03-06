<!--
  Copyright 2017-2019 BloomReach Inc (https://www.bloomreach.com)

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
## Installation
These instructions assume a Hippo CMS project based on the Hippo website archetype, i.e. a Maven multi-module project 
with parent pom `org.onehippo.cms7:hippo-cms7-release` and consisting of at least sub-modules /cms and /cms-dependencies.

### Forge Repository
In the main pom.xml of the project, in the repositories section, add this repository if it is not configured there yet. 

```
<repository>
  <id>hippo-forge</id>
  <name>Bloomreach Forge maven 2 repository.</name>
  <url>https://maven.onehippo.com/maven2-forge/</url>
</repository>
```

### Dependency Management 
Add this property to the properties section of the root pom.xml:

    <bloomreach.forge.gallery-background-processor.version>2.0.0</bloomreach.forge.gallery-background-processor.version>

Select the correct version for your project. See the [release notes](release-notes.html) for more information on which 
version is applicable.

Add this dependency to the `<dependencyManagement>` section of the root pom.xml:

```
<dependency>
  <groupId>org.bloomreach.forge.gallery-background-processor</groupId>
  <artifactId>gallery-background-processor</artifactId>
  <version>${bloomreach.forge.gallery-background-processor.version}</version>
</dependency>
```
### Installation in the CMS application
Add the following dependency to `cms-dependencies/pom.xml`.
 
``` 
<dependency>
  <groupId>org.bloomreach.forge.gallery-background-processor</groupId>
  <artifactId>gallery-background-processor</artifactId>
</dependency>
```

Rebuild and start your project, then <a href="configuration.html">configure the BackgroundScalingGalleryProcessorPlugin</a>. 
In case you start with an existing repository don't forget to add *-Drepo.bootstrap=true* to your startup options. 

