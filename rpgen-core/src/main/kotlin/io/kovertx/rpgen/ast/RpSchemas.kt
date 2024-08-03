package io.kovertx.rpgen.ast

/**
 * Container for parsed schema data
 */
data class RpSchemas(
    val configs: List<RpConfig<*>>,
    val models: List<RpModel>,
    val rpcs: List<RpRpc>,
    val https: List<RpHttp>
)