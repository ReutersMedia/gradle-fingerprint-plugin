# Gradle Fingerprint Plugin

A gradle plugin to add file's checksum to its name and replaces references to the file

# Usage
If we want to fingerprint our javascript and css file and we have a the following Java webapp project strucutre:
* *javascript* files are under: src/main/webapp/js
* *css* files are under src/main/webapp/css
* *jsp and html* files are under src/main/webapp/WEB-INF/pages

Then we will add this to our **build.gradle** file:

```groovy
// Pull the plugin from maven repository
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'com.reuters:gradle-fingerprint-plugin:0.2'
    }
}

// create a fingerprint task for javascript and css file
task fingerprint(type: com.reuters.gradle.FingerprintTask) {
    description = 'fingerprint front end resources'
    group = 'build'
    source fileTree(dir: 'src/main/webapp', include: ['**/*.js', '**/*.css'])
    destinationDir = file('build/fingerprint')
    // replace references to javascript and css files in html and jsp files
    replaceInFiles = fileTree(dir: 'src/main/webapp/WEB-INF/pages', include: ['**/*.html', '**/*.jsp'])
    replacedDestDir = file('build/fingerprint/pages')
    fingerprintLength = 10 // length of the checksum to add to file name valid values are 8-20 or default is 8
}

// configure war task to take fingerprinted files and updated jsp and html files
war.dependsOn fingerprint
war {
    exclude ('js', 'css', 'WEB-INF/pages')
    from ('build/fingerprint/js') {
        into 'js'
    }
    from ('build/fingerprint/css') {
        into 'css'
    }
    from ('build/fingerprint/pages') {
        into 'WEB-INF/pages'
    }
}
```