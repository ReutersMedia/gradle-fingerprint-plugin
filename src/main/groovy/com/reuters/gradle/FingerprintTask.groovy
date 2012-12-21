package com.reuters.gradle

import org.gradle.api.internal.tasks.execution.TaskValidator
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

class FingerprintTask extends SourceTask {
    @OutputDirectory def destinationDir

    @Input @Optional def fingerprintLength

    @Input def replaceNameInFiles

    { // add validator for length
        addValidator({ task, messages ->
            if (fingerprintLength != null &&
                (!fingerprintLength instanceof Integer || fingerprintLength < 8 || fingerprintLength > 20)) {
                messages.add('fingerprintLength should be between 8 and 20')
            }
        } as TaskValidator)
    }

    File getDestinationDir() {
        project.file(destinationDir)
    }

    @TaskAction
    def doFingerprint() {
        source.visit { element ->
            if (!element.directory) {
                project.ant.checksum(file: element.file, algorithm: 'SHA', property: 'checksum')
                def checksum = fingerprintLength ? project.ant.properties['checksum'][1..fingerprintLength] :
                    project.ant.properties['checksum']
                def outputFile = buildOutputFilename(element.file, checksum)
                def relativePath = element.relativePath.replaceLastName('')
                project.ant.copyfile(src: element.file.absolutePath,
                    dest: "$destinationDir.path/$relativePath$outputFile")
            }
        }
    }

    def buildOutputFilename(File file, String checksum) {
        file.name.lastIndexOf('.').with { index ->
            index > -1 ? file.name[0..<index] + "-$checksum" + file.name[index..-1] : file.name + "-$checksum"
        }
    }
}