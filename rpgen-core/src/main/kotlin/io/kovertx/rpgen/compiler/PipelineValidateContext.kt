package io.kovertx.rpgen.compiler

import io.kovertx.rpgen.IValidateContext
import io.kovertx.rpgen.ast.RpSchemas

class PipelineValidateContext(
    override val schemas: RpSchemas
) : IValidateContext {

    val warnings = mutableListOf<String>()
    val errors = mutableListOf<String>()

    override fun emitWarning(message: String) {
        warnings.add(message)
    }

    override fun emitError(message: String) {
        errors.add(message)
    }
}