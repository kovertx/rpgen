package io.kovertx.rpgen.plugins.openapi

import io.kovertx.rpgen.IGenerateContext
import com.reidsync.kxjsonpatch.JsonPatch
import io.kovertx.rpgen.ast.*
import io.kovertx.rpgen.plugins.RpGenerator
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

object OpenApiSchemaGenerator : RpGenerator<OpenApiConfig, Unit> {
    override val id = "schema"
    override val language = OpenApiLanguage

    override fun generate(ctx: IGenerateContext) {
        val allTypes = ctx.schemas.models.flatMap { model -> model.types }
        val jsonConfig = Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
        ctx.schemas.https.forEach { http ->
            var root: JsonElement = buildJsonObject {
                put("openapi", "3.1.0")
                putJsonObject("info") {
                    put("title", http.name)
                    put("version", "1.0")
                }
                put("paths", createPathsObject(http))
                putJsonObject("components") {
                    put("schemas", createSchemasObject(allTypes))
                    put("securitySchemes", buildSecuritySchemasObject(http))
                }
            }

            ctx.schemas.configs
                .map { it.data }
                .filterIsInstance<OpenApiConfig>()
                .forEach { config ->
                    root = JsonPatch.apply(config.schemaDiff, root)
                    println("Using config: ${config}")
                }

            val content = jsonConfig.encodeToString(root)
            ctx.output.writeFile("openapi.${http.name}.json") {
                this.print(content)
            }
        }
    }

    private fun createPathsObject(http: RpHttp): JsonObject {
        val paths = mutableMapOf<String, JsonObject>()
        http.flatEndpoints
            .groupBy { endpoint ->
                "/" + endpoint.path.map { segment ->
                    when (segment) {
                        is RpHttpPathLiteral -> segment.value
                        is RpHttpPathParam -> "{${segment.name}}"
                    }
                }.joinToString("/")
            }.forEach { path, endpoints ->
                val pathObject = buildJsonObject {
                    endpoints.forEach { endpoint ->
                        val verb = endpoint.verb.name.lowercase()
                        put(verb, createOperationObject(endpoint))
                    }
                }
                paths.put(path, pathObject)
            }
        return JsonObject(paths)
    }

    private fun createOperationObject(endpoint: RpHttpEndpoint): JsonObject {
        return buildJsonObject {
            put("operationId", endpoint.name)

            val auth = endpoint.authConstraints
            if (auth != null) {
                putJsonArray("security") {
                    addJsonObject {
                        putJsonArray(auth.name) {}
                    }
                }
            }

            val requestBody = endpoint.bodyType
            if (requestBody != null) putJsonObject("requestBody") {
                putJsonObject("content") {
                    putJsonObject("application/json") {
                        put("schema", refToSchema(requestBody))
                    }
                }
            }

            put("responses", buildJsonObject {
                endpoint.responseDefs.forEach { response ->
                    response.codesOrImplicit.forEach { status ->
                        putJsonObject("${status.code}") {
                            putJsonObject("content") {
                                response.type?.let { responseType ->
                                    putJsonObject("application/json") {
                                        put("schema", refToSchema(responseType))
                                    }
                                }
                            }
                        }
                    }
                }
            })

        }
    }

    private fun buildSecuritySchemasObject(http: RpHttp): JsonObject {
        return buildJsonObject {
            http.authSchemes.forEach { authScheme ->
                putJsonObject(authScheme.name) {
                    when (authScheme) {
                        is RpHttpAuthSchemeBasic -> {
                            put("type", "http")
                            put("scheme", "basic")
                        }
                        is RpHttpAuthSchemeBearer -> {
                            put("type", "http")
                            put("scheme", "Bearer")

                            val token = authScheme.token
                            when (token) {
                                is RpHttpBearerJwt -> {
                                    put("bearerFormat", "JWT")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun createSchemasObject(types: List<RpTypeDef>): JsonObject {
        return buildJsonObject {
            types.forEach { type ->
                put(type.name, createSchema(type))
            }
        }
    }

    private val primitiveSchemas = mapOf(
        "str" to buildJsonObject {
            put("type", "string")
        },
        "bool" to buildJsonObject {
            put("type", "boolean")
        },
        "u8" to buildJsonObject {
            put("type", "integer")
            put("minimum", 0)
            put("maximum", 255)
        },
        "u16" to buildJsonObject {
            put("type", "integer")
            put("minimum", 0)
            put("maximum", 65535)
        },
        "u32" to buildJsonObject {
            put("type", "integer")
            put("minimum", 0)
            put("maximum", 4294967295)
        },
        "u64" to buildJsonObject {
            put("type", "integer")
            put("minimum", 0)
        },
        "i8" to buildJsonObject {
            put("type", "integer")
            put("minimum", Byte.MIN_VALUE.toInt())
            put("maximum", Byte.MAX_VALUE.toInt())
        },
        "i16" to buildJsonObject {
            put("type", "integer")
            put("minimum", Short.MIN_VALUE.toInt())
            put("maximum", Short.MAX_VALUE.toInt())
        },
        "i32" to buildJsonObject {
            put("type", "integer")
            put("format", "int32")
        },
        "i64" to buildJsonObject {
            put("type", "integer")
            put("format", "int32")
        },
        "f32" to buildJsonObject {
            put("type", "number")
            put("format", "float")
        },
        "f64" to buildJsonObject {
            put("type", "number")
            put("format", "double")
        }
    )

    private fun refToSchema(ref: RpTypeRef): JsonObject {
        val basic = primitiveSchemas[ref.type] ?:
            if (ref.type == "List") {
                buildJsonObject {
                    put("type", "array")
                    put("items", refToSchema(ref.params[0]))
                }
            } else {
                buildJsonObject {
                    put("\$ref", "#/components/schemas/${ref.type}")
                }
            }

        if (ref.isNullable) {
            return buildJsonObject {
                basic.forEach { key, value ->
                    put("key", value)
                }
                put("nullable", true)
            }
        }

        return basic
    }

    private fun createSchema(type: RpTypeDef): JsonObject {
        return when (type) {
            is RpExternalTypeDef -> buildJsonObject { }
            is RpId -> buildJsonObject {
                put("type", "string")
                put("format", "uuid")
            }
            is RpEnum -> buildJsonObject {
                put("type", "integer")
                put("oneOf", buildJsonArray {
                    type.values.forEach { enumValue ->
                        add(buildJsonObject {
                            put("title", enumValue.name)
                            put("const", enumValue.value)
                        })
                    }
                })
            }
            is RpStruct -> buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    type.fields.forEach { field ->
                        put(field.name, refToSchema(field.type))
                    }
                })
                put("required", buildJsonArray {
                    type.fields.filter { !it.isOptional }.forEach { field ->
                        add(field.name)
                    }
                })
            }
            is RpOpaque -> primitiveSchemas[type.primitive]!!
        }
    }
}