package com.verza.ui.expressive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PosterScrimTest {
    private fun grey(v: Int) = (0xFF shl 24) or (v shl 16) or (v shl 8) or v

    @Test fun `white comes back as white and black as black`() {
        assertEquals(1f, brightnessOf(IntArray(10) { grey(255) }), 0.01f)
        assertEquals(0f, brightnessOf(IntArray(10) { grey(0) }), 0.01f)
    }

    @Test fun `a pale cover with a dark corner counts as pale`() {
        val pixels = IntArray(100) { if (it < 15) grey(0) else grey(255) }
        assertEquals(1f, brightnessOf(pixels), 0.01f)
    }

    @Test fun `brighter covers get more scrim, within bounds`() {
        assertEquals(0.62f, typeScrim(1f), 0.001f)
        assertEquals(0.10f, typeScrim(0.2f), 0.001f)
        assertTrue(typeScrim(0.7f) in 0.10f..0.62f && typeScrim(0.7f) < typeScrim(0.9f))
    }
}
