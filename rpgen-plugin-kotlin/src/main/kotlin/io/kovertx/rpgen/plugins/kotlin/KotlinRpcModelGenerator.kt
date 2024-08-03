package io.kovertx.rpgen.plugins.kotlin

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.util.SmartWriter
import io.kovertx.rpgen.util.smartly
import io.kovertx.rpgen.ast.*

object KotlinRpcModelGenerator : KotlinGeneratorBase<Unit>() {
    override val id: String = "rpc-model"

    override fun generate(ctx: IGenerateContext) {
        val config = mergeKotlinConfigs(ctx.schemas, "rpc")
        val pkg = (config.pkg ?: "io.kovertx.rpgen.demo")
        val pkgPath = pkg.replace(Regex("\\s*\\.\\s*"), "/")

        val modelPkg = mergeKotlinConfigs(ctx.schemas, "model")
            .pkg ?: "com.pdehaan.rpggen.demo"

        ctx.schemas.rpcs.forEach { rpc ->
            ctx.output.writeFile("${pkgPath}/Generated${rpc.nameUpperCamelCase}RpcModel.kt") {
                smartly {
                    ln("package ${pkg}")
                    ln("import ${modelPkg}.*")
                    printRpcModelTypes(rpc)
                }
            }
        }
    }

    private fun SmartWriter.printRpcModelTypes(rpc: RpRpc, parents: List<RpRpcGroup> = emptyList()) {

        ln()
        ln("sealed interface I${rpc.nameUpperCamelCase}Message")
        ln("sealed interface I${rpc.nameUpperCamelCase}ClientMessage : I${rpc.nameUpperCamelCase}Message")
        ln("sealed interface I${rpc.nameUpperCamelCase}Command : I${rpc.nameUpperCamelCase}ClientMessage")
        p("sealed interface I${rpc.nameUpperCamelCase}Query<T> : I${rpc.nameUpperCamelCase}ClientMessage ").curly {
            ln("fun wrapResponse(value: T): I${rpc.nameUpperCamelCase}QueryResponse<T>")
        }
        ln("sealed interface I${rpc.nameUpperCamelCase}ServerMessage : I${rpc.nameUpperCamelCase}Message")
        ln("sealed interface I${rpc.nameUpperCamelCase}Notice : I${rpc.nameUpperCamelCase}ServerMessage")
        ln("sealed interface I${rpc.nameUpperCamelCase}QueryResponse<T> : I${rpc.nameUpperCamelCase}ServerMessage ").curly {
            ln("val value: T")
        }

        rpc.methods.forEach { method -> printRpcMethod(method, rpc) }
    }

    private fun SmartWriter.printRpcMethod(
        method: RpRpcMethod, rpc: RpRpc, parents: List<RpRpcGroup> = emptyList()
    ) = when (method) {
        is RpRpcGroup -> printRpcGroup(method, rpc, parents)
        is RpRpcCommand -> printRpcCommand(method, rpc, parents)
        is RpRpcQuery -> printRpcQuery(method, rpc, parents)
        is RpRpcNotice -> TODO()
    }

    private fun SmartWriter.printRpcGroup(
        group: RpRpcGroup, rpc: RpRpc, parents: List<RpRpcGroup>): SmartWriter = ln()
            .p("sealed interface I${rpc.nameUpperCamelCase}${group.nameUpperCamelCase}Message : ")
            .printBasePrefix(rpc, parents).p("Message ").curly {
                group.args.forEach { field ->
                    p("var ").printField(field).ln()
                }
            }
            .ln().p("sealed interface I${rpc.nameUpperCamelCase}${group.nameUpperCamelCase}Command : ")
            .printBasePrefix(rpc, parents).ln("Command : I${rpc.name}${group.nameUpperCamelCase}Message")
            .ln().p("sealed interface I${rpc.nameUpperCamelCase}${group.nameUpperCamelCase}Query<T> : ")
            .printBasePrefix(rpc, parents).ln("Query<T> : I${rpc.name}${group.nameUpperCamelCase}Message")
            .ln().p("sealed interface I${rpc.nameUpperCamelCase}${group.nameUpperCamelCase}Notice : ")
            .printBasePrefix(rpc, parents).ln("Notice : I${rpc.name}${group.nameUpperCamelCase}Message")
            .ln().p("sealed interface I${rpc.nameUpperCamelCase}${group.nameUpperCamelCase}QueryResponse<T> : ")
            .printBasePrefix(rpc, parents).ln("QueryResponse<T> : I${rpc.nameUpperCamelCase}${group.nameUpperCamelCase}Message")

    private fun SmartWriter.printRpcCommand(
        command: RpRpcCommand, rpc: RpRpc, parents: List<RpRpcGroup>) = ln()
            .ln("data class ${command.nameUpperCamelCase}(")
            .printBasePrefix(rpc, parents).p("Command ").parens {
                printBaseFields(parents)
                command.args.forEach { field ->
                    p("var ").printField(field)
                }
        }

    private fun SmartWriter.printRpcQuery(
        query: RpRpcQuery, rpc: RpRpc, parents: List<RpRpcGroup>) = ln()
        .ln("data class ${query.nameUpperCamelCase}(")
        .indent {
            printBaseFields(parents)
            query.args.forEach { field ->
                p("var ").printField(field).ln(",")
            }
        }.p("): ").printBasePrefix(rpc, parents).p("Query<").printTypeRef(query.returnType).p("> ").curly {
            p("override fun wrapResponse(value: ")
            printTypeRef(query.returnType)
            p(") = ${query.nameUpperCamelCase}Response(value)")
        }
        .ln()
        .p("data class ${query.nameUpperCamelCase}Response(override val value: ")
        .printTypeRef(query.returnType)
        .p("): I${rpc.nameUpperCamelCase}QueryResponse<").printTypeRef(query.returnType)
        .ln(">")

    private fun SmartWriter.printBasePrefix(rpc: RpRpc, parents: List<RpRpcGroup>) =
        p("I${rpc.nameUpperCamelCase}").pIf(!parents.isEmpty()) { p(parents.last().name) }

    private fun SmartWriter.printBaseFields(parents: List<RpRpcGroup>): SmartWriter {
        parents.forEach { group ->
            group.args.forEach { field ->
                p("override var").printField(field).ln(",")
            }
        }
        return this
    }
}