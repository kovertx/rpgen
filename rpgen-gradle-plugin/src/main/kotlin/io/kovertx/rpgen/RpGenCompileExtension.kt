package io.kovertx.rpgen
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile

open class RpGenCompileExtension(project: Project) {

    @InputFile
    val manifestFile: RegularFileProperty = project.objects.fileProperty()
}