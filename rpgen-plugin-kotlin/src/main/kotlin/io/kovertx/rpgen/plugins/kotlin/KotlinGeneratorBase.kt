package io.kovertx.rpgen.plugins.kotlin

import io.kovertx.rpgen.ast.RpField
import io.kovertx.rpgen.ast.RpSchemas
import io.kovertx.rpgen.ast.RpTypeRef
import io.kovertx.rpgen.plugins.RpGenerator
import io.kovertx.rpgen.plugins.RpLanguage
import io.kovertx.rpgen.util.SmartWriter

abstract class KotlinGeneratorBase<TConf : Any> : RpGenerator<KotlinConfig, TConf> {
    override val language: RpLanguage<KotlinConfig> = KotlinLanguage

    protected fun SmartWriter.blockComment(handler: SmartWriter.() -> Unit) =
        ln("/**").prefix(" * ", handler).ln(" */")

    protected fun SmartWriter.printField(field: RpField): SmartWriter {
        p(field.name).p(": ")

        if (field.isOptional) {
            if (field.type.isNullable) {
                p("Maybe<").printTypeRef(field.type).p("> = Maybe.Undefined")
            } else {
                printTypeRef(field.type).p("? = null")
            }
        } else {
            printTypeRef(field.type)
        }
        return this
    }

    protected val builtinTypeSpecs = mapOf(
        "bool" to BuiltinTypeSpec("Boolean"),
        "str" to BuiltinTypeSpec("String"),
        "i16" to BuiltinTypeSpec("Short"),
        "i32" to BuiltinTypeSpec("Int"),
        "i64" to BuiltinTypeSpec("Long"),
        "f32" to BuiltinTypeSpec("Float"),
        "f64" to BuiltinTypeSpec("Double"),
        "List" to BuiltinTypeSpec("List", 1)
    )

    protected data class BuiltinTypeSpec(
        val kotlinType: String,
        val nParams: Int = 0)

    protected fun SmartWriter.printTypeRef(ref: RpTypeRef): SmartWriter {
        val builtin = builtinTypeSpecs[ref.type]
        if (builtin == null) {
            p(ref.type)
        } else {
            p(builtin.kotlinType)
            if (builtin.nParams != ref.params.size) {
                throw IllegalArgumentException("Unexpected number of type parameters for ${ref.type}")
            }
            if (ref.params.isNotEmpty()) {
                p("<")
                ref.params.forEachIndexed { i, param ->
                    if (i > 0) p(", ")
                    printTypeRef(param)
                }
                p(">")
            }
        }
        if (ref.isNullable) p("?")
        return this
    }

    protected fun getKotlinConfigs(
        schemas: RpSchemas,
        scope: String
    ): List<KotlinConfig> {
        val collected = mutableListOf<KotlinConfig>()
        schemas.configs.forEach { config ->
            if (config.data !is KotlinConfig) return@forEach
            if (config.scope != null && config.scope != scope) return@forEach
            if (config.generator != null && config.generator != id) return@forEach
            collected.add(config.data as KotlinConfig)
        }
        return collected
    }

    protected fun mergeKotlinConfigs(
        schemas: RpSchemas,
        scope: String
    ) : KotlinConfig = getKotlinConfigs(schemas, scope)
        .fold(KotlinConfig(null, emptyList())) { a, b -> KotlinConfig(
            pkg = b.pkg ?: a.pkg,
            imports = a.imports.plus(b.imports))
        }

    protected fun SmartWriter.printImport(import: KotlinImport): SmartWriter {
        p("import ${import.klassName}")
        if (import.alias != null) p(" as ${import.alias}")
        return ln()
    }
}