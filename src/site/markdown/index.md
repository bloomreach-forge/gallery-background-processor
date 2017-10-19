## Gallery Background Processing Plugin
The Gallery Background Processing Plugin will move the image variant creation process from inside the upload request to a 
background process. This can be useful when an image set has many configured variants.

The plugin provides an alternative gallery processor class that only creates the original and thumbnail variants directly and 
posts an event to a repository module, that will create the other variants in the background.   

## Code on GitHub
The open source code is on GitHub at [github.com/onehippo-forge/gallery-background](https://github.com/onehippo-forge/gallery-background)  

## Contents
- [Installation instructions](install.html)
- [Configuration](configuration.html)
- [Release Notes](release-notes.html)


