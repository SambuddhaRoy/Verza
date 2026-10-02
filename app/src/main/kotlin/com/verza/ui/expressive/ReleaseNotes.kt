package com.verza.ui.expressive

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

/**
 * Release notes, as written on GitHub, made readable in the app.
 *
 * The notes are Markdown, and the What's new and Update sheets used to show them as typed:
 * asterisks round every bold phrase, hashes in front of every heading. This handles exactly what
 * the notes use and nothing more: `## ` headings, `- ` bullets, `**bold**`, and `[text](url)` links,
 * which become their text (the sheet is not the place to leave the app). Anything else passes
 * through as written, which is what it was doing already.
 *
 * ponytail: a dozen lines, not a Markdown library; the notes are written by us, in this subset.
 * Reach for a real parser if they ever need tables or nested lists.
 */
fun releaseNotesText(markdown: String): AnnotatedString = buildAnnotatedString {
    val lines = markdown.replace("\r\n", "\n").trim().lines()
    var blank = 0
    lines.forEachIndexed { i, raw ->
        val line = raw.trimEnd()
        if (line.isBlank()) {
            blank++
            return@forEachIndexed
        }
        if (length > 0) append(if (blank > 0) "\n\n" else "\n")
        blank = 0
        when {
            line.startsWith("## ") || line.startsWith("# ") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                inline(line.trimStart('#', ' '))
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                append("•  ")
                inline(line.substring(2))
            }
            else -> inline(line)
        }
    }
}

private val LINK = Regex("""\[([^\]]+)]\([^)]*\)""")

/** Bold runs and links within one line. */
private fun AnnotatedString.Builder.inline(text: String) {
    val plain = LINK.replace(text) { it.groupValues[1] }
    val parts = plain.split("**")
    parts.forEachIndexed { i, part ->
        // Odd pieces sit between a pair of ** markers. An unpaired ** leaves its tail plain.
        if (i % 2 == 1 && i < parts.size - 1) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
        else append(if (i % 2 == 1) "**$part" else part)
    }
}
