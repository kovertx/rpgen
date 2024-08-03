package io.kovertx.rpgen.parser

import RpRpcLexer
import RpRpcParser
import RpRpcParser.*
import io.kovertx.rpgen.ast.*
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import java.lang.IllegalArgumentException

object RpcParser {
    fun parseRpc(name: String, src: String, parent: SourceRef): RpRpc {
        val lexer = RpRpcLexer(ANTLRInputStream(src))
        val parser = RpRpcParser(CommonTokenStream(lexer))

        val methods = parser.rpc_api().rpc_method().map { mapMethod(it, parent) }
        return RpRpc(name, methods, parent)
    }

    private fun mapMethod(ctx: Rpc_methodContext, parent: SourceRef): RpRpcMethod {
        return when {
            ctx.rpc_command_decl() != null -> mapCommand(ctx.rpc_command_decl(), parent)
            ctx.rpc_query_decl() != null -> mapQuery(ctx.rpc_query_decl(), parent)
            ctx.rpc_notice_decl() != null -> mapNotice(ctx.rpc_notice_decl(), parent)
            ctx.rpc_group() != null -> mapGroup(ctx.rpc_group(), parent)
            else -> throw IllegalArgumentException("Unknown method parse tree")
        }
    }

    private fun mapGroup(ctx: Rpc_groupContext, parent: SourceRef) = RpRpcGroup(
        name = ctx.name.text,
        args = mapArgs(ctx.method_args(), parent),
        doc = listOf(),
        methods = ctx.rpc_method().map { mapMethod(it, parent) },
        parsedFrom = parent.child(ctx))

    private fun mapCommand(ctx: Rpc_command_declContext, parent: SourceRef) = RpRpcCommand(
        name = ctx.name.text,
        args = mapArgs(ctx.method_args(), parent),
        doc = listOf(),
        parsedFrom = parent.child(ctx))

    private fun mapQuery(ctx: Rpc_query_declContext, parent: SourceRef) = RpRpcQuery(
        name = ctx.name.text,
        args = mapArgs(ctx.method_args(), parent),
        returnType = mapTypeRef(ctx.type_ref(), parent),
        doc = listOf(),
        parsedFrom = parent.child(ctx))

    private fun mapNotice(ctx: Rpc_notice_declContext, parent: SourceRef) = RpRpcNotice(
        name = ctx.name.text,
        args = mapArgs(ctx.method_args(), parent),
        doc = listOf(),
        parsedFrom = parent.child(ctx))

    private fun mapArgs(ctx: Method_argsContext, parent: SourceRef) =
        ctx.field_list().field().map { mapField(it, parent) }

    private fun mapField(ctx: FieldContext, parent: SourceRef) = RpField(
        name = ctx.name.text,
        isOptional = ctx.isOptional != null,
        type = mapTypeRef(ctx.type, parent),
        parsedFrom = parent.child(ctx))

    private fun mapTypeRef(ctx: Type_refContext, parent: SourceRef): RpTypeRef = RpTypeRef(
        type = ctx.type.text,
        params = ctx.type_ref().map { mapTypeRef(it, parent) },
        isNullable = ctx.isNullable != null,
        parsedFrom = parent.child(ctx))
}