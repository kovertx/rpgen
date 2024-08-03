package io.kovertx.rpgen.util

object IdUtils {
    fun splitIdentifier(id: String): List<String> {
        val segments = mutableListOf<String>()
        val currentSegment = StringBuilder()

        fun emitCurrentSegment() {
            if (currentSegment.isNotEmpty()) {
                segments.add(currentSegment.toString())
                currentSegment.clear()
            }
        }

        for (c in id) {
            when {
                // Uppercase character indicates new segment, and is part of the new segment
                c.isUpperCase() -> {
                    emitCurrentSegment()
                    currentSegment.append(c.lowercaseChar())
                }
                // other letters should continue a segment
                c.isLowerCase() -> currentSegment.append(c)
                c.isDigit() -> {
                    if (currentSegment.isEmpty()) throw IllegalArgumentException("identifier segment cannot begin with a digit: ${id}")
                     currentSegment.append(c)
                }
                c == '_' || c == '-' -> emitCurrentSegment()
            }
        }
        emitCurrentSegment()

        return segments
    }

    fun toCamelCase(id: String) = splitIdentifier(id).mapIndexed { i, s ->
        if (i == 0) s else s.replaceFirstChar { it.uppercaseChar() }
    }.joinToString("")

    fun toUpperCamelCase(id: String) = splitIdentifier(id).map { s ->
        s.replaceFirstChar { it.uppercaseChar() }
    }.joinToString("")

    fun toSnakeCase(id: String) = splitIdentifier(id).joinToString("_")

    fun toKebabCase(id: String) = splitIdentifier(id).joinToString("-")
}