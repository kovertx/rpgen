package io.kovertx.rpgen.parser

import RpHttpLexer
import RpHttpParser
import RpHttpParser.*
import io.kovertx.rpgen.ast.*
import org.antlr.v4.runtime.*

object HttpParser {

    fun parseHttp(name: String, src: String, parent: SourceRef): RpHttp {
        val lexer = RpHttpLexer(ANTLRInputStream(src)).apply {
            line = parent.line
            charPositionInLine = parent.col
        }
        val parser = RpHttpParser(CommonTokenStream(lexer))

        val root = parser.http_api()
        return RpHttp(
            name = name,
            authSchemes = root.http_auth_scheme().map { mapHttpAuthScheme(it, parent) },
            routes = root.http_route().map { mapRoute(it, parent) },
            parsedFrom = parent)
    }

    private fun mapHttpAuthScheme(ctx: Http_auth_schemeContext?, parent: SourceRef) = when (ctx) {
        is Http_auth_scheme_basicContext -> RpHttpAuthSchemeBasic(
            name = ctx.name.text,
            parsedFrom = parent.child(ctx))
        is Http_auth_scheme_bearerContext -> RpHttpAuthSchemeBearer(
            name = ctx.name.text,
            token = mapBearerToken(ctx.http_bearer_token(), parent),
            parsedFrom = parent.child(ctx))
        else -> throw IllegalArgumentException("Unexpected auth scheme AST node: ${ctx}")
    }

    private fun mapBearerToken(ctx: Http_bearer_tokenContext, parent: SourceRef) = when (ctx) {
        is Http_bearer_token_jwtContext -> RpHttpBearerJwt(
            token = ctx.token_type?.let { mapTypeRef(it, parent) },
            payload = ctx.payload_type?.let { mapTypeRef(it, parent) },
            parsedFrom = parent.child(ctx)
        )
        else -> TODO("Unhandled bearer token type")
    }

    private fun mapRoute(ctx: Http_routeContext, parent: SourceRef): RpHttpRoute = when (ctx) {
        is Http_endpointContext -> mapEndpoint(ctx.detail, parent)
        is Http_route_groupContext -> mapRouteGroup(ctx.detail, parent)
        else -> throw IllegalArgumentException("Unknown route type in parse tree")
    }

    private fun mapRouteGroup(ctx: Http_route_group_detailContext, parent: SourceRef) =
        RpHttpRouteGroup(
            path = mapPath(ctx.http_path(), parent),
            authConstraints = mapAuthConstraint(ctx.http_auth_requirement(), parent),
            children = ctx.http_route().map { mapRoute(it, parent) },
            parsedFrom = parent.child(ctx))

    private fun mapVerb(ctx: Http_verbContext) =
        when {
            ctx.DELETE() != null -> RpHttpVerb.Delete
            ctx.GET() != null -> RpHttpVerb.Get
            ctx.PATCH() != null -> RpHttpVerb.Patch
            ctx.POST() != null -> RpHttpVerb.Post
            ctx.PUT() != null -> RpHttpVerb.Put
            else -> throw IllegalArgumentException("Unknown HTTP verb: ${ctx.text}")
        }

    private fun <T, R> Iterable<T>?.mapNullAsEmpty(transform: (T) -> R): List<R> {
        if (this == null) return emptyList()
        return this.map(transform)
    }

    private fun mapEndpoint(ctx: Http_endpoint_detailContext, parent: SourceRef) =
        RpHttpEndpoint(
            name = ctx.name.text,
            verb = mapVerb(ctx.http_verb()),
            path = mapPath(ctx.http_path(), parent),
            authConstraints = mapAuthConstraint(ctx.http_auth_requirement(), parent),
            bodyType = ctx.http_request_type()
                .map { mapTypeRef(it.type_ref(), parent) }
                .lastOrNull(),
            queryParams = ctx.http_query_params()?.struct_body()?.field_list()?.field()
                .mapNullAsEmpty { mapField(it, parent) },
            responseDefs = ctx.http_response_def().map { mapResponseType(it, parent) },
            parsedFrom = parent.child(ctx))

    private fun mapResponseType(ctx: Http_response_defContext, parent: SourceRef) = RpHttpResponseDef(
        codes = ctx.http_response_status().map { mapResponseStatus(it, parent) },
        type = ctx.type_ref()?.let { mapTypeRef(it, parent) },
        parsedFrom = parent.child(ctx))

    private fun mapResponseStatus(ctx: Http_response_statusContext, parent: SourceRef) =
        RpHttpResponseStatus(
            code = ctx.INT_LITERAL().text.toInt(),
            name = ctx.identifier()?.text,
            parsedFrom = parent.child(ctx))

    private fun mapAuthConstraint(ctx: Http_auth_requirementContext?, parent: SourceRef) =
        ctx?.let {
            RpHttpAuthConstraint(
                name = it.auth_name.text,
                required = it.REQUIRE() != null,
                parsedFrom = parent.child(it))
        }

    private fun mapPath(ctx: Http_pathContext, parent: SourceRef) =
        ctx.http_path_part().map { mapPathPart(it, parent) }

    private fun mapPathPart(ctx: Http_path_partContext, parent: SourceRef): RpHttpPathPart =
        when (ctx) {
            is Path_literal_simpleContext -> RpHttpPathLiteral(
                value = ctx.text,
                parsedFrom = parent.child(ctx))
            is Path_literal_quotedContext -> RpHttpPathLiteral(
                value = ctx.STR_LITERAL().unquoteStr(),
                parsedFrom = parent.child(ctx))
            is Path_paramContext -> RpHttpPathParam(
                name = ctx.param.text,
                type = mapTypeRef(ctx.type, parent),
                parsedFrom = parent.child(ctx))
            else -> throw IllegalArgumentException(
                "Unhandled path part type ${ctx.javaClass.simpleName}")
        }

    private fun mapField(ctx: FieldContext, parent: SourceRef) = RpField(
        name = ctx.name.text,
        isOptional = ctx.isOptional != null,
        type = mapTypeRef(ctx.type_ref(), parent),
        parsedFrom = parent.child(ctx))

    private fun mapTypeRef(ctx: Type_refContext, parent: SourceRef): RpTypeRef = RpTypeRef(
        type = ctx.type.text,
        isNullable = ctx.isNullable != null,
        params = ctx.type_ref().map { mapTypeRef(it, parent) },
        parsedFrom = parent.child(ctx))
}