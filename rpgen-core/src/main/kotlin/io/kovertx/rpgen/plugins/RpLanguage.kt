package io.kovertx.rpgen.plugins

import io.kovertx.rpgen.IValidateContext
import io.kovertx.rpgen.ast.SourceRef

interface RpLanguage<TConfig : Any> {
    val id: String
    fun parseLanguageConfig(src: String, parentSource: SourceRef): TConfig

    fun validate(ctx: IValidateContext) { /* default no-op */ }
}