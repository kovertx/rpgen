package io.kovertx.rpgen.plugins.kotlin

import io.kovertx.rpgen.plugins.RpGenerator
import io.kovertx.rpgen.plugins.RpLanguage
import io.kovertx.rpgen.plugins.RpPlugin
import io.kovertx.rpgen.plugins.kotlin.kovertx.KotlinKovertxWebGenerator

class KotlinPlugin : RpPlugin {
    override val languages: List<RpLanguage<*>> = listOf(KotlinLanguage)
    override val generators: List<RpGenerator<*, *>> = listOf(
        KotlinModelGenerator,
        KotlinRpcModelGenerator,
        KotlinVertxWebGenerator,
        KotlinVertxEventBusRpcGenerator,
        KotlinKovertxWebGenerator
    )
}
