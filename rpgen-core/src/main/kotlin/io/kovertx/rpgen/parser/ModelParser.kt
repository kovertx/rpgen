package io.kovertx.rpgen.parser

import RpModelLexer
import RpModelParser
import RpModelParser.*
import io.kovertx.rpgen.ast.*
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

object ModelParser : BaseExprParser<
    Type_refContext,
    FieldContext,
    ExprContext,
    Expr_callContext,
    Expr_idContext,
    Expr_refContext>(Expr_callContext::class, Expr_idContext::class, Expr_refContext::class)
{
    override fun getFieldName(field: FieldContext) = field.name
    override fun getFieldType(field: FieldContext) = field.type_ref()
    override fun getFieldOptional(field: FieldContext) = field.isOptional
    override fun getRefName(ref: Type_refContext) = ref.type
    override fun getRefParams(ref: Type_refContext) = ref.type_ref()
    override fun getRefNullable(ref: Type_refContext) = ref.isNullable
    override fun getFunc(call: Expr_callContext) = call.func
    override fun getName(id: Expr_idContext) = id.identifier()
    override fun getArgs(call: Expr_callContext) = call.expr()
    override fun getType(ref: Expr_refContext) = ref.type_ref()

    fun parseModel(name: String, src: String, parent: SourceRef = SourceRef("unknown", 0, 0)): RpModel {
        val lexer = RpModelLexer(ANTLRInputStream(src))
        val parser = RpModelParser(CommonTokenStream(lexer))

        val types: List<RpTypeDef> = parser.model_root().children.map {
            return@map when (it) {
                is Extern_declContext -> mapExtern(it, parent)
                is Id_declContext -> mapId(it, parent)
                is Enum_declContext -> mapEnum(it, parent)
                is Struct_declContext -> mapStruct(it, parent)
                is Opaque_declContext -> mapOpaque(it, parent)
                else -> throw IllegalArgumentException("Unexpected parse node: ${it.javaClass.simpleName}")
            }
        }

        return RpModel(
            name = name,
            types = types,
            parsedFrom = parent)
    }

    private fun mapExtern(ctx: Extern_declContext, parent: SourceRef) =
        RpExternalTypeDef(ctx.name.text, parent.child(ctx))

    private fun mapId(ctx: Id_declContext, parent: SourceRef) =
        RpId(ctx.name.text, parent.child(ctx))

    private fun mapOpaque(ctx: Opaque_declContext, parent: SourceRef) = RpOpaque(
        name = ctx.name.text,
        primitive = ctx.primitive_type().text,
        parsedFrom = parent.child(ctx))

    private fun mapEnum(ctx: Enum_declContext, parent: SourceRef) = RpEnum(
        name = ctx.name.text,
        values = ctx.enum_value().map { mapEnumValue(it, parent) },
        parsedFrom = parent.child(ctx))

    private fun mapEnumValue(ctx: Enum_valueContext, parent: SourceRef) = RpEnumValue(
        name = ctx.name.text,
        value = Integer.parseInt(ctx.value.text, 10),
        parsedFrom = parent.child(ctx))

    private fun mapStruct(ctx: Struct_declContext, parent: SourceRef) = RpStruct(
        name = ctx.name.text,
        fields = mapFields(ctx.struct_body().field_list().field(), parent),
        mutations = ctx.mutation().map { mapMutation(it, parent) },
        parsedFrom = parent.child(ctx))

    private fun mapMutation(ctx: MutationContext, parent: SourceRef) = RpMutation(
        id = ctx.id.text,
        args = ctx.expr().map { mapExpr(it, parent) },
        parsedFrom = parent.child(ctx))
}