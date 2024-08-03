package io.kovertx.rpgen.plugins

import java.util.ServiceLoader
import kotlin.IllegalArgumentException

object PluginManager {

    val languages: Map<String, RpLanguage<*>>
    val generators: Map<GeneratorKey, RpGenerator<*, *>>

    init {
        val langs = mutableListOf<RpLanguage<*>>()
        val gens = mutableListOf<RpGenerator<*, *>>()

        ServiceLoader.load(RpPlugin::class.java).forEach { plugin ->
            langs.addAll(plugin.languages)
            gens.addAll(plugin.generators)
        }

        languages = langs.associateBy { it.id }
        generators = gens.associateBy { GeneratorKey(it.language.id, it.id) }
    }

    fun getLanguage(lang: String) = languages.get(lang) ?: throw IllegalArgumentException("Unknown language: $lang")
    fun getGenerator(lang: String, gen: String) = generators.get(GeneratorKey(lang, gen)) ?: throw IllegalArgumentException("Unknown generator: $lang@$gen")
}

data class GeneratorKey(val lang: String, val gen: String)