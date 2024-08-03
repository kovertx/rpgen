package io.kovertx.rpgen.plugins.openapi

import io.kovertx.rpgen.ast.SourceRef
import io.kovertx.rpgen.plugins.RpLanguage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

object OpenApiLanguage : RpLanguage<OpenApiConfig> {
    override val id = "openapi"

    override fun parseLanguageConfig(src: String, parentSource: SourceRef) =
        OpenApiConfig(Json.decodeFromString<JsonArray>(src))
}

data class OpenApiConfig(
    val schemaDiff: JsonArray
)