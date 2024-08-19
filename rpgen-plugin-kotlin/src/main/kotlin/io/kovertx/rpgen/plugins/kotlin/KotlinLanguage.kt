package io.kovertx.rpgen.plugins.kotlin

import KotlinConfigLexer
import KotlinConfigParser
import io.kovertx.rpgen.ast.SourceRef
import io.kovertx.rpgen.plugins.RpLanguage
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

object KotlinLanguage : RpLanguage<KotlinConfig> {
    override val id: String = "kotlin"

    override fun parseLanguageConfig(src: String, parentSource: SourceRef): KotlinConfig {
        val lexer = KotlinConfigLexer(ANTLRInputStream(src))
        val parser = KotlinConfigParser(CommonTokenStream(lexer))
        val root = parser.kotlin_config()
        return KotlinConfig(
            pkg = root.kotlin_package()?.scoped_identifier()?.text,
            imports = root.kotlin_import().map {
                KotlinImport(
                    klassName = it.scoped_identifier().text,
                    alias = it.identifier()?.text)
            },
            aliases = root.kotlin_typealias().map {
                KotlinTypeAlias(
                    name = it.identifier().text,
                    src = it.scoped_identifier().text
                )
            }
        )
    }
}

data class KotlinConfig(
    val pkg: String?,
    val imports: List<KotlinImport>,
    val aliases: List<KotlinTypeAlias>)
data class KotlinImport(val klassName: String, val alias: String?)
data class KotlinTypeAlias(val name: String, val src: String)
