package io.kovertx.rpgen.compiler

import io.kovertx.rpgen.ast.RpSchemas
import io.kovertx.rpgen.compiler.output.DebugOutput
import io.kovertx.rpgen.compiler.output.DirectoryOutput
import io.kovertx.rpgen.config.CompilerConfig
import io.kovertx.rpgen.config.CompilerOptions
import io.kovertx.rpgen.parser.SchemaParser
import io.kovertx.rpgen.plugins.PluginManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import kotlin.io.path.readText

class RpGenCompiler(
    private val schemas: List<Path>,
    private val pipelines: List<Pipeline>
) {
    fun compile() {
        val mergedSchemas = schemas.map { schemaPath -> SchemaParser.parse(schemaPath.readText()) }
            .reduce { a, b -> RpSchemas(
                configs = a.configs.plus(b.configs),
                models = a.models.plus(b.models),
                https = a.https.plus(b.https),
                rpcs = a.rpcs.plus(b.rpcs)
            ) }
        pipelines.forEach { pipeline ->
            pipeline.validate(mergedSchemas)
        }
        pipelines.forEach { pipeline ->
            pipeline.generate(mergedSchemas)
        }
    }

    companion object {
        fun fromConfigFile(configFile: Path, options: CompilerOptions = CompilerOptions()): RpGenCompiler {
            val config = configFile.inputStream()
                .use { Json.decodeFromStream<CompilerConfig>(it) }
            val schemas = config.schemas.map {
                configFile.resolveSibling(it)
            }
            val pipelines = config.pipelines.map { pipelineConfig ->
                val outDir = configFile.parent.absolute()
                    .resolve(pipelineConfig.outDir)
                    .normalize()
                    .absolutePathString()
                val output = if (options.debug) DebugOutput(outDir) else DirectoryOutput(outDir)
                val generators = pipelineConfig.generators.map {
                    val (langId, genId) = it.split('@')
                    PluginManager.getGenerator(langId, genId)
                }
                Pipeline(output = output, generators = generators)
            }
            return RpGenCompiler(schemas, pipelines)
        }
    }
}
