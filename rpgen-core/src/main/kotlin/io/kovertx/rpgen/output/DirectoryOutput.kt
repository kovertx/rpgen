package io.kovertx.rpgen.compiler.output

import io.kovertx.rpgen.output.IGeneratorOutput
import java.io.PrintWriter
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

class DirectoryOutput(val dir: String) : IGeneratorOutput {
    override fun writeFile(path: String, writer: PrintWriter.() -> Unit) {
        val full = Path(dir, path)
        full.parent.createDirectories()
        full.outputStream().use {
            PrintWriter(it).use {
                writer(it)
            }
        }
    }
}