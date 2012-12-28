# Gradle Fingerprint Plugin 

A gradle plugin to add a file's checksum to its name and update references to the file with the new name.
For example: 
* A javascript file *foo.js* will be renamed to *foo-79aa9453.js*.
* A referernce to *foo.js* in *bar.html* like `<script src="/js/foo.js"></script>` will be replaced with `<script src="/js/foo-79aa9453.js"></script>`

For more information about assets fingerprinting and caching see Google's PageSpeed caching [documentation](https://developers.google.com/speed/docs/best-practices/caching).

### Usage 
If we want to fingerprint our javascript and css files and we have a the following Java webapp project strucutre:
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
    fingerprintLength = 10
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

### Fingerprint task configuration
* source - Files to fingerprint
* destinationDir - Output directory for fingerprinted files. Note that the relative path from the source are maintained in the output directory
* replaceInFiles *(Optional)* - Files that contain references to the fingerprinted that need to be replaced with the new fingerprinted file names. If not set the task will only fingerprint files without looking for references
* replacedDestDir *(Optional)* - Output direcotry for placing the updated referencing files. Note that the relative path from the replaceInFiles directory are maintained in the output directory. If not passed will default to destinationDir value
* fingerprintLength *(Optional)* - The file chechsum length to add to the filename. Valid values are 8-20, default value is 8
