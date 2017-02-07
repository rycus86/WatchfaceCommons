# WatchfaceCommons
Library to provide common utilities for Android Wear watchface development

Usage
-----
*For now* clone the project then run **gradlew clean build installArchives** to install it into your local Maven repository.

In the project you want to use it add this to your *repositories* configuration:
```
maven { url "${System.properties['user.home']}/.m2/repository" }
```

And finally in the module *dependencies* add:
```
compile 'hu.rycus.watchface:watchface-commons:0.8.0-SNAPSHOT'
```
