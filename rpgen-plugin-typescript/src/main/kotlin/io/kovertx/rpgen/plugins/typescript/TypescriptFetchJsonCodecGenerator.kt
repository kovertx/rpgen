package io.kovertx.rpgen.plugins.typescript

import io.kovertx.rpgen.IGenerateContext
import io.kovertx.rpgen.util.SmartWriter
import io.kovertx.rpgen.util.smartly
import io.kovertx.rpgen.ast.*

object TypescriptFetchJsonCodecGenerator : BaseTypescriptGenerator<Unit>() {
    override val id = "fetch-json-codec"
    override val summary = "Generates HTTP client code using Fetch API and json-codec library"

    override fun generate(ctx: IGenerateContext) {
        ctx.schemas.https.forEach { http ->
            ctx.output.writeFile("Generated${http.name}FetchJsonCodecClient.ts") {
                smartly {
                    printImports(ctx)
                    printClientAuth(http)
                    printClientConfig(ctx, http)
                    printResponseType(http)
                    printClientClass(http)
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

    private fun SmartWriter.printClientAuth(http: RpHttp) {
        ln()
        p("export type ${http.name}AuthProvider<T> = T extends Function ? never : T | (() => T | Promise<T>)")
        ln()
        p("export interface ${http.name}ClientAuth ").curly {
            http.authSchemes.forEach { authSchema ->
                p("${authSchema.name}?: ${http.name}AuthProvider<")
                when (authSchema) {
                    is RpHttpAuthSchemeBasic -> p("{ username: string, password: string }")
                    is RpHttpAuthSchemeBearer -> {
                        val tokenType = authSchema.token
                        when (tokenType) {
                            is RpHttpBearerJwt -> {
                                p(tokenType.token?.type ?: "string")
                            }
                        }
                    }
                }
                ln(">,")
            }
        }
    }

    private fun SmartWriter.printJwtHelpers(jwt: RpHttpBearerJwt) {
        val token = jwt.token
        val payload = jwt.payload
        if (token == null || payload == null) return

        blockComment {
            ln("Attempts to decode the payload part of a JWT token.")
            ln("WARNING: This function does not check the signature of the token")
        }
        p("decode${payload.type}(token: ${token.type}) ").curly {
            ln("let [_header, payload, _signature] = token.split(\".\");")
            ln("payload = payload.replace(/-/g, \"+\").replace(/_/g, \"/\");")
            ln("const json = btoa(payload).split('').map(c => String.fromCharCode(c.charCodeAt(0))).join(\"\");")
            ln("return this.codecs.${payload.type}.parse(json);")
        }
    }

    private fun SmartWriter.printClientConfig(ctx: IGenerateContext, http: RpHttp) {
        // TODO only require codecs for the model types we actually use
        ln()
        p("export type ${http.name}ClientCodecs")
        ctx.schemas.models.forEachIndexed { i, model ->
            if (i > 0) p(" & ")
            else p(" = ")
            p("${model.name}ModelJsonCodecs")
        }
        ln(";")

        ln()
        p("export interface ${http.name}ClientConfig ").curly {
            ln("baseUrl?: string,")
            ln("auth?: ${http.name}ClientAuth,")
            ln("codecs: ${http.name}ClientCodecs")
        }
    }

    private fun SmartWriter.printResponseType(http: RpHttp) {
        ln()
        p("export interface ${http.name}Response<T, Status extends number, Name extends string | undefined> ").curly {
            ln("name: Name,")
            ln("status: Status,")
            ln("body: T,")
        }

        http.flatEndpoints.forEach { endpoint ->
            ln()
            ln("export type ${endpoint.nameUpperCamelCase}Response = ").indent {
                endpoint.responseDefs.forEach { response ->
                    val responseType = response.type

                    response.codesOrImplicit.forEach { status ->
                        p("| ${http.name}Response<")

                        if (responseType == null) p("undefined") else printTypeRef(responseType)
                        p(", ${status.code}")

                        val name = status.name
                        if (name == null) p(", undefined") else p(", \"${name}\"")

                        ln(">")
                    }
                }
            }
        }
    }

    private fun SmartWriter.printClientClass(http: RpHttp) {

        http.flatEndpoints.forEach { printEndpointRequestTypes(http, it) }

        ln()
        p("export class ${http.name}Client ").curly {
            ln("baseUrl: string = \"\"")
            ln("readonly auth: ${http.name}ClientAuth")
            ln("private readonly codecs: ${http.name}ClientCodecs")

            ln()
            p("constructor(config: ${http.name}ClientConfig) ").curly {
                ln("this.baseUrl = config.baseUrl ?? this.baseUrl;")
                ln("this.auth = config.auth ?? {};")
                ln("this.codecs = config.codecs;")
            }

            ln()
            p("private async resolveAuth<T>(provider: ${http.name}AuthProvider<T>) ").curly {
                ln("if (provider === undefined) throw new Error(\"Auth not defined\");")
                p("if (typeof provider === \"function\") ").curly {
                    ln("const tmp = provider();")
                    ln("return (tmp instanceof Promise) ? await tmp : tmp;")
                }
                ln("return provider as T;")
            }

            http.flatEndpoints.forEach { endpoint ->
                printEndpointFunction(http, endpoint)
            }
        }
    }

    private fun SmartWriter.printEndpointRequestTypes(http: RpHttp, endpoint: RpHttpEndpoint) {
        val body = endpoint.bodyType
        val authName = endpoint.authConstraints?.name

        ln()
        p("export interface ${endpoint.nameUpperCamelCase}RequestData ").curly {
            endpoint.pathParams.forEach { param ->
                p(param.name).p(": ").printTypeRef(param.type).ln(",")
            }
            endpoint.queryParams.forEach { param ->
                printField(param).ln(",")
            }
            if (body != null) p("body: ").printTypeRef(body).ln(",")
            if (authName != null) ln("auth?: ${http.name}ClientAuth['${authName}'],")
        }
    }

    private fun SmartWriter.printEndpointFunction(
        http: RpHttp, endpoint: RpHttpEndpoint
    ) {
        val body = endpoint.bodyType
        val authName = endpoint.authConstraints?.name

        ln()
        blockComment {
            ln("TODO")
        }

        val isDataOptional = endpoint.bodyType == null &&
            endpoint.pathParams.isEmpty() &&
            (endpoint.queryParams.isEmpty() || endpoint.queryParams.all { it.isOptional })

        // function decl
        p("async ${endpoint.nameCamelCase}(data: ${endpoint.nameUpperCamelCase}RequestData")
        if (isDataOptional) p(" = {}")
        p(")")

        // return type, body
        p(": Promise<${endpoint.nameUpperCamelCase}Response> ").curly {
            if (endpoint.queryParams.isNotEmpty()) {
                ln("const searchParams = new URLSearchParams();")
                endpoint.queryParams.forEach { queryParam ->
                    if (queryParam.isOptional) {
                        p("if (data.${queryParam.name} !== undefined) ")
                    }
                    ln("searchParams.set(\"${queryParam.name}\", `\${data.${queryParam.name}}`);")
                }
            }
            p("const url = `\${this.baseUrl}")
            endpoint.path.forEach { pathSegment ->
                p("/")
                when (pathSegment) {
                    is RpHttpPathLiteral -> p(pathSegment.value)
                    is RpHttpPathParam -> p("\${data.${pathSegment.name}}")
                }
            }
            if (endpoint.queryParams.isNotEmpty()) {
                p("?\${searchParams.toString()}")
            }
            ln("`;")

            if (authName != null) {
                ln("const auth = await this.resolveAuth(data.auth ?? this.auth['${authName}']);")
                ln("if (auth === undefined) throw new Error('auth is undefined')")
            }

            ln()
            ln("const response = await fetch(url, {").indent {
                ln("method: \"${endpoint.verb.name.uppercase()}\",")
                if (body != null) {
                    ln("body: this.codecs.${body.type}.stringify(data.body),")
                }
                p("headers: ").curly {
                    if (body != null) {
                        ln("\"content-type\": \"application/json\",")
                    }
                    if (authName != null) {
                        ln("authorization: `bearer \${auth}`,")
                    }
                }
            }.ln("});")

            ln()
            p("switch (response.status) ").curly {
                endpoint.responseDefs.forEach { response ->
                    val responseType = response.type

                    response.codesOrImplicit.forEach { status ->
                        val name = status.name

                        ln("case ${status.code}:")
                        indent {
                            p("return ").curly {
                                ln("status: ${status.code},")

                                p("name: ")
                                if (name == null) ln("undefined,") else ln("\"$name\",")

                                p("body: ")
                                if (responseType == null) ln("undefined,") else ln("this.codecs.${responseType.type}.parse(await response.text()),")
                            }
                        }
                    }
                }

                ln("default: throw 'Unexpected status code'")
            }
        }
    }
}