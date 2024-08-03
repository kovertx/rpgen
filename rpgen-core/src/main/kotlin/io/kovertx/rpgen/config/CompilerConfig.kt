package io.kovertx.rpgen.config

import kotlinx.serialization.Serializable

@Serializable
data class CompilerConfig(
    val schemas: List<String>,
    val pipelines: List<PipelineConfig>
)

