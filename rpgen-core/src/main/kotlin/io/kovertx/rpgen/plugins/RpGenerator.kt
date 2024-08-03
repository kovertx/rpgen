package io.kovertx.rpgen.plugins

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.IValidateContext
import io.kovertx.rpgen.ast.SourceRef

interface RpGenerator<TLangConfig : Any, TGenConfig : Any> {
    val id: String
    val language: RpLanguage<TLangConfig>
    val summary get(): String = "no summary"

    /**
     * Generators are allowed to define a custom configuration type
     */
    fun parseGeneratorConfig(src: String, parentSource: SourceRef): TGenConfig = throw UnsupportedOperationException(
        "No generator-specific configuration type defined for ${language.id}@${id}")

    fun validate(ctx: IValidateContext) { /* default no-op */ }
    fun generate(ctx: IGenerateContext)
}