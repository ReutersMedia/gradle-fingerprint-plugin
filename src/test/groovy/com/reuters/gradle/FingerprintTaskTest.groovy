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
        def (resorucesDir, task) = createAndConfigureTask('**/*.txt', '**/*.html', 4)
        List<String> messages = []
        task.validators*.validate(task,messages)
        assertTrue(messages.size == 1)
        messages.contains(FingerprintTask.FINGERPRINT_LENGHT_OUTSIDE_RANAGE_MESSAGE)
    }

    @Test
    public void generatesChecksum() {
        def (resorucesDir, task) = createAndConfigureTask('**/*.png', '**/*.css', 10)
        task.doFingerprint()
        verifyChecksumFiles('**/*.png', 10)
    }

    def createAndConfigureTask(sourceInclude, replaceFilesInclude, fingerprintLength) {
        def task = project.task('fingerprint', type: FingerprintTask)
        def cssResource = this.getClass().getResource('/css').toString() - 'file:'
        def resourcesDir = cssResource - '/css'
        def buildDir = resourcesDir - '/resources/test'
        task.source = project.fileTree(dir: resourcesDir, include: sourceInclude)
        task.destinationDir = project.file(buildDir)
        if (fingerprintLength > 0 ) {
            task.fingerprintLength = fingerprintLength
        }
        task.replaceInFiles = project.fileTree(dir: resourcesDir , include: replaceFilesInclude)
        return [resourcesDir, task]
    }

    def verifyChecksumFiles(includePattren, fingerprintLength) {

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