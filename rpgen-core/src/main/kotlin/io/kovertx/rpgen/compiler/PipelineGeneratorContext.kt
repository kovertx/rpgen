package io.kovertx.rpgen.compiler

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.output.IGeneratorOutput
import io.kovertx.rpgen.ast.RpSchemas
import java.nio.file.Path

class PipelineGeneratorContext(
    override val schemas: RpSchemas,
    override val output: IGeneratorOutput
) : IGenerateContext