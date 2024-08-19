package io.kovertx.rpgen.plugins.kotlin.kovertx

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.util.SmartWriter
import io.kovertx.rpgen.util.smartly
import io.kovertx.rpgen.ast.*
import io.kovertx.rpgen.plugins.kotlin.KotlinGeneratorBase

object KotlinKovertxWebGenerator : KotlinGeneratorBase<Unit>() {
    override val id: String get() = "kovertx-web"

    private val statusCodeNames = mapOf(
        100 to "Continue",
        101 to "SwitchingProtocols",
        102 to "Processing",
        103 to "ProcessingServices",
        200 to "Ok",
        201 to "Created",
        202 to "Accepted",
        203 to "NonAuthoritativeInformation",
        204 to "NoContent",
        205 to "ResetContent",
        206 to "PartialContent",
        300 to "MultipleChoices",
        301 to "MovedPermanently",
        302 to "Found",
        303 to "SeeOther",
        304 to "NotModified",
        307 to "TemporaryRedirect",
        308 to "PermanentRedirect",
        400 to "BadRequest",
        401 to "Unauthorized",
        403 to "Forbidden",
        404 to "NotFound",
        405 to "MethodNotAllowed",
        406 to "NotAcceptable",
        407 to "ProxyAuthenticationRequired",
        408 to "RequestTimeout",
        409 to "Conflict",
        410 to "Gone",
        411 to "LengthRequired",
        412 to "PreconditionFailed",
        413 to "PayloadTooLarge",
        414 to "URITooLong",
        415 to "UnsupportedMediaType",
        416 to "RangeNotSatisfiable",
        417 to "ExpectationFailed",
        418 to "ImaTeapot",
        421 to "MisdirectedRequest",
        426 to "UpgradeRequired",
        428 to "PreconditionRequired",
        429 to "TooManyRequests",
        431 to "RequestHeaderFieldTooLarge",
        451 to "UnavailableFrLegalReasons",
        500 to "InternalServerError",
        501 to "NotImplemented",
        502 to "BadGateway",
        503 to "ServiceUnavailable",
        504 to "GatewayTimeout",
        505 to "HttpVersionNotSupported",
        506 to "VariantAlsoNegotiates",
        507 to "InsufficientStorage",
        508 to "LoopDetected",
        510 to "NotExtended",
        511 to "NetworkAuthenticationRequired",
    )

    private fun statusCodeName(code: Int): String {
        if (code < 100 || code >= 600) {
            throw IllegalArgumentException("$code is not a valid status code")
        }
        return statusCodeNames[code] ?: "Status${code}"
    }

    override fun generate(ctx: IGenerateContext) {
        ctx.schemas.https.forEach { http ->

            val config = mergeKotlinConfigs(ctx.schemas, "http")
            val pkg = (config.pkg ?: "io.kovertx.rpgen.demo")
            val pkgPath = pkg.replace(Regex("\\s*\\.\\s*"), "/")

            val modelPkg = mergeKotlinConfigs(ctx.schemas, "model")
                .pkg ?: "io.kovertx.rpggen.demo"

            ctx.output.writeFile("${pkgPath}/Generated${http.name}Routes.kt") {
                smartly {
                    ln("package ${pkg}")
                    ln("import io.vertx.core.Future")
                    ln("import io.vertx.core.Vertx")
                    ln("import io.vertx.core.json.JsonObject")
                    ln("import io.vertx.ext.auth.jwt.JWTAuth")
                    ln("import io.vertx.ext.web.Router")
                    ln("import io.vertx.ext.web.RoutingContext")
                    ln("import io.vertx.ext.web.handler.AuthenticationHandler")
                    ln("import io.vertx.ext.web.handler.JWTAuthHandler")
                    ln("import kotlinx.serialization.json.Json")
                    ln("import kotlinx.serialization.encodeToString")
                    ln("import io.kovertx.web.RouteBuilder")
                    ln("import io.kovertx.web.RouterBuilder")
                    ln("import io.kovertx.web.buildRouter")
                    ln("import ${modelPkg}.*")
                    printEndpointContexts(http)
                    printHttpApi(http)
                    printBuildRouter(http)
                }
            }
        }
    }

    private fun SmartWriter.printEndpointContexts(http: RpHttp) {
        ln()
        ln("private val jwtJson = Json { ignoreUnknownKeys = true }")
        val authSchemesByName = http.authSchemes.associateBy { it.name }

        http.flatEndpoints.forEach { endpoint ->
            ln()
            p("class ${endpoint.nameUpperCamelCase}RoutingContext(private val ctx: RoutingContext): RoutingContext by ctx ").curly {
                endpoint.responseDefs.forEach { response ->
                    val responseType = response.type

                    response.codesOrImplicit.forEach { status ->
                        val statusName = status.name ?: "return${statusCodeName(status.code)}"
                        p("fun $statusName(")
                        if (responseType != null) p("body: ").printTypeRef(responseType)
                        p(") ").curly {
                            p("ctx.response().setStatusCode(${status.code}).send(")
                            if (responseType != null) p("Json.encodeToString(body)")
                            ln(")")
                        }
                    }
                }

                val authName = endpoint.authConstraints?.name
                if (authName != null) {
                    val auth = authSchemesByName[authName]
                        ?: throw IllegalArgumentException("Auth scheme ${authName} not defined")
                    when (auth) {
                        is RpHttpAuthSchemeBearer -> {
                            val token = auth.token
                            when (token) {
                                is RpHttpBearerJwt -> {
                                    val payloadType = token.payload?.let { capture { printTypeRef(it) } } ?: "ObjectNode"
                                    ln("val auth get() = jwtJson.decodeFromString<${payloadType}>(ctx.user().principal().encode())")
                                }
                            }
                        }
                        is RpHttpAuthSchemeBasic -> { /* noop */ }
                    }
                }
            }
        }
    }

    private fun SmartWriter.printHttpApi(http: RpHttp) = ln()
        .blockComment {
            ln("Generated interface defining methods needed to implement ${http.name} HTTP API.")
        }
        .p("interface ${http.name}Api ").curly {

            http.flatEndpoints.forEach { endpoint ->
                printControllerEndpoint(endpoint)
            }

            http.authSchemes.forEach { authScheme ->
                when (authScheme) {
                    is RpHttpAuthSchemeBasic -> {
                        ln()
                        ln("fun create${authScheme.name}AuthHandler(): AuthenticationHandler")
                    }
                    is RpHttpAuthSchemeBearer -> {
                        val token = authScheme.token
                        when (token) {
                            is RpHttpBearerJwt -> {
                                ln()
                                ln("val ${authScheme.name}JwtAuth: JWTAuth")
                                ln()
                                ln("fun create${authScheme.name}AuthHandler(): AuthenticationHandler = JWTAuthHandler.create(${authScheme.name}JwtAuth)")
                                ln()
                                p("fun generate${authScheme.name}Token(payload: ")
                                val payload = token.payload
                                if (payload != null) printTypeRef(payload) else p("JsonElement")
                                p("): ")
                                val tokenType = token.token
                                if (tokenType != null) printTypeRef(tokenType) else p("String")
                                p(" ").curly {
                                    ln("val json = Json.encodeToString(payload)")
                                    ln("val rawToken = ${authScheme.name}JwtAuth.generateToken(JsonObject(json))")
                                    p("return ")
                                    if (tokenType != null) printTypeRef(tokenType).ln("(rawToken)") else ln("rawToken")
                                }
                            }
                        }
                    }
                }
            }

            http.flatEndpoints
                .flatMap { it.path.filterIsInstance<RpHttpPathParam>() }
                .map { it.type }
                .plus(http.flatEndpoints.flatMap { it.queryParams.map { it.type } })
                .filter { it.type != "str" }
                .map {
                    capture { printTypeRef(it) } }
                .toSet()
                .forEach { type ->
                    val typeAscii = type
                        .replace("<", "$1")
                        .replace(">", "$2")
                        .replace(",", "$3")
                        .replace(" ", "")
                    ln().blockComment {
                        ln("Defines how to parse a path parameter (String) as the mapped type.")
                    }
                    p("fun decode${typeAscii}(str: String): $type").ln()
                }

            ln().blockComment {
                ln("Injector called once before all api-defined endpoints (once per router)")
            }
            ln("fun beforeAllRoutes(builder: RouterBuilder) {}")

            ln().blockComment {
                ln("Injector called once after all api-defined endpoints (once per router)")
            }
            ln("fun afterAllRoutes(builder: RouterBuilder) {}")

            ln().blockComment {
                ln("Injector called before all other handlers when defining a route (once for each route)")
            }
            ln("fun beforeEachRoute(builder: RouteBuilder) {}")

            ln().blockComment {
                ln("Injector called after all other handlers when defining a route (once for each route)")
            }
            ln("fun afterEachRoute(builder: RouteBuilder) {}")

            ln().blockComment {
                ln("Injector called just before an auth handler when defining a route (called once")
                ln("for each route that defines an auth type)")
            }
            ln("fun beforeAnyAuth(builder: RouteBuilder) {}")

            ln().blockComment {
                ln("Injector called just after an auth handler when defining a route (called once")
                ln("for each routes that defines an auth type)")
            }
            ln("fun afterAnyAuth(builder: RouteBuilder) {}")
        }

