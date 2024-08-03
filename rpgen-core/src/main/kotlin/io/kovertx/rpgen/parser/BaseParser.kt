package io.kovertx.rpgen.parser

import io.kovertx.rpgen.ast.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import kotlin.reflect.KClass

abstract class BaseParser<
    TRef : ParserRuleContext,
    TField : ParserRuleContext
    > {

    protected fun mapField(field: TField, parent: SourceRef) = RpField(
        name = getFieldName(field).text,
        isOptional = getFieldOptional(field) != null,
        type = mapTypeRef(getFieldType(field), parent),
        parsedFrom = parent.child(field)
    )

    protected fun mapFields(fields: List<TField>, parent: SourceRef) =
        fields.map { mapField(it, parent) }

    protected fun mapTypeRef(ref: TRef, parent: SourceRef): RpTypeRef = RpTypeRef(
        type = getRefName(ref).text,
        params = getRefParams(ref).map { mapTypeRef(it, parent) },
        isNullable = getRefNullable(ref) != null,
        parsedFrom = parent.child(ref)
    )

    protected abstract fun getRefName(ref: TRef): ParserRuleContext
    protected abstract fun getRefParams(ref: TRef): List<TRef>
    protected abstract fun getRefNullable(ref: TRef): Token?

    protected abstract fun getFieldName(field: TField): ParserRuleContext
    protected abstract fun getFieldType(field: TField): TRef
    protected abstract fun getFieldOptional(field: TField): Token?
}

abstract class BaseExprParser<
    TRef : ParserRuleContext,
    TField : ParserRuleContext,
    TExpr : ParserRuleContext,
    TExprCall : TExpr,
    TExprId : TExpr,
    TExprRef : TExpr
    >(
    private val exprCall: KClass<TExprCall>,
    private val exprId: KClass<TExprId>,
    private val exprRef: KClass<TExprRef>,
) : BaseParser<TRef, TField>() {

    private fun mapExprCall(call: TExprCall, parent: SourceRef) = RpExprCall(
        func = getFunc(call).text,
        args = getArgs(call).map { mapExpr(it, parent) },
        parsedFrom = parent.child(call)
    )

    private fun mapExprId(id: TExprId, parent: SourceRef) = RpExprIdentifier(
        name = getName(id).text,
        parsedFrom = parent.child(id)
    )

    private fun mapExprRef(ref: TExprRef, parent: SourceRef) = RpExprTypeRef(
        ref = mapTypeRef(getType(ref), parent),
        parsedFrom = parent.child(ref)
    )

    protected abstract fun getFunc(call: TExprCall): ParserRuleContext
    protected abstract fun getArgs(call: TExprCall): List<TExpr>
    protected abstract fun getName(id: TExprId): ParserRuleContext
    protected abstract fun getType(ref: TExprRef): TRef

    fun mapExpr(expr: TExpr, parent: SourceRef): RpExpr {
        return when {
            exprCall.isInstance(expr) -> mapExprCall(expr as TExprCall, parent)
            exprId.isInstance(expr) -> mapExprId(expr as TExprId, parent)
            exprRef.isInstance(expr) -> mapExprRef(expr as TExprRef, parent)
            else -> TODO()
        }
    }
}