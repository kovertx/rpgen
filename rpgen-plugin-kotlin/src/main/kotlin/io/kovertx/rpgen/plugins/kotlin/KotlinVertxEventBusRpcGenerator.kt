package io.kovertx.rpgen.plugins.kotlin

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.ast.*
import io.kovertx.rpgen.util.SmartWriter
import io.kovertx.rpgen.util.smartly

object KotlinVertxEventBusRpcGenerator : KotlinGeneratorBase<Unit>() {
    override val id = "vertx-event-bus-rpc"

    override fun generate(ctx: IGenerateContext) {
        val config = mergeKotlinConfigs(ctx.schemas, "rpc")
        val pkg = (config.pkg ?: "io.kovertx.rpgen.demo")
        val pkgPath = pkg.replace(Regex("\\s*\\.\\s*"), "/")

        val modelPkg = mergeKotlinConfigs(ctx.schemas, "model")
            .pkg ?: "io.kovertx.rpggen.demo"

        ctx.schemas.rpcs.forEach { rpc ->
            ctx.output.writeFile("${pkgPath}/Generated${rpc.name}EventBusRpc.kt") {
                smartly {
                    ln("package ${pkg}")
                    ln("import io.vertx.core.Future")
                    ln("import io.vertx.core.eventbus.EventBus")
                    ln("import io.kovertx.eventbus.registerKotlinxSerializationCodec")
                    ln("import ${modelPkg}.*")
                    printApiInterface(rpc)
                    printEventBusExtension(rpc)
                    printEventBusClient(rpc)
                }
            }
        }
    }

    private fun SmartWriter.printApiInterface(rpc: RpRpc) {
        ln()
        p("interface ${rpc.nameUpperCamelCase}EventBusApi ").curly {
            rpc.terminalMethods.forEach { method ->
                printApiMethod(method)
            }
        }
    }

    private fun SmartWriter.printApiMethod(method: RpRpcMethod) {
        ln().p("fun ${method.nameCamelCase}(")
        method.args.forEach { arg ->
            printField(arg).p(",")
        }
        p("): ")
        when (method) {
            is RpRpcQuery -> p("Future<").printTypeRef(method.returnType).ln(">")
            is RpRpcCommand -> ln("Unit")
            is RpRpcNotice -> ln("Unit")
            else -> TODO("Unsupported method type")
        }
    }

    private fun SmartWriter.printEventBusExtension(rpc: RpRpc) {
        ln()
        p("fun EventBus.register${rpc.nameUpperCamelCase}RpcCodecs() ").curly {
            rpc.terminalMethods.forEach { method ->
                ln("registerKotlinxSerializationCodec<${method.nameUpperCamelCase}>()")
                if (method is RpRpcQuery) {
                    ln("registerKotlinxSerializationCodec<${method.nameUpperCamelCase}Response>()")
                }
            }
        }
        ln()
        p("fun EventBus.consume${rpc.nameUpperCamelCase}Rpc(address: String = \"${rpc.name}\", api: ${rpc.name}EventBusApi) ").curly {
            ln("consumer<I${rpc.nameUpperCamelCase}ClientMessage>(address) { message ->")
            indent {
                ln("val body = message.body()")
                p("when (body) ").curly {
                    rpc.terminalMethods.forEach { method ->
                        p("is ${method.nameUpperCamelCase} -> ").curly {
                            printMethodHandlerBody(method)
                        }
                    }
                }
            }
            ln("}")
        }
    }

    private fun SmartWriter.printMethodHandlerBody(method: RpRpcMethod) {
        when (method) {
            is RpRpcQuery -> {
                p("api.${method.name}(")
                method.args.forEach { p("body.${it.name}, ") }
                ln(")")
                indent {
                    ln(".onSuccess { message.reply(body.wrapResponse(it)) }")
                    ln(".onFailure { message.fail(500, it.message) }")
                }
            }
            else -> TODO()
        }
    }

    private fun SmartWriter.printEventBusClient(rpc: RpRpc) {
        ln()
        ln("class GeneratedNutritionSearchEventBusClient(")
        indent {
            ln("private val bus: EventBus,")
            ln("private val address: String = \"${rpc.name}\"")
        }
        p(") : ${rpc.nameUpperCamelCase}EventBusApi ").curly {
            rpc.terminalMethods.forEach { method ->
                printClientMethod(method)
            }
        }
    }

    private fun SmartWriter.printClientMethod(method: RpRpcMethod) {
        ln()
        p("override fun ${method.nameCamelCase}(")
        method.args.forEachIndexed { i, field ->
            if (i > 0) p(", ")
            printField(field)
        }
        p("): ")
        when (method) {
            is RpRpcQuery -> {
                p("Future<").printTypeRef(method.returnType).ln("> = ")
                indent {
                    p("bus.request<${method.nameUpperCamelCase}Response>(address, ")
                    p("${method.nameUpperCamelCase}(")
                    method.args.forEachIndexed { i, field ->
                        if (i > 0) p(", ")
                        p(field.name)
                    }
                    ln(")).map { it.body().value }")
                }
            }
            else -> TODO()
        }
    }
}