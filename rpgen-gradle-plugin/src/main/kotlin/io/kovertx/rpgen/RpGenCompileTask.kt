package io.kovertx.rpgen

import io.kovertx.rpgen.compiler.RpGenCompiler
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

open class RpGenCompileTask : DefaultTask() {

    @InputFile
    val manifestFile = project.objects.fileProperty()

    @TaskAction
    fun compile() {
        val manifest = manifestFile.get().asFile.toPath()
        RpGenCompiler.fromConfigFile(manifest).compile()
    }
}