package io.kovertx.rpgen.ast

/**
 * A parsed configuration block
 * @property languageId the language id associated with this configuration block
 * @property generator the generator id associated with this configuration block (if specified)
 * @property scope the scope associated with this configuration block (if specified)
 * @property data the parsed configuration from this block
 */
data class RpConfig<T>(
    val languageId: String,
    val generator: String?,
    val scope: String?,
    val data: T,
    override val parsedFrom: SourceRef
) : RpAstNode
