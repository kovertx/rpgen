package io.kovertx.rpgen.plugins.typescript

import TypescriptConfigLexer
import TypescriptConfigParser
import io.kovertx.rpgen.ast.RpTypeRef
import io.kovertx.rpgen.ast.SourceRef
import io.kovertx.rpgen.parser.child
import io.kovertx.rpgen.plugins.RpLanguage
import io.kovertx.rpgen.plugins.typescript.TypescriptLanguage.mapTypeRef
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

object TypescriptLanguage : RpLanguage<TypescriptConfig> {
    override val id: String = "typescript"

    override fun parseLanguageConfig(src: String, parentSource: SourceRef): TypescriptConfig {
        val lexer = TypescriptConfigLexer(ANTLRInputStream(src)).apply {
            line = parentSource.line
            charPositionInLine = parentSource.col
        }
        val parser = TypescriptConfigParser(CommonTokenStream(lexer))
        val root = parser.typescript_config()
        return TypescriptConfig(
            imports = root.typescript_import().map { imp ->
                TypescriptImport(
                    src = imp.STR_LITERAL().text,
                    types = imp.typescript_imported_type().map { typ ->
                        TypescriptImportType(
                            name = typ.type_name.text,
                            alias = typ.alias?.text
                        )
                    }
                )
            },
            aliases = root.typescript_alias().map { alias ->
                TypescriptAlias(
                    name = alias.identifier().text,
                    from = alias.type_ref().mapTypeRef(parentSource)
                )
            }
        )
    }

    private fun TypescriptConfigParser.Type_refContext.mapTypeRef(parentSource: SourceRef): RpTypeRef = RpTypeRef(
        type = this.identifier().text,
        isNullable = false,
        params = this.type_ref().map { it.mapTypeRef(parentSource) },
        parsedFrom = parentSource.child(this)
    )
}

data class TypescriptConfig(
    val imports: List<TypescriptImport>,
    val aliases: List<TypescriptAlias>
)
data class TypescriptImport(
    val src: String,
    val types: List<TypescriptImportType>
)
data class TypescriptImportType(
    val name: String,
    val alias: String?,
)
data class TypescriptAlias(
    val name: String,
    val from: RpTypeRef
)