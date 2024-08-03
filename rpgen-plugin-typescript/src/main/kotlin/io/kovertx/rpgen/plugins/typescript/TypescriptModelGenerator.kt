package io.kovertx.rpgen.plugins.typescript

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.util.SmartWriter
import io.kovertx.rpgen.util.smartly
import io.kovertx.rpgen.ast.*

object TypescriptModelGenerator : BaseTypescriptGenerator<Unit>() {
    override val id: String = "model"

    override fun generate(ctx: IGenerateContext) {
        ctx.schemas.models.forEach { model ->
            val config = mergeKotlinConfigs(ctx.schemas, "model")

            ctx.output.writeFile("generated${model.name}Model.ts") {
                smartly {
                    config.imports.forEach { import -> printImport(import) }
                    config.aliases.forEach { alias -> printAlias(alias) }
                    printModel(model)
                }
            }
        }
    }

    private fun SmartWriter.printModel(model: RpModel) {
        val externs = model.types.filterIsInstance<RpExternalTypeDef>()
        if (externs.isNotEmpty()) {
            p("export type { ")
            externs.forEachIndexed { i, extern ->
                if (i > 0) p(", ")
                p(extern.name)
            }
            ln(" };")
        }
        model.types.forEach { type ->
            when (type) {
                is RpExternalTypeDef -> {}
                is RpId -> printIdType(type)
                is RpEnum -> printEnumType(type)
                is RpStruct -> printStructType(type)
                is RpOpaque -> printOpaqueType(type)
            }
        }
    }

    private fun SmartWriter.printIdType(id: RpId) {
        ln()
        ln("export declare const _is${id.nameUpperCamelCase}: unique symbol;")
        ln()
        ln("export type ${id.name} = string & { [_is${id.nameUpperCamelCase}]: true }");
    }

    private fun SmartWriter.printOpaqueType(opaque: RpOpaque) {
        ln()
        ln("export declare const _is${opaque.name}: unique symbol;")
        p("export type ${opaque.name} = ")
        when (opaque.primitive) {
            "str" -> p("string")
            "bool" -> p("boolean")
            "i8", "i16", "i32",
            "u8", "u16", "u32",
            "f32", "f64" -> p("number")
            "i64", "u64" -> p("bigint")
        }
        ln(" & { [_is${opaque.name}]: true };")
    }

    private fun SmartWriter.printEnumType(enum: RpEnum) {
        ln()
        p("export enum ${enum.name} ").curly {
            enum.values.forEach { value ->
                ln("${value.name} = ${value.value},")
            }
        }
    }

    private fun SmartWriter.printStructType(struct: RpStruct) {
        ln()
        p("export interface ${struct.name} ").curly {
            struct.fields.forEach { field ->
                printField(field).ln(",")
            }
        }
    }
}