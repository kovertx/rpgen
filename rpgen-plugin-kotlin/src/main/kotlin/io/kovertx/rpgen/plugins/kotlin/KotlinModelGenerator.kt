package io.kovertx.rpgen.plugins.kotlin

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.util.SmartWriter
import io.kovertx.rpgen.util.smartly
import io.kovertx.rpgen.ast.*

object KotlinModelGenerator : KotlinGeneratorBase<Unit>() {
    override val id: String = "model"

    override fun generate(ctx: IGenerateContext) {
        val config = mergeKotlinConfigs(ctx.schemas, "model")

        val pkg = config.pkg ?: "io.kovertx.rpggen.demo"
        val pkgPath = pkg.replace(".", "/")

        ctx.schemas.models.forEach { model ->
            ctx.output.writeFile("$pkgPath/Maybe.kt") {
                smartly {
                    ln("package $pkg")
                    ln("import kotlinx.serialization.KSerializer")
                    ln("import kotlinx.serialization.Serializable")
                    ln("import kotlinx.serialization.encoding.Decoder")
                    ln("import kotlinx.serialization.encoding.Encoder")
                    ln()
                    ln("@Serializable(with = MaybeSerializer::class)")
                    p("sealed class Maybe<out T> ").curly {
                        ln("abstract val valueOrNull: T?")
                        ln()
                        p("data object Undefined : Maybe<Nothing>() ").curly {
                            ln("override val valueOrNull get() = null")
                        }
                        ln()
                        p("data class Defined<T>(val value: T) : Maybe<T>() ").curly {
                            ln("override val valueOrNull get() = value")
                        }
                    }
                    ln()
                    p("open class MaybeSerializer<T>(private val valueSerializer: KSerializer<T>) : KSerializer<Maybe<T>> ").curly {
                        ln("override val descriptor = valueSerializer.descriptor")
                        ln()
                        ln("override fun deserialize(decoder: Decoder) =")
                        indent { ln("Maybe.Defined(decoder.decodeSerializableValue(valueSerializer))") }
                        ln()
                        p("override fun serialize(encoder: Encoder, value: Maybe<T>) = when (value) ").curly {
                            ln("is Maybe.Undefined -> {}")
                            ln("is Maybe.Defined -> valueSerializer.serialize(encoder, value.value)")
                        }
                    }
                }
            }
            ctx.output.writeFile("${pkgPath}/Generated${model.name}Model.kt") {
                smartly {
                    ln("package $pkg")
                    ln("import kotlinx.serialization.KSerializer")
                    ln("import kotlinx.serialization.Serializable")
                    ln("import kotlinx.serialization.descriptors.PrimitiveKind")
                    ln("import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor")
                    ln("import kotlinx.serialization.encoding.Decoder")
                    ln("import kotlinx.serialization.encoding.Encoder")
                    ln("import java.util.UUID")

                    config.imports.forEach { printImport(it) }
                    config.aliases.forEach { printTypeAlias(it) }

                    printModelTypes(this, model)
                }
            }
        }
    }

    private fun printModelTypes(smartWriter: SmartWriter, model: RpModel) = with (smartWriter) {
        model.types.forEach { type ->
            when (type) {
                is RpExternalTypeDef -> Unit // no-op
                is RpId -> printIdClass(type)
                is RpEnum -> printEnumClass(type)
                is RpStruct -> printStructClass(type)
                is RpOpaque -> printOpaqueType(type)
            }
        }
    }

    private fun SmartWriter.printIdClass(id: RpId) = ln()
        .ln("@Serializable(with = ${id.name}.Companion::class)")
        .p("data class ${id.name}(val value: UUID) ").curly {
            p("companion object : KSerializer<${id.name}> ").curly {
                ln("override val descriptor = PrimitiveSerialDescriptor(\"${id.name}\", PrimitiveKind.STRING)")

                if (id.tags.contains("base64")) {
                    ln("override fun deserialize(decoder: Decoder) = ${id.name}(UUIDUtils.decodeB64(decoder.decodeString()))")
                    ln("override fun serialize(encoder: Encoder, value: ${id.name}) = encoder.encodeString(UUIDUtils.encodeB64(value.value))")
                } else {
                    ln("override fun deserialize(decoder: Decoder) = ${id.name}(UUID.fromString(decoder.decodeString()))")
                    ln("override fun serialize(encoder: Encoder, value: ${id.name}) = encoder.encodeString(value.value.toString())")
                }
            }
        }

    private fun SmartWriter.printEnumClass(enum: RpEnum) = ln()
        .ln("@Serializable(with = ${enum.name}.Companion::class)")
        .p("enum class ${enum.name}(val value: Int) ").curly {
            enum.values.forEach { enumValue ->
                ln("${enumValue.name}(${enumValue.value}),")
            }
            ln(";")
            p("companion object : KSerializer<${enum.name}> ").curly {
                ln("override val descriptor = PrimitiveSerialDescriptor(\"${enum.name}\", PrimitiveKind.STRING)")
                ln("override fun serialize(encoder: Encoder, value: ${enum.name}) = encoder.encodeInt(value.value)")
                ln("override fun deserialize(decoder: Decoder) = fromValue(decoder.decodeInt())")
                p("@JvmStatic fun fromValue(value: Int): ${enum.name} ").curly {
                    p("return when (value) ").curly {
                        enum.values.forEach { ln("${it.value} -> ${it.name}") }
                        ln("else -> throw IllegalArgumentException()")
                    }
                }
            }
        }

    private fun SmartWriter.printStructClass(struct: RpStruct) = ln()
        .ln("@Serializable")
        .p("data class ${struct.name} ").parens {
            struct.fields.map {
                p("var ").printField(it)
                ln(",") }
        }

    private fun SmartWriter.printOpaqueType(opaque: RpOpaque): SmartWriter {
        val builtin = builtinTypeSpecs[opaque.primitive] ?: throw RuntimeException()
        if (builtin.nParams > 0) throw RuntimeException()

        ln()
        ln("@Serializable(with = ${opaque.name}.Companion::class)")
        p("data class ${opaque.name}(val wrapped: ${builtin.kotlinType}) ").curly {
            p("companion object : KSerializer<${opaque.name}> ").curly {
                ln("override val descriptor = PrimitiveSerialDescriptor(\"${opaque.name}\", PrimitiveKind.${builtin.kotlinType.uppercase()})")
                ln("override fun deserialize(decoder: Decoder) = ${opaque.name}(decoder.decode${builtin.kotlinType}())")
                ln("override fun serialize(encoder: Encoder, value: ${opaque.name}) = encoder.encode${builtin.kotlinType}(value.wrapped)")
            }
        }
        return this;
    }
}