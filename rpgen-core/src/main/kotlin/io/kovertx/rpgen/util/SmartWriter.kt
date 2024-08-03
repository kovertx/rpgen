package io.kovertx.rpgen.util

import java.io.PrintWriter
import java.io.StringWriter
import java.util.*

data class SmartWriterOptions(
    var indexBlankLines: Boolean = false,
    var indentStr: String = " ",
    var indentCount: Int = 2
)

/**
 * A utility class that makes it easier to write nicely formatted output to a PrintWriter
 */
class SmartWriter(
    private val writer: PrintWriter,
    val options: SmartWriterOptions = SmartWriterOptions()
) {

    private val prefixes = Stack<String>()
    private var isLineStarted = false

    init {
        prefixes.push("")
    }

    /**
     * Print directly to the writer
     */
    fun p(s: String): SmartWriter {
        if (!isLineStarted) {
            writer.print(prefixes.peek())
            isLineStarted = true
        }
        writer.print(s)
        return this
    }

    fun pIf(cond: Boolean, block: SmartWriter.() -> Unit): SmartWriter {
        if (cond) block(this)
        return this
    }

    fun capture(fn: SmartWriter.() -> Unit): String = StringWriter().use { stringWriter ->
        PrintWriter(stringWriter).use { printWriter ->
            fn(SmartWriter(printWriter))
        }
        return@use stringWriter.toString()
    }

    /**
     * Print an EOL
     */
    fun ln(): SmartWriter {
        if (!isLineStarted && options.indexBlankLines) {
            p("")
        }

        writer.println()
        isLineStarted = false
        return this
    }

    /**
     * Print a string followed by an EOL
     */
    fun ln(s: String) = p(s).ln()

    fun plns(vararg ss: String): SmartWriter = ss.foldRight(this) { s, self -> self.ln(s) }

    /**
     * Calls handler, with default indentation for all lines printed inside it's scope
     */
    fun indent(block: SmartWriter.() -> Unit): SmartWriter {
        return indent(options.indentCount, block)
    }

    /**
     * Custom number of spaces
     */
    fun indent(n: Int, block: SmartWriter.() -> Unit): SmartWriter {
        val prefix = (1..n)
            .map { options.indentStr }
            .joinToString("")
        return prefix(prefix, block)
    }

    /**
     * Call handler, with a custom prefix for all lines printed during it
     */
    fun prefix(prefix: String, block: SmartWriter.() -> Unit): SmartWriter {
        prefixes.push(prefixes.peek() + prefix)
        block(this)
        prefixes.pop()
        return this
    }

    fun curly(block: SmartWriter.() -> Unit): SmartWriter {
        ln("{")
        indent(block)
        return ln("}")
    }

    fun parens(block: SmartWriter.() -> Unit): SmartWriter {
        ln("(")
        indent(block)
        return ln(")")
    }

    fun squares(block: SmartWriter.() -> Unit): SmartWriter {
        ln("[")
        indent(block)
        return ln("]")
    }

    fun <T> seperated(separator: String, items: Iterable<T>, itemBlock: SmartWriter.() -> Unit) =
        seperated({ p(separator) }, items, itemBlock)

    fun <T> seperated(separatorBlock: SmartWriter.() -> Unit, items: Iterable<T>, itemBlock: SmartWriter.() -> Unit): SmartWriter {
        items.forEachIndexed { i, x ->
            if (i > 0) separatorBlock(this)
            itemBlock(this)
        }
        return this
    }

    fun <T> forEach(items: Iterable<T>, block: SmartWriter.(T) -> Unit): SmartWriter {
        items.forEach { block(it) }
        return this
    }
}

fun PrintWriter.smartly(handler: SmartWriter.() -> Unit) {
    handler(SmartWriter(this))
}
