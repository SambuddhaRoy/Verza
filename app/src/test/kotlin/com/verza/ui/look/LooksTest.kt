package com.verza.ui.look

import androidx.compose.ui.graphics.Color
import com.verza.ui.theme.Palette
import com.verza.ui.theme.contrastRatio
import com.verza.ui.theme.fromHsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LooksTest {
    /** Covers of every hue, from washed out to vivid, and none at all. */
    private val covers: List<List<Color>> = listOf(emptyList<Color>()) +
        (0 until 360 step 20).flatMap { h -> listOf(0.3f, 0.9f).map { s -> listOf(fromHsv(h.toFloat(), s, 0.8f)) } }

    private fun check(what: String, fg: Color, bg: Color, floor: Float = Palette.MIN_CONTRAST) {
        val r = contrastRatio(fg, bg)
        assertTrue("$what reads at %.2f:1".format(r), r >= floor)
    }

    /** Words on a look's page read, whatever is playing and whichever the system's theme. */
    @Test fun everyLookReadsOnItsPage() {
        for (look in Looks.all) for (dark in listOf(true, false)) for (cover in covers) {
            val p = look.palette(cover, dark)
            val name = "${look.name} (${if (dark) "dark" else "light"}, ${cover.firstOrNull()})"
            check("$name ink", p.ink, p.canvas)
            check("$name secondary", p.inkMuted, p.canvas)
            check("$name accent", p.accent, p.canvas)
        }
    }

    /** And on Now Playing, whose ground the look makes from the page's. */
    @Test fun everyLookReadsOnNowPlaying() {
        for (look in Looks.all) for (dark in listOf(true, false)) for (cover in covers) {
            val p = look.nowPlaying(look.palette(cover, dark))
            val name = "${look.name} Now Playing (${if (dark) "dark" else "light"})"
            check("$name ink", p.ink, p.canvas)
            check("$name secondary", p.inkMuted, p.canvas)
        }
    }

    /** A spring past its end asked Roboto Flex for weight 1050, and the app crashed: weights are held to 100 to 1000. */
    @Test fun flexWeightsStayInRange() {
        assertEquals(1000, FlexLook.flex(1050, 100f, 16f, 20f).fontWeight?.weight)
        assertEquals(100, FlexLook.flex(40, 100f, 16f, 20f).fontWeight?.weight)
    }

    @Test fun eachLookHasItsOwnId() {
        assertEquals(Looks.all.size, Looks.all.map { it.id }.toSet().size)
        assertEquals(GlassLook, Looks.byId("no such look"))
        assertEquals(14, Looks.all.size)
    }
}
