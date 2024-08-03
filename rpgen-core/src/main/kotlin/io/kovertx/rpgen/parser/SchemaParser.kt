package io.kovertx.rpgen.parser

import RpGenLexer
import RpGenParser
import RpGenParser.*
import io.kovertx.rpgen.ast.*
import io.kovertx.rpgen.plugins.PluginManager
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream

object SchemaParser {
    fun parse(src: String, doc: String = "unknown"): RpSchemas {
        val lexer = RpGenLexer(ANTLRInputStream(src))
        val parser = RpGenParser(CommonTokenStream(lexer))

        val configs = mutableListOf<RpConfig<*>>()
        val models = mutableListOf<RpModel>()
        val rpcs = mutableListOf<RpRpc>()
        val https = mutableListOf<RpHttp>()

        parser.blocks().children.forEach { block ->
            when (block) {
                is Config_blockContext -> {
                    val lang = block.lang.text
                    val gen = block.gen?.text
                    val scope = block.scope?.text
                    val content = block.any_content().text

                    if (gen == null) {
                        val language = PluginManager.getLanguage(lang)
                        val data = language.parseLanguageConfig(content, block.any_content().toSourceRef(doc))
                        configs.add(RpConfig(lang, gen, scope, data, block.toSourceRef(doc)))
                    } else {
                        val generator = PluginManager.getGenerator(lang, gen)
                        val data = generator.parseGeneratorConfig(content, block.any_content().toSourceRef(doc))
                        configs.add(RpConfig(lang, gen, scope, data, block.toSourceRef(doc)))
                    }
                }
                is Model_blockContext -> {
                    val model = ModelParser.parseModel(
                        block.name.text,
                        block.any_content().text
                    )
                    models.add(model)
                }
                is Rpc_blockContext -> {
                    val rpc = RpcParser.parseRpc(
                        block.name.text,
                        block.any_content().text,
                        block.any_content().toSourceRef(doc)
                    ).copy(parsedFrom = block.toSourceRef(doc))
                    rpcs.add(rpc)
                }
                is Http_blockContext -> {
                    val http = HttpParser.parseHttp(
                        block.name.text,
                        block.any_content().text,
                        block.any_content().toSourceRef(doc)
                    ).copy(parsedFrom = block.toSourceRef(doc))

                    https.add(http)
                }
            }
        }

        return RpSchemas(
            configs = configs,
            models = models,
            rpcs = rpcs,
            https = https
        )
    }

}

