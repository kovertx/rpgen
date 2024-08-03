package io.kovertx.rpgen.compiler

import io.kovertx.rpgen.output.IGeneratorOutput
import io.kovertx.rpgen.ast.RpSchemas
import io.kovertx.rpgen.plugins.RpGenerator

class Pipeline(
    val generators: List<RpGenerator<*, *>>,
    val output: IGeneratorOutput
) {
    fun validate(schemas: RpSchemas) {
        generators.forEach { it.validate(PipelineValidateContext(schemas)) }
    }

    fun generate(schemas: RpSchemas) {
        generators.forEach { it.generate(PipelineGeneratorContext(schemas, output)) }
    }
}