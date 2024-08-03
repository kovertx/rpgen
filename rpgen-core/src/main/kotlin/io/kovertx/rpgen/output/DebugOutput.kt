package io.kovertx.rpgen.compiler.output

import io.kovertx.rpgen.output.IGeneratorOutput
import java.io.PrintWriter
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

class DebugOutput(val dir: String) : IGeneratorOutput {
    override fun writeFile(path: String, writer: PrintWriter.() -> Unit) {
        val full = Paths.get(dir, path).normalize().absolutePathString()
        println("--- debug write: ${full}")
        val printer = PrintWriter(System.out)
        writer.invoke(printer)
        printer.flush()
        println("--- debug close: ${full}")
    }
}