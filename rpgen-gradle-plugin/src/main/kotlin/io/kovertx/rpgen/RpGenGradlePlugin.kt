package io.kovertx.rpgen

import org.gradle.api.Plugin
import org.gradle.api.Project

class RpGenGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val extension = project.extensions.create("compileRpGen", RpGenCompileExtension::class.java)

        project.tasks.register("compileRpGen", RpGenCompileTask::class.java) {
            it.manifestFile.set(extension.manifestFile)
        }

        project.getTasksByName("compileKotlin", false).forEach { task ->
            task.dependsOn("compileRpGen")
        }

        project.getTasksByName("compileJava", false).forEach { task ->
            task.dependsOn("compileRpGen")
        }
    }
}