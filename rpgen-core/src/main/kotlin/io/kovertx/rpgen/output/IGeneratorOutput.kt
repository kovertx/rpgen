package io.kovertx.rpgen.output

import java.io.PrintWriter

interface IGeneratorOutput {
    fun writeFile(path: String, writer: PrintWriter.() -> Unit)
}
