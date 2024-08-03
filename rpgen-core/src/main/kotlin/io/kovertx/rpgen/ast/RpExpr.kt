package io.kovertx.rpgen.ast

import java.math.BigDecimal

sealed interface RpExpr : RpAstNode

data class RpExprCall(
    val func: String,
    val args: List<RpExpr>,
    override val parsedFrom: SourceRef
) : RpExpr {
    override val childAstNodes get() = args.asSequence()
}

data class RpExprIdentifier(
    val name: String,
    override val parsedFrom: SourceRef
) : RpExpr

data class RpExprTypeRef(
    val ref: RpTypeRef,
    override val parsedFrom: SourceRef
) : RpExpr {
    override val childAstNodes get() = sequenceOf(ref)
}

data class RpExprStr(
    val value: String,
    override val parsedFrom: SourceRef
) : RpExpr

data class RpExprNumber(
    val value: BigDecimal,
    override val parsedFrom: SourceRef
) : RpExpr

data class RpExprBool(
    val value: Boolean,
    override val parsedFrom: SourceRef
) : RpExpr
