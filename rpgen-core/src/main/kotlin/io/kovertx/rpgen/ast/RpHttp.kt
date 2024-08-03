package io.kovertx.rpgen.ast

data class RpHttp(
    override val name: String,
    val authSchemes: List<RpHttpAuthScheme>,
    val routes: List<RpHttpRoute>,
    override val parsedFrom: SourceRef
) : RpAstNode, RpNamedNode {

    val flatEndpoints get() = routes.flatMap { it.flatEndpoints }
    override val childAstNodes get() = sequence {
        yieldAll(authSchemes)
        yieldAll(routes)
    }
}

sealed interface RpHttpAuthScheme : RpAstNode, RpNamedNode
data class RpHttpAuthSchemeBasic(
    override val name: String,
    override val parsedFrom: SourceRef
) : RpHttpAuthScheme
data class RpHttpAuthSchemeBearer(
    override val name: String,
    val token: RpHttpBearerToken,
    override val parsedFrom: SourceRef
) : RpHttpAuthScheme {
    override val childAstNodes = sequenceOf(token)
}

sealed interface RpHttpBearerToken : RpAstNode
data class RpHttpBearerJwt(
    val token: RpTypeRef?,
    val payload: RpTypeRef?,
    override val parsedFrom: SourceRef
) : RpHttpBearerToken {
    override val childAstNodes = sequence {
        if (token != null) yield(token)
        if (payload != null) yield(payload)
    }
}

sealed interface RpHttpRoute : RpAstNode {
    val path: List<RpHttpPathPart>
    val authConstraints: RpHttpAuthConstraint?
    val flatEndpoints: List<RpHttpEndpoint>
}

data class RpHttpAuthConstraint(
    override val name: String,
    val required: Boolean,
    override val parsedFrom: SourceRef
) : RpAstNode, RpNamedNode

data class RpHttpRouteGroup(
    override val path: List<RpHttpPathPart>,
    override val authConstraints: RpHttpAuthConstraint?,
    val children: List<RpHttpRoute>,
    override val parsedFrom: SourceRef
) : RpHttpRoute {

    override val flatEndpoints get() = children
        .flatMap { it.flatEndpoints }
        .map { it.copy(
            path = path.plus(it.path),
            authConstraints = it.authConstraints ?: authConstraints
        ) }

    override val childAstNodes get() = sequence {
        yieldAll(path)
        if (authConstraints != null) yield(authConstraints)
        yieldAll(children)
    }
}

data class RpHttpEndpoint(
    override val name: String,
    val verb: RpHttpVerb,
    override val path: List<RpHttpPathPart>,
    override val authConstraints: RpHttpAuthConstraint?,
    val queryParams: List<RpField>,
    val bodyType: RpTypeRef?,
    val responseDefs: List<RpHttpResponseDef>,
    override val parsedFrom: SourceRef
) : RpHttpRoute, RpNamedNode {

    val pathParams get() = path.filterIsInstance<RpHttpPathParam>()
    override val flatEndpoints get() = listOf(this)

    override val childAstNodes get() = sequence {
        yieldAll(path)
        if (authConstraints != null) yield(authConstraints)
        yieldAll(queryParams)
        if (bodyType != null) yield(bodyType)
        yieldAll(responseDefs)
    }
}

sealed interface RpHttpPathPart : RpAstNode
data class RpHttpPathLiteral(
    val value: String,
    override val parsedFrom: SourceRef
) : RpHttpPathPart

data class RpHttpPathParam(
    override val name: String,
    val type: RpTypeRef,
    override val parsedFrom: SourceRef
) : RpHttpPathPart, RpNamedNode {
    override val childAstNodes get() = sequenceOf(type)
}

data class RpHttpHeader(
    val name: String,
    val isOptional: Boolean,
    val type: RpTypeRef,
    override val parsedFrom: SourceRef
) : RpAstNode {
    override val childAstNodes get() = sequenceOf(type)
}

data class RpHttpResponseDef(
    val codes: List<RpHttpResponseStatus>,
    val type: RpTypeRef?,
    override val parsedFrom: SourceRef
) : RpAstNode {
    override val childAstNodes get() = sequence {
        codes.forEach { yield(it) }
        if (type != null) yield(type)
    }
    val codesOrImplicit get() = codes.ifEmpty {
        listOf(RpHttpResponseStatus(200, null, parsedFrom))
    }
}

data class RpHttpResponseStatus(
    val code: Int,
    val name: String?,
    override val parsedFrom: SourceRef
) : RpAstNode

enum class RpHttpVerb {
    Delete, Get, Patch, Post, Put
}