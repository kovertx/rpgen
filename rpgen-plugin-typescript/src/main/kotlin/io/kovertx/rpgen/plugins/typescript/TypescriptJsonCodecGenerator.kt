package io.kovertx.rpgen.plugins.typescript

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.util.SmartWriter
import io.kovertx.rpgen.util.smartly
import io.kovertx.rpgen.ast.*

object TypescriptJsonCodecGenerator : BaseTypescriptGenerator<Unit>() {
    override val id = "json-codec"

    override fun generate(ctx: IGenerateContext) {

        val config = getTypescriptConfigs(ctx.schemas, "model")

        ctx.schemas.models.forEach { model ->
            ctx.output.writeFile("generated${model.name}ModelJsonCodec.ts") {
                smartly {
                    ln("import { Codecs, JsonCodec } from \"@kovertx/typescript-json-codec\";")
                    p("import { ")
                    model.types.forEachIndexed { i, type ->
                        if (i > 0) p(", ")
                        if (type !is RpEnum) p("type ")
                        p(type.name)
                    }
                    ln(" } from \"./generated${model.name}Model\";")

                    printExternCodecsType(model)
                    printModelCodecsType(model)
                    printModelCodecFactory(model)
                }
            }
        }
    }

    private fun SmartWriter.printExternCodecsType(model: RpModel) {
        val hasExterns = model.types.any { it is RpExternalTypeDef }
        if (!hasExterns) return

        ln()
        blockComment {
            ln("Container for JsonCodec implementations for extern types defined in ${model.name} model schema.")
        }
        p("export interface ${model.name}ExternCodecs ").curly {
            model.types.filterIsInstance<RpExternalTypeDef>().forEach { extern ->
                p("readonly ").p(extern.name).p(": JsonCodec<").p(extern.name).ln(">")
            }
        }
    }

    private fun SmartWriter.printModelCodecsType(model: RpModel) {
        ln()
        p("export interface ${model.name}ModelJsonCodecs ")
        val hasExterns = model.types.any { it is RpExternalTypeDef }
        if (hasExterns) p("extends ${model.name}ExternCodecs ")
        curly {
            model.types.forEach { type ->
                when (type) {
                    is RpExternalTypeDef -> return@forEach
                    else -> p("readonly ").p(type.name).p(": JsonCodec<").p(type.name).ln(">")
                }
            }
        }
    }

    private fun SmartWriter.printModelCodecFactory(model: RpModel) {
        val hasExterns = model.types.any { it is RpExternalTypeDef }
        ln()
        blockComment {
            ln("Creates an object containing JsonCodec objects for types defined in ${model.name} model schema.")
            if (hasExterns) ln("@param externs codec implementations for declared extern types")
        }
        p("export function make${model.name}JsonCodecs(")
        if (hasExterns) p("externs: ${model.name}ExternCodecs")
        p("): ${model.name}ModelJsonCodecs ").curly {
            model.types.forEach { type ->
                p("const _${type.name} =")
                when (type) {
                    is RpExternalTypeDef -> ln("externs.${type.name};")
                    is RpId -> ln("Codecs.string.asserting((_s): asserts _s is ${type.name} => {});")
                    is RpEnum -> ln("Codecs.enum(${type.name});")
                    is RpStruct -> {
                        printDeclareStructCodec(type)
                    }
                    is RpOpaque -> when (type.primitive) {
                        "i8", "i16", "i32",
                        "u8", "u16", "u32",
                        "f32", "f64" -> p("Codecs.number")
                        "bool" -> p("Codecs.boolean")
                        "str" -> p("Codecs.string")
                        else -> throw IllegalArgumentException("Unhandled opaque primitive type: ${type.primitive}")
                    }.ln(".asserting((_x): asserts _x is ${type.name} => {})")
                }
            }
            ln()
            p("return ").curly {
                model.types.forEach { type ->
                    ln("${type.name}: _${type.name},")
                }
            }
        }
    }

    private fun SmartWriter.printDeclareStructCodec(struct: RpStruct) {
        ln("Codecs.object<${struct.name}>({").indent {
            struct.fields.forEach { field ->
                p(field.name).p(": ").printCodecForType(field.type)
                if (field.isOptional) p(".optional()")
                ln(",")
            }
        }.ln("});")
    }

    private fun SmartWriter.printCodecForType(type: RpTypeRef): SmartWriter {
        when (type.type) {
            "str" -> p("Codecs.string")
            "bool" -> p("Codecs.boolean")
            "u8", "u16", "u32",
            "i8", "i16", "i32",
            "f32", "f64" -> p("Codecs.number")
            "u64", "i64" -> p("Codecs.bigint")
            "List" -> p("Codecs.array(").printCodecForType(type.params[0]).p(")")
            else -> p("Codecs.lazy(() => _${type.type})")
        }
        if (type.isNullable) p(".nullable")
        return this
    }
}