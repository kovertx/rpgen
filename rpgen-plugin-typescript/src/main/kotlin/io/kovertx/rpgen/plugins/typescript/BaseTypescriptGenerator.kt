package io.kovertx.rpgen.plugins.typescript

import io.kovertx.rpgen.ast.RpField
import io.kovertx.rpgen.ast.RpSchemas
import io.kovertx.rpgen.ast.RpTypeRef
import io.kovertx.rpgen.plugins.RpGenerator
import io.kovertx.rpgen.util.SmartWriter

abstract class BaseTypescriptGenerator<TConf : Any> : RpGenerator<TypescriptConfig, TConf> {
    override val language = TypescriptLanguage

    protected fun SmartWriter.blockComment(handler: SmartWriter.() -> Unit) =
        ln("/**").prefix(" * ", handler).ln(" */")

    protected fun SmartWriter.printField(field: RpField) = p(field.name)
        .pIf(field.isOptional) {
            p("?")
        }.p(": ")
        .printTypeRef(field.type)

    protected fun SmartWriter.printTypeRef(type: RpTypeRef): SmartWriter {
        when (type.type) {
            "str" -> p("string")
            "bool" -> p("boolean")
            "u8", "u16", "u32",
            "i8", "i16", "i32",
            "f32", "f64" -> p("number")
            "u64", "i64" -> p("bigint")
            "List" -> p("Array<").printTypeRef(type.params[0]).p(">")
            else -> p(type.type)
        }
        if (type.isNullable) p(" | null")
        return this
    }

    protected fun SmartWriter.printImport(import: TypescriptImport) {
        p("import type { ")
        import.types.forEachIndexed { i, typ ->
            if (i > 0) p(", ")
            p(typ.name)
            val alias = typ.alias
            if (alias != null) p(" as ").p(alias)
        }
        ln(" } from ${import.src};")
    }

    protected fun SmartWriter.printAlias(alias: TypescriptAlias) {
        p("type ${alias.name} = ").printTypeRef(alias.from).ln()
    }

    protected fun getTypescriptConfigs(
        schemas: RpSchemas,
        scope: String
    ): List<TypescriptConfig> {
        val collected = mutableListOf<TypescriptConfig>()
        schemas.configs.forEach { config ->
            if (config.data !is TypescriptConfig) return@forEach
            if (config.scope != null && config.scope != scope) return@forEach
            if (config.generator != null && config.generator != id) return@forEach
            collected.add(config.data as TypescriptConfig)
        }
        return collected
    }

    protected fun mergeKotlinConfigs(
        schemas: RpSchemas,
        scope: String
    ) : TypescriptConfig = getTypescriptConfigs(schemas, scope)
        .fold(TypescriptConfig(emptyList(), emptyList())) { a, b -> TypescriptConfig(
            imports = a.imports.plus(b.imports),
            aliases = a.aliases.plus(b.aliases))
        }
}