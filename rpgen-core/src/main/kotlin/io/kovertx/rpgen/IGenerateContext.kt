package io.kovertx.rpgen

import io.kovertx.rpgen.ast.RpSchemas
import io.kovertx.rpgen.output.IGeneratorOutput
import java.nio.file.Path

interface IGenerateContext {
    val schemas: RpSchemas
    val output: IGeneratorOutput
}