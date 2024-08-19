package io.kovertx.rpgen.ast

data class RpModel(
    override val name: String,
    val types: List<RpTypeDef>,
    override val parsedFrom: SourceRef
) : RpAstNode, RpNamedNode {

    override val childAstNodes get() = types.asSequence()
}

sealed interface RpTypeDef : RpAstNode, RpNamedNode

/**
 * Declares a type that is defined outside of the model
 */
data class RpExternalTypeDef(
    override val name: String,
    override val parsedFrom: SourceRef
): RpTypeDef

data class RpOpaque(
    override val name: String,
    val primitive: String,
    override val parsedFrom: SourceRef
): RpTypeDef

/**
 * Defines a named UUID wrapper type
 */
data class RpId(
    override val name: String,
    override val parsedFrom: SourceRef,
    val tags: Set<String>
) : RpTypeDef

/**
 * Defines an enumeration type
 */
data class RpEnum(
    override val name: String,
    val values: List<RpEnumValue>,
    override val parsedFrom: SourceRef
) : RpTypeDef {
    override val childAstNodes = values.asSequence()
}

data class RpEnumValue(
    override val name: String,
    val value: Int,
    override val parsedFrom: SourceRef
) : RpAstNode, RpNamedNode

/**
 * Defines a data structure
 */
data class RpStruct(
    override val name: String,
    val fields: List<RpField>,
    val mutations: List<RpMutation>,
    override val parsedFrom: SourceRef
) : RpTypeDef {
    override val childAstNodes get() = fields.asSequence()
}

/**
 * Defines a field (e.g. for a struct)
 */
data class RpField(
    override val name: String,
    val isOptional: Boolean,
    val type: RpTypeRef,
    override val parsedFrom: SourceRef
) : RpAstNode, RpNamedNode {
    override val childAstNodes get() = sequenceOf(type)
}

/**
 * Defines a type reference
 */
data class RpTypeRef(
    val type: String,
    val params: List<RpTypeRef>,
    val isNullable: Boolean,
    override val parsedFrom: SourceRef
) : RpAstNode {
    override val childAstNodes get() = params.asSequence()

    override fun collectTypeDependencies(results: MutableSet<RpTypeDependency>) {
        when (type) {
            "i8", "i16", "i32", "i64",
            "u8", "u16", "u32", "u64",
            "str", "bool" -> results.add(RpTypeDependency(type, true, 0))
            "list" -> {
                results.add(RpTypeDependency(type, true, 1))
            }
            else -> {
                results.add(RpTypeDependency(type, false, params.size))
            }
        }
        super.collectTypeDependencies(results)
    }
}

data class RpMutation(
    val id: String,
    val args: List<RpExpr>,
    override val parsedFrom: SourceRef
) : RpAstNode {
    override val childAstNodes get() = args.asSequence()
}