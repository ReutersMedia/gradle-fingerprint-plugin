package com.reuters.gradle

import org.gradle.api.file.FileTree
import org.gradle.api.internal.tasks.execution.TaskValidator
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class FingerprintTask extends SourceTask {

    public static final String FINGERPRINT_LENGHT_OUTSIDE_RANAGE_MESSAGE =
        'fingerprintLength should be between 8 and 20'

    @OutputDirectory def destinationDir
    @Input @Optional def fingerprintLength
    @Input FileTree replaceInFiles
    @Input @Optional def replacedDestDir

    { // add validator for length
        addValidator({ task, messages ->
            if (fingerprintLength != null &&
                (!fingerprintLength instanceof Integer || fingerprintLength < 8 || fingerprintLength > 20)) {
                messages.add(FINGERPRINT_LENGHT_OUTSIDE_RANAGE_MESSAGE)
            }
        } as TaskValidator)
    }

    File getDestinationDir() {
        project.file(destinationDir)
    }

    File getReplacedDestDir() {
        replacedDestDir != null ? project.file(replacedDestDir) : destinationDir
    }

    @TaskAction
    def doFingerprint() {
        // sort files by path+name length so we first match the longest ones
        def filenameMap = new TreeMap([ compare: {a, b ->
            if (b.length() - a.length() == 0) {
                b.compareTo(a)
            } else {
                b.length() - a.length()
            }
            }] as Comparator)
        source.visit { sourceFile ->
            if (!sourceFile.directory) {
                def checksumProperty = "$sourceFile.file.absolutePath-checksum"
                project.ant.checksum(file: sourceFile.file, algorithm: 'SHA', property: checksumProperty)
                def checksum = fingerprintLength ? project.ant.properties[checksumProperty][1..fingerprintLength] :
                    project.ant.properties[checksumProperty]
                def outputFile = buildOutputFilename(sourceFile.file, checksum)
                def relativePath = sourceFile.relativePath.replaceLastName('')
                project.ant.copyfile(src: sourceFile.file.absolutePath,
                    dest: "$destinationDir.path/$relativePath$outputFile")
                filenameMap["$relativePath$sourceFile.file.name"] = "$relativePath$outputFile"
            }
        }
        // only try to replace names if we actually fingerprinted some files
        if (filenameMap.size() > 0) {
            replaceInFiles.visit { replaceFile ->
                if (!replaceFile.directory) {
                    def contents = replaceFile.file.text
                    filenameMap.each() { originalName, fingerprintedName ->
                        contents = contents.replaceAll(originalName, fingerprintedName)
                    }
                    def relativePath = replaceFile.relativePath.replaceLastName('')
                    new File("$replacedDestDir.path/$relativePath").mkdirs()
                    def file = new File("$replacedDestDir.path/$relativePath$replaceFile.file.name")
                    file.write(contents)
                }
            }
        }
    }

    def buildOutputFilename(File file, String checksum) {
        file.name.lastIndexOf('.').with { index ->
            index > -1 ? file.name[0..<index] + "-$checksum" + file.name[index..-1] : file.name + "-$checksum"
        }
    }
}