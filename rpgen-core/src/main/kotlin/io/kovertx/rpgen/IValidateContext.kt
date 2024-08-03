package io.kovertx.rpgen

import io.kovertx.rpgen.ast.RpSchemas

interface IValidateContext {
    val schemas: RpSchemas
    fun emitWarning(message: String)
    fun emitError(message: String)
}
