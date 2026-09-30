package com.verza.ui.expressive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The three promises [pickTitleFace] makes about a song's title face. */
class TitleFaceTest {

    private val all: (Int) -> Boolean = { true }

    @Test
    fun `the same song always gets the same face`() {
        val first = pickTitleFace("videoId-abc", 12, avoid = null, covers = all)
        repeat(5) { assertEquals(first, pickTitleFace("videoId-abc", 12, avoid = null, covers = all)) }
    }

    @Test
    fun `a song never repeats the face of the song before it`() {
        // Sweep enough keys that plain hashing would certainly collide with the previous face.
        var previous: Int? = null
        for (i in 0 until 500) {
            val face = pickTitleFace("track-$i", 12, avoid = previous, covers = all)
            assertNotEquals("track-$i repeated the previous face", previous, face)
            previous = face
        }
    }

    @Test
    fun `a face missing a character is passed over, and no face at all means null`() {
        // Only face 7 can set this title.
        val face = pickTitleFace("anything", 12, avoid = null) { it == 7 }
        assertEquals(7, face)

        // A title none of them can set (Devanagari, say) falls back to the app's own type.
        assertNull(pickTitleFace("anything", 12, avoid = null) { false })

        // If the only face that fits is the last song's, a repeat beats a half-drawn title.
        assertEquals(3, pickTitleFace("anything", 12, avoid = 3) { it == 3 })
    }
}
