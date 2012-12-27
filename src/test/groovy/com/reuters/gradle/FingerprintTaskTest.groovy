package com.reuters.gradle

import static org.junit.Assert.assertTrue

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FingerprintTaskTest {
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder()

    public Project project = ProjectBuilder.builder().build()

    @Test
    public void canAddTaskToProject() {
        def task = project.task('fingerprint', type: FingerprintTask)
        assertTrue(task instanceof FingerprintTask)
    }

    @Test
    public void fingerprintLenghNotInRange() {
        def (resorucesDir, task) = createAndConfigureTask('**/*.txt', '', '**/*.html', '', 4)
        List<String> messages = []
        task.validators*.validate(task,messages)
        assertTrue(messages.size == 1)
        messages.contains(FingerprintTask.FINGERPRINT_LENGHT_OUTSIDE_RANAGE_MESSAGE)
    }

    @Test
    public void fingerprintImages() {
        def fingerprintLength = 10
        def (resourcesDir, task) = createAndConfigureTask(['**/*.png', '**/*.gif'], 'expected/**/*', '**/*.css',
            'expected/**/*', fingerprintLength)
        task.doFingerprint()

        // verify images
        verifyChecksumFile(resourcesDir + '/img', task.destinationDir.path + '/img', 'glyphicons-halflings-white.png',
            fingerprintLength)
        verifyChecksumFile(resourcesDir + '/img', task.destinationDir.path + '/img', 'glyphicons-halflings.png',
            fingerprintLength)
        verifyChecksumFile(resourcesDir + '/img/gifs', task.destinationDir.path + '/img/gifs',
            'transparent.gif', fingerprintLength)
        // verify references in css files
        assertTrue(doFilesHaveSameContent(resourcesDir + '/expected/css/print.css',
            task.destinationDir.path + '/css/print.css'))
        assertTrue(doFilesHaveSameContent(resourcesDir + '/expected/css/style.css',
            task.destinationDir.path + '/css/style.css'))
    }

    @Test
    public void fingerprintJavascriptFiles() {
        def fingerprintLength = 10
        def (resourcesDir, task) = createAndConfigureTask('**/*.js', 'expected/**/*', '**/*.html',
            'expected/**/*', fingerprintLength)
        task.doFingerprint()

        // verify images
        verifyChecksumFile(resourcesDir + '/js', task.destinationDir.path + '/js', 'jquery.js',
            fingerprintLength)
        verifyChecksumFile(resourcesDir + '/js', task.destinationDir.path + '/js', 'bootstrap.js',
            fingerprintLength)
        verifyChecksumFile(resourcesDir + '/js/min', task.destinationDir.path + '/js/min', 'jquery.min.js',
            fingerprintLength)
        verifyChecksumFile(resourcesDir + '/js/min', task.destinationDir.path + '/js/min', 'bootstrap.min.js',
            fingerprintLength)
        // verify references in html files
        assertTrue(doFilesHaveSameContent(resourcesDir + '/expected/pages/index.html',
            task.destinationDir.path + '/pages/index.html'))
        assertTrue(doFilesHaveSameContent(resourcesDir + '/expected/pages/min/other.html',
            task.destinationDir.path + '/pages/min/other.html'))
    }

    def createAndConfigureTask(sourceInclude, sourceExclude, replaceFilesInclude, replaceFilesExclude,
        fingerprintLength) {

        def task = project.task('fingerprint', type: FingerprintTask)
        def cssResource = this.getClass().getResource('/css').toString() - 'file:'
        def resourcesDir = cssResource - '/css'
        def buildDir = resourcesDir - '/resources/test'
        buildDir = buildDir + '/output'
        new File(buildDir).mkdirs()
        task.source = project.fileTree(dir: resourcesDir, include: sourceInclude, exclude: sourceExclude)
        task.destinationDir = project.file(buildDir)
        if (fingerprintLength > 0 ) {
            task.fingerprintLength = fingerprintLength
        }
        task.replaceInFiles = project.fileTree(dir: resourcesDir , include: replaceFilesInclude,
            exclude: replaceFilesExclude)
        return [resourcesDir, task]
    }

    def verifyChecksumFiles(task, includePattren, fingerprintLength) {
        println task.destinationDir
    }

    def verifyChecksumFile(srcDir, destDir, filename, fingerprintLength) {
        def fileExtension = filename.lastIndexOf('.').with { index -> index > -1 ? filename[index..-1] : ''}
        def filenameOnly = filename - fileExtension
        def fileFilter = "$filenameOnly-.{$fingerprintLength}\\$fileExtension"
        def destFile = []
        new File(destDir).eachFileMatch ~fileFilter, { file ->
            destFile << file.name
        }
        assertTrue(destFile.size == 1)
        destFile = destFile[0]
        assertTrue(doFilesHaveSameContent("$srcDir/$filename", "$destDir/$destFile"))
    }

    def doFilesHaveSameContent(fileA, fileB) {
        def inFileA = [:]
        new File(fileA).eachLine {
            inFileA.put(it, null)
        }
        new File(fileB).eachLine {
            if(!inFileA.containsKey(it)) {
                return false;
            }
        }
        return true
    }
}