package io.kovertx.rpgen.plugins.openapi

import io.kovertx.rpgen.plugins.RpPlugin

class OpenApiPlugin : RpPlugin {
    override val languages get() = listOf(OpenApiLanguage)
    override val generators get() = listOf(OpenApiSchemaGenerator)
}