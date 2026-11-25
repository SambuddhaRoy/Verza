package com.verza.ui.theme

import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteTest {
    /**
     * The accent is the words on Play and Shuffle, which sit on the fill, as well as on the page: it has
     * to read on both, whatever the cover, in either theme.
     */
    @Test fun accentReadsOnThePageAndOnAButton() {
        for (dark in listOf(true, false)) for (h in 0 until 360 step 6) for (s in listOf(0.2f, 0.5f, 0.8f, 1f)) for (v in listOf(0.2f, 0.5f, 0.8f, 1f)) {
            val p = Palette.from(listOf(fromHsv(h.toFloat(), s, v)), dark)
            val button = p.fill.compositeOver(p.canvas)
            assertTrue("hue $h s $s v $v dark $dark on the page", contrastRatio(p.accent, p.canvas) >= Palette.MIN_CONTRAST)
            assertTrue("hue $h s $s v $v dark $dark on a button", contrastRatio(p.accent, button) >= Palette.MIN_CONTRAST)
            assertTrue("secondary text, dark $dark", contrastRatio(p.inkMuted, p.canvas) >= Palette.MIN_CONTRAST)
        }
    }

    @Test fun noCoverIsMagenta() {
        val p = Palette.from(emptyList(), dark = true)
        assertTrue(hsv(p.accent)[0] in 300f..340f)
    }
}
