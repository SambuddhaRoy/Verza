package com.verza.ui.expressive

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** The app has one corner radius. This fails on a corner written any way but through it. */
class CornerConsistencyTest {
    private val defined = setOf("Shapes.kt", "ExpressiveShapes.kt", "LiquidGlass.kt")

    // Flat along one edge is fine (a sheet, a bar), as long as the radius is the one radius.
    private val stray = Regex(
        """\bCircleShape\b|\bSquircleShape\(|\bRectangleShape\b|ButtonDefaults\.(textS|s)hape|""" +
            """\bRoundedCornerShape\((?!top[A-Z]\w* = (com\.verza\.ui\.theme\.)?VerzaCorner)""",
    )

    @Test fun `every corner in the UI is the one radius`() {
        val found = File("src/main/kotlin/com/verza/ui").walkTopDown()
            .filter { it.extension == "kt" && it.name !in defined }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { i, line ->
                    "${file.name}:${i + 1}: ${line.trim()}".takeIf { !line.startsWith("import ") && stray.containsMatchIn(line) }
                }
            }.toList()
        assertEquals(found.joinToString("\n"), 0, found.size)
    }
}