    private fun SmartWriter.printControllerEndpoint(endpoint: RpHttpEndpoint) {
        // TODO if there are multiple response types we should provide a helper to determine result
        val params = endpoint.path.filterIsInstance<RpHttpPathParam>()
        val body = endpoint.bodyType

        ln()
        blockComment {
            ln("Implementation for ${endpoint.verb} ${endpoint.path.toPathStr()}")
            ln("@param ctx the routing context for this request")
            params.forEach { param ->
                ln("@param ${param.name} path parameter :${param.name}")
            }
            if (body != null) ln("@param body deserialized request body")
        }
        p("fun ${endpoint.name}(")

        // if the endpoint requires we'll inject a wrapped RoutingContext with helpers to extract
        // auth content
        p("ctx: ${endpoint.nameUpperCamelCase}RoutingContext")

        // path parameters will be parsed and passed as arguments to controller methods
        endpoint.path.filterIsInstance<RpHttpPathParam>().forEach { param ->
            p(", ").p(param.name).p(": ").printTypeRef(param.type)
        }

        endpoint.queryParams.forEach { param ->
            p(", ").p(param.name).p(": ").printTypeRef(param.type)
            if (param.isOptional) p("?")
        }

        // if the endpoint has a body type we'll pass that as well
        if (body != null) {
            p(", body: ").printTypeRef(body)
        }

        p("): Unit")
    }

    private fun SmartWriter.printBuildRouter(http: RpHttp) {
        ln()
        blockComment {
            ln("Builds a router that connects endpoints to the API implementation.")
        }
        p("fun build${http.nameUpperCamelCase}Router(vertx: Vertx, api: ${http.nameUpperCamelCase}Api): Router = buildRouter(vertx) ").curly {
            http.authSchemes.forEach { authScheme ->
                ln("val auth${authScheme.name}Handler = api.create${authScheme.name}AuthHandler()")
            }

            ln()
            ln("api.beforeAllRoutes(this)")

            http.flatEndpoints.forEach {
                printBuildRoute(it)
            }

            ln()
            ln("api.afterAllRoutes(this)")
        }
    }

    private fun SmartWriter.printBuildRoute(endpoint: RpHttpEndpoint) {
        val verb = endpoint.verb.name.toLowerCase()
        val authName = endpoint.authConstraints?.name

        ln()
        p("$verb(\"${endpoint.path.toPathStr()}\") ").curly {
            if (endpoint.bodyType != null) ln("consumes(\"application/json\")")

            ln("api.beforeEachRoute(this)")
            if (authName != null) {
                ln("api.beforeAnyAuth(this)")
                ln("handler(auth${authName}Handler)")
                ln("api.afterAnyAuth(this)")
            }
            ln("handler { ctx ->").indent {
                // Decode path parameters
                endpoint.path.filterIsInstance<RpHttpPathParam>().forEach { param ->
                    p("val ${param.name}: ").printTypeRef(param.type).p(" = ")

                    if (param.type.type == "str") ln("ctx.pathParam(\"${param.name}\")")
                    else p("api.decode").printTypeRef(param.type)
                        .ln("(ctx.pathParam(\"${param.name}\"))")
                }

                // Decode query parameters
                endpoint.queryParams.forEach { param ->
                    p("val ${param.name}: ").printTypeRef(param.type)
                    ln("? = ctx.queryParam(\"${param.name}\")")
                    indent {
                        if (param.type.type != "str") ln(".map { api.decode${param.type.type}(it) }")
                        ln(".firstOrNull()")
                    }
                    if (!param.isOptional) ln("if (${param.name} == null) throw RuntimeException()")
                }

                val body = endpoint.bodyType
                if (body != null) {
                    p("val body = Json.decodeFromString<")
                        .printTypeRef(body)
                        .ln(">(ctx.body().asString())")
                }

                p("api.").p(endpoint.name).p("(")
                p("${endpoint.nameUpperCamelCase}RoutingContext(ctx)")
                endpoint.path.filterIsInstance<RpHttpPathParam>().forEach { param ->
                    p(", ${param.name}")
                }

                endpoint.queryParams.forEach { param ->
                    p(", ${param.name}")
                }

                if (body != null) p(", body")
                ln(")")
            }.ln("}")
            ln("api.afterEachRoute(this)")
        }

    }

    private fun List<RpHttpPathPart>.toPathStr() = "/" + map { part ->
        when (part) {
            is RpHttpPathLiteral -> part.value
            is RpHttpPathParam -> ":${part.name}"
        }
    }.joinToString("/")

}