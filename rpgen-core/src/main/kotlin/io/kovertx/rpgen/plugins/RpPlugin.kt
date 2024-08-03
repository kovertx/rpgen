package io.kovertx.rpgen.plugins

interface RpPlugin {
    val languages: List<RpLanguage<*>> get() = emptyList()
    val generators: List<RpGenerator<*, *>> get() = emptyList()
}

