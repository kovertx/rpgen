package io.kovertx.rpgen.ast

import io.kovertx.rpgen.util.IdUtils

data class SourceRef(val doc: String, val line: Int, val col: Int) {
    override fun toString() = "[${doc} @ ${line}:${col}]"
}

sealed interface RpNamedNode {
    val name: String

    val nameCamelCase get() = IdUtils.toCamelCase(name)
    val nameUpperCamelCase get() = IdUtils.toUpperCamelCase(name)
    val nameSnakeCase get() = IdUtils.toSnakeCase(name)
    val nameKebabCase get() = IdUtils.toKebabCase(name)
}

sealed interface RpAstNode {
    val parsedFrom: SourceRef

    val childAstNodes get(): Sequence<RpAstNode> = emptySequence()

    fun collectTypeDependencies(results: MutableSet<RpTypeDependency>) {
        for (node in childAstNodes) {
            node.collectTypeDependencies(results)
        }
    }
}

data class RpTypeDependency(
    val name: String,
    val isBuiltin: Boolean,
    val nTypeParams: Int
)