package io.kovertx.rpgen.parser

import io.kovertx.rpgen.ast.SourceRef
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.TerminalNode

fun ParserRuleContext.toSourceRef(doc: String) = SourceRef(
    line = start.line,
    col = start.charPositionInLine,
    doc = doc)

fun SourceRef.child(ctx: ParserRuleContext): SourceRef = ctx.toSourceRef(this.doc)

fun TerminalNode.unquoteStr(): String {
    val quoted = this.text
    val sb = StringBuilder()

    var i = 1
    while (i < quoted.length - 1) {
        val c = quoted[i]
        if (c == '\\') {
            when (quoted[i + 1]) {
                'r' -> sb.append('\r')
                'n' -> sb.append('\n')
                'b' -> sb.append('\b')
                't' -> sb.append('\t')
                '"' -> sb.append('"')
                '\\' -> sb.append('\\')
                else -> throw IllegalArgumentException("Unknown escape sequence \\${quoted[i+1]}")
            }
            i += 2
        } else {
            sb.append(c)
            i++
        }
    }
    return sb.toString()
}