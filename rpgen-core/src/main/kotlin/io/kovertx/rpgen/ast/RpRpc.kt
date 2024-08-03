package io.kovertx.rpgen.ast

data class RpRpc(
    override val name: String,
    val methods: List<RpRpcMethod>,
    override val parsedFrom: SourceRef
) : RpAstNode, RpNamedNode {
    val terminalMethods get() = methods.flatMap { it.terminalMethods }

    override val childAstNodes get() = methods.asSequence()
}

sealed interface RpRpcMethod : RpAstNode, RpNamedNode {
    val args: List<RpField>
    val doc: List<String>

    val terminalMethods get() = listOf(this)
}

data class RpRpcGroup(
    override val name: String,
    override val args: List<RpField>,
    override val doc: List<String>,
    val methods: List<RpRpcMethod>,
    override val parsedFrom: SourceRef
) : RpRpcMethod {

    override val terminalMethods get() =
        methods.flatMap { it.terminalMethods }

    override val childAstNodes: Sequence<RpAstNode> get() = sequence {
        yieldAll(args)
        yieldAll(methods)
    }
}

data class RpRpcCommand(
    override val name: String,
    override val args: List<RpField>,
    override val doc: List<String>,
    override val parsedFrom: SourceRef
) : RpRpcMethod {
    override val childAstNodes get() = args.asSequence()
}

data class RpRpcQuery(
    override val name: String,
    override val args: List<RpField>,
    override val doc: List<String>,
    val returnType: RpTypeRef,
    override val parsedFrom: SourceRef
) : RpRpcMethod {
    override val childAstNodes get() = sequence {
        yieldAll(args)
        yield(returnType)
    }
}

data class RpRpcNotice(
    override val name: String,
    override val args: List<RpField>,
    override val doc: List<String>,
    override val parsedFrom: SourceRef
) : RpRpcMethod {
    override val childAstNodes get() = args.asSequence()
}
