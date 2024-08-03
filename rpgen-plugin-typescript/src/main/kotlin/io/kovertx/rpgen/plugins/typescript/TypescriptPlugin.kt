package io.kovertx.rpgen.plugins.typescript

import io.kovertx.rpgen.plugins.RpGenerator
import io.kovertx.rpgen.plugins.RpPlugin

class TypescriptPlugin : RpPlugin {
    override val languages = listOf(TypescriptLanguage)

    override val generators: List<RpGenerator<*, *>> = listOf(
        TypescriptModelGenerator,
        TypescriptRpcModelGenerator,
        TypescriptJsonCodecGenerator,
        TypescriptFetchJsonCodecGenerator,
    )
}