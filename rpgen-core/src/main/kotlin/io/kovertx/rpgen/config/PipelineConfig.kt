package io.kovertx.rpgen.config
import kotlinx.serialization.Serializable

@Serializable
data class PipelineConfig(
    val outDir: String,
    val generators: List<String>,
)