package io.kovertx.rpgen.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.kovertx.rpgen.compiler.RpGenCompiler
import io.kovertx.rpgen.config.CompilerOptions
import io.kovertx.rpgen.plugins.PluginManager

abstract class BaseCompileCommand : CliktCommand() {
    abstract val debug: Boolean
    val manifestFile by option("-m","--manifest").path(true).required()

    override fun run() {
        RpGenCompiler.fromConfigFile(manifestFile, CompilerOptions(
            debug = debug
        )).compile()
    }
}

object CompileCommand : BaseCompileCommand() {
    override val debug: Boolean = false
}

object DebugCommand : BaseCompileCommand() {
    override val debug: Boolean = true
}

object ListCommand : CliktCommand() {
    override fun run() {
        println("--- listing all loaded languages/generators ---")
        PluginManager.generators.toList()
            .groupBy { it.first.lang }
            .forEach {
                val lang = it.key
                it.value.map { it.second.id }.forEach { generatorId ->
                    println("$lang:$generatorId")
                }
            }
    }
}

object RpGenApp : NoOpCliktCommand()

fun main(args: Array<String>) = RpGenApp.subcommands(
    CompileCommand,
    DebugCommand,
    ListCommand
).main(args)
