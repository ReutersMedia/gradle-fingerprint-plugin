package com.reuters.gradle

import static org.junit.Assert.assertTrue

import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
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
        List<String> messages = []
        assertTrue(task instanceof FingerprintTask)
    }

    @Test
    public void generatesChecksum() {
        def srcDir = tempDir.newFolder()
        addTestFile(srcDir, 'test.js');
        def task = project.task('fingerprint', type: FingerprintTask)
        task.source = srcDir
        task.destinationDir = srcDir
        task.fingerprintLength = 10
        task.doFingerprint()
        verifyChecksumFile(srcDir.absolutePath, task.destinationDir.absolutePath, 'test.js', 10)
    }

    def verifyChecksumFile(srcDir, destDir, filename, fingerprintLength) {
        def fileExtension = filename.lastIndexOf('.').with { index -> index > -1 ? filename[index..-1] : ''}
        def filenameOnly = filename - fileExtension
        def fileFilter = "$filenameOnly-.{$fingerprintLength}\\$fileExtension"
        println fileFilter
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

    def addTestFile(dir, filename) {
        new File("$dir").mkdirs()
        addFile("$dir/$filename", "some dummy content")
    }

    def addFile(name, contents) {
        def file = new File(name)
        file << contents
    }
}