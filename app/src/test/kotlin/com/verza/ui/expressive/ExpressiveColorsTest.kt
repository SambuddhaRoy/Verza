package com.verza.ui.expressive

import androidx.compose.ui.graphics.Color
import com.verza.ui.theme.CoverColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The contrast contract.
 *
 * This is the whole reason the redesign exists. The old player sampled its text colour off the album
 * art, so a cover with a dark navy vibrant swatch produced navy text on a near-black background and
 * nothing noticed. Moving to a saturated container makes that failure *easier* to reproduce, not
 * harder, because there is no longer a fixed white ramp underneath as a safety net.
 *
 * So the promise is checked here rather than trusted: sweep the entire hue wheel at a range of
 * saturations and lightnesses, and assert every text/background pair the UI actually uses clears
 * MIN_CONTRAST. If someone later "improves" the palette maths, this fails instead of shipping.
 */
class ExpressiveColorsTest {

    private fun coverOf(c: Color) = CoverColors(
        accent = c,
        bg = c,
        ink = Color.White,
        sub = Color.White,
        faint = Color.White,
        line = Color.White,
    )

    /** Every seed a real cover could plausibly produce. */
    private fun seeds(): List<Color> = buildList {
        var hue = 0f
        while (hue < 360f) {
            for (s in listOf(0.15f, 0.45f, 0.75f, 1f)) {
                for (v in listOf(0.12f, 0.35f, 0.6f, 0.85f, 1f)) {
                    add(fromHsv(hue, s, v))
                }
            }
            hue += 15f
        }
        // The degenerate ones that break naive maths: pure black, pure white, mid grey.
        add(Color.Black); add(Color.White); add(Color(0xFF808080))
    }

    @Test
    fun `every text pair clears the contrast floor for every possible cover`() {
        val floor = ExpressiveColors.MIN_CONTRAST
        var checked = 0
        for (seed in seeds()) {
            val c = expressiveColorsFrom(coverOf(seed))
            val where = "seed=${seed.value.toString(16)}"

            assertTrue(
                "$where: onContainer only ${contrastRatio(c.container, c.onContainer)}",
                contrastRatio(c.container, c.onContainer) >= floor,
            )
            assertTrue(
                "$where: onContainerMuted only ${contrastRatio(c.container, c.onContainerMuted)}",
                contrastRatio(c.container, c.onContainerMuted) >= floor,
            )
            assertTrue(
                "$where: onAccent only ${contrastRatio(c.accent, c.onAccent)}",
                contrastRatio(c.accent, c.onAccent) >= floor,
            )
            assertTrue(
                "$where: onSurface only ${contrastRatio(c.surface, c.onSurface)}",
                contrastRatio(c.surface, c.onSurface) >= floor,
            )
            assertTrue(
                "$where: onSurfaceMuted only ${contrastRatio(c.surface, c.onSurfaceMuted)}",
                contrastRatio(c.surface, c.onSurfaceMuted) >= floor,
            )
            // Tertiary is a real text/fill pair too, not decoration — it carries secondary emphasis.
            assertTrue(
                "$where: onTertiary only ${contrastRatio(c.tertiary, c.onTertiary)}",
                contrastRatio(c.tertiary, c.onTertiary) >= floor,
            )
            checked++
        }
        assertTrue("swept a meaningful number of seeds", checked > 400)
    }

    @Test
    fun `accent is visibly separate from the container it sits on`() {
        // Two tones of the same lightness read as one flat colour — the controls have to pop off the
        // background, not merely be legible against it.
        for (seed in seeds()) {
            val c = expressiveColorsFrom(coverOf(seed))
            assertTrue(
                "accent/container contrast ${contrastRatio(c.accent, c.container)} for ${seed.value.toString(16)}",
                contrastRatio(c.accent, c.container) >= ExpressiveColors.MIN_CONTRAST,
            )
        }
    }

    @Test
    fun `container stays in a saturated mid-dark band whatever the cover`() {
        for (seed in seeds()) {
            val c = expressiveColorsFrom(coverOf(seed))
            val v = hsv(c.container)[2]
            // Never so bright it becomes a light theme by accident, never so dark it stops being a
            // colour at all — both extremes were what made covers feel arbitrary before.
            assertTrue("container value $v out of band", v in 0.28f..0.56f)
        }
    }

    @Test
    fun `hsv round-trips`() {
        // The maths moved out of android.graphics so this file could be tested at all; check it did
        // not quietly change meaning on the way.
        for (seed in seeds()) {
            val h = hsv(seed)
            val back = fromHsv(h[0], h[1], h[2])
            assertEquals("red", seed.red, back.red, 0.01f)
            assertEquals("green", seed.green, back.green, 0.01f)
            assertEquals("blue", seed.blue, back.blue, 0.01f)
        }
    }

    @Test
    fun `the Material scheme derived from the palette is readable too`() {
        val floor = ExpressiveColors.MIN_CONTRAST
        for (seed in seeds()) {
            val c = expressiveColorsFrom(coverOf(seed))
            val s = expressiveColorScheme(c)
            val where = "seed=${seed.value.toString(16)}"

            // The pairs that untouched screens actually draw with.
            val pairs = listOf(
                "background/onBackground" to (s.background to s.onBackground),
                "surface/onSurface" to (s.surface to s.onSurface),
                "surfaceVariant/onSurfaceVariant" to (s.surfaceVariant to s.onSurfaceVariant),
                "primary/onPrimary" to (s.primary to s.onPrimary),
                "secondary/onSecondary" to (s.secondary to s.onSecondary),
                "tertiary/onTertiary" to (s.tertiary to s.onTertiary),
                "primaryContainer/onPrimaryContainer" to (s.primaryContainer to s.onPrimaryContainer),
                "secondaryContainer/onSecondaryContainer" to (s.secondaryContainer to s.onSecondaryContainer),
            )
            for ((name, pair) in pairs) {
                val ratio = contrastRatio(pair.first, pair.second)
                assertTrue("$where: $name only $ratio", ratio >= floor)
            }
        }
    }
}
