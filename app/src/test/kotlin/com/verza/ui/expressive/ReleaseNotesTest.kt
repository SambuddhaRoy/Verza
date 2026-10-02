package com.verza.ui.expressive

import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The What's new sheet used to show release notes with their Markdown still in them. */
class ReleaseNotesTest {

    private val notes = """
        **A new look you can switch to: Poster.**

        ## Choose your design

        - **Material** is Verza as you know it.
        - **Poster** is new.

        Collected by [Uncut](https://uncut.wtf). Licences ship with the app.
    """.trimIndent()

    @Test
    fun `no markdown is left showing`() {
        val text = releaseNotesText(notes).text
        assertFalse("asterisks left in: $text", "**" in text)
        assertFalse("a heading marker left in: $text", "##" in text)
        assertFalse("a link's URL left in: $text", "https://" in text)
        assertTrue("the link's text was lost", "Collected by Uncut." in text)
        assertTrue("bullets should read as bullets", "•  Material is Verza as you know it." in text)
    }

    @Test
    fun `bold and headings are styled, not just stripped`() {
        val styled = releaseNotesText(notes)
        val bold = styled.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
            .map { styled.text.substring(it.start, it.end) }
        assertTrue("the opening line should be bold: $bold", "A new look you can switch to: Poster." in bold)
        assertTrue("the heading should be bold: $bold", "Choose your design" in bold)
        assertTrue("the bullet's lead word should be bold: $bold", "Material" in bold)
    }

    @Test
    fun `paragraphs keep their gap, and a stray asterisk pair survives`() {
        assertEquals("one\n\ntwo", releaseNotesText("one\n\n\n\ntwo").text)
        assertEquals("a ** b", releaseNotesText("a ** b").text)
    }
}
