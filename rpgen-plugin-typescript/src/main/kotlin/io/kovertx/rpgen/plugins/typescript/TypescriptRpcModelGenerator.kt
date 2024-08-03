package io.kovertx.rpgen.plugins.typescript

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.ast.*
import io.kovertx.rpgen.util.SmartWriter
import io.kovertx.rpgen.util.smartly

object TypescriptRpcModelGenerator : BaseTypescriptGenerator<Unit>() {
    override val id = "rpc-model"

    override fun generate(ctx: IGenerateContext) {
        ctx.schemas.rpcs.forEach { rpc ->
            val config = mergeKotlinConfigs(ctx.schemas, "rpc")
            ctx.output.writeFile("generated${rpc.name}Rpc.ts") {
                smartly {
                    printImports(ctx)
                    printRpc(rpc)
                }
            }
        }
    }

    private fun SmartWriter.printImports(ctx: IGenerateContext) {
        ctx.schemas.models.forEach { model ->
            ln("import {").indent {
                model.types.forEach {
                    if (it !is RpEnum) p("type ")
                    ln("${it.name},")
                }
            }.ln("} from \"./generated${model.name}Model\";")
            ln("import { type ${model.name}ModelJsonCodecs } from \"./generated${model.name}ModelJsonCodec\";")
        }
    }

    private fun SmartWriter.printRpc(rpc: RpRpc) {
        ln()
        p("export interface I${rpc.name}Command<K extends string> ").curly {
            ln("_r: K")
        }

        ln()
        p("export interface I${rpc.name}Query<K extends string, R> ").curly {
            ln("_response: R")
            ln("_t: K")
        }

        ln()
        p("export interface I${rpc.name}QueryResponse<K extends string, R> ").curly {
            ln("_t: K")
            ln("_c: number")
            ln("response: R")
        }

        ln()
        p("export interface I${rpc.name}Notice<K extends string> ").curly {
            ln("_t: K")
        }

        rpc.methods.forEach { method ->
            printRpcMethod(rpc.name, method)
        }

        ln()
        p("export type ${rpc.name}Command")
        indent {
            rpc.terminalMethods.filterIsInstance<RpRpcCommand>().forEachIndexed { i, command ->
                if (i == 0) p("= ") else p("| ")
                ln(command.name)
            }
        }
        ln()
        p("export type ${rpc.name}Query")
        indent {
            rpc.terminalMethods.filterIsInstance<RpRpcQuery>().forEachIndexed { i, query ->
                if (i == 0) p("= ") else p("| ")
                ln(query.name)
            }
        }
        ln()
        p("export type ${rpc.name}QueryResponse")
        indent {
            rpc.terminalMethods.filterIsInstance<RpRpcQuery>().forEachIndexed { i, query ->
                if (i == 0) p("= ") else p("| ")
                p(query.name).ln("Response")
            }
        }
        ln()
        p("export type ${rpc.name}Notice")
        indent {
            rpc.terminalMethods.filterIsInstance<RpRpcNotice>().forEachIndexed { i, notice ->
                if (i == 0) p("= ") else p("| ")
                ln(notice.name)
            }
        }
        ln()
        ln("export type ${rpc.name}ClientMessage")
        indent {
            ln("= ${rpc.name}Command")
            ln("| ${rpc.name}Query")
        }
        ln()
        ln("export type ${rpc.name}ServerMessage")
        indent {
            ln("= ${rpc.name}Notice")
            ln("| ${rpc.name}QueryResponse")
        }
        ln()
        ln("export type ${rpc.name}Message")
        indent {
            ln("= ${rpc.name}ClientMessage")
            ln("| ${rpc.name}ServerMessage")
        }
    }

    private fun SmartWriter.printRpcMethod(root: String, method: RpRpcMethod): SmartWriter = when (method) {
        is RpRpcGroup -> printRpcGroup(root, method)
        is RpRpcCommand -> printRpcCommand(root, method)
        is RpRpcQuery -> printRpcQuery(root, method)
        is RpRpcNotice -> printRpcNotice(root, method)
    }

    private fun SmartWriter.printRpcGroup(root: String, group: RpRpcGroup) = ln()
        .p("export interface I${root}${group.name}Command<K extends string> extends I${root}Command<K> ")
        .curly {
            group.args.forEach { printField(it).ln() }
        }
        .ln()
        .p("export interface I${root}${group.name}Query<K extends string, R> extends I${root}Query<K, R> ")
        .curly {
            group.args.forEach { printField(it).ln() }
        }
        .ln()
        .p("export interface I${root}${group.name}QueryResponse<K extends string, R> extends I${root}QueryResponse<K, R>")
        .ln()
        .p("export interface I${root}${group.name}Notice<K extends string> extends I${root}Notice<K> ")
        .curly {
            group.args.forEach { printField(it).ln() }
        }.also {
            group.methods.forEach { printRpcMethod("${root}${group.name}", it) }
        }

    private fun SmartWriter.printRpcCommand(root: String, command: RpRpcCommand) = ln()
        .p("export interface ${command.name} extends I${root}Command<\"${command.name}\"> ")
        .curly {
            command.args.forEach { printField(it).ln() }
        }

    private fun SmartWriter.printRpcQuery(root: String, query: RpRpcQuery) = ln()
        .p("export interface ${query.name} extends I${root}Query<\"${query.name}\", ")
        .printTypeRef(query.returnType).p("> ")
        .curly {
            query.args.forEach { printField(it).ln() }
        }
        .ln()
        .p("export interface ${query.name}Response extends I${root}QueryResponse<\"${query.name}Response\", ")
        .printTypeRef(query.returnType).p(">")

    private fun SmartWriter.printRpcNotice(root: String, notice: RpRpcNotice) = ln()
        .p("export interface ${notice.name} extends I${root}Notice<\"${notice.name}\"> ")
        .curly {
            notice.args.forEach { printField(it).ln() }
        }
}