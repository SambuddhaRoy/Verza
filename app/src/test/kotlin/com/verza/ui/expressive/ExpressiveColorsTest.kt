package com.verza.ui.expressive

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
        for (flavour in ColorFlavour.entries) for (seed in seeds()) {
            val c = expressiveColorsFrom(coverOf(seed), flavour)
            val where = "$flavour seed=${seed.value.toString(16)}"

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
        for (flavour in ColorFlavour.entries) for (seed in seeds()) {
            val c = expressiveColorsFrom(coverOf(seed), flavour)
            assertTrue(
                "accent/container ${contrastRatio(c.accent, c.container)} for $flavour ${seed.value.toString(16)}",
                contrastRatio(c.accent, c.container) >= ExpressiveColors.MIN_CONTRAST,
            )
        }
    }

    @Test
    fun `each flavour keeps the container inside its own declared band`() {
        // A flavour is only its windows, so this is the one thing that makes it that flavour. Deep
        // that drifts bright is not a bug in some other file, it is Deep no longer being Deep.
        for (flavour in ColorFlavour.entries) for (seed in seeds()) {
            val v = hsv(expressiveColorsFrom(coverOf(seed), flavour).container)[2]
            assertTrue(
                "$flavour container value $v outside ${flavour.valRange}",
                v >= flavour.valRange.start - 0.02f && v <= flavour.valRange.endInclusive + 0.02f,
            )
        }
    }

    @Test
    fun `the flavours are actually different from one another`() {
        // Guards against a windows edit that quietly collapses two of them onto the same palette —
        // which is the failure that made the cover-shape shuffle look broken, in another costume.
        val mid = fromHsv(210f, 0.7f, 0.7f)
        val containers = ColorFlavour.entries.map { expressiveColorsFrom(coverOf(mid), it).container }
        assertEquals("every flavour distinct", containers.size, containers.distinct().size)
    }

    @Test
    fun `Pastel is light and the others are not`() {
        // The point of Pastel is a light room. If the ink rules quietly darken it back down it still
        // passes every contrast check while being pointless.
        for (seed in seeds()) {
            val pastel = expressiveColorsFrom(coverOf(seed), ColorFlavour.PASTEL)
            assertTrue("Pastel container too dark", pastel.container.luminance() > 0.5f)
            // ...and its ink therefore has to be dark, which is the case white-on-white would fail.
            assertTrue("Pastel ink too light", pastel.onContainer.luminance() < 0.5f)

            val deep = expressiveColorsFrom(coverOf(seed), ColorFlavour.DEEP)
            assertTrue("Deep container too light", deep.container.luminance() < 0.35f)
        }
    }

    @Test
    fun `a colourless cover produces a colourless palette`() {
        // A black and white sleeve used to come out saturated: the extractor floored every
        // swatch to 0.42 saturation and the flavour floored it again into its own window, so the
        // hue being amplified was JPEG rounding noise. The answer for a grey cover is grey.
        val greys = listOf(0f, 0.12f, 0.35f, 0.5f, 0.78f, 1f).map { fromHsv(210f, 0f, it) }
        for (flavour in ColorFlavour.entries) for (grey in greys) {
            val c = expressiveColorsFrom(coverOf(grey).copy(monochrome = true), flavour)
            val where = "$flavour grey=${grey.value.toString(16)}"
            for ((name, colour) in listOf(
                "container" to c.container,
                "surface" to c.surface,
                "accent" to c.accent,
                "tertiary" to c.tertiary,
                "surfaceHigh" to c.surfaceHigh,
            )) {
                assertEquals("$where: $name has colour in it", 0f, hsv(colour)[1], 0.02f)
            }
        }
    }

    @Test
    fun `a colourless palette is still readable`() {
        // Removing the colour must not remove the contrast guarantee with it. Grey on grey is
        // exactly where a palette quietly stops being legible.
        val floor = ExpressiveColors.MIN_CONTRAST
        val greys = listOf(0f, 0.2f, 0.45f, 0.7f, 1f).map { fromHsv(0f, 0f, it) }
        for (flavour in ColorFlavour.entries) for (grey in greys) {
            val c = expressiveColorsFrom(coverOf(grey).copy(monochrome = true), flavour)
            val where = "$flavour grey=${grey.value.toString(16)}"
            assertTrue("$where: onContainer", contrastRatio(c.container, c.onContainer) >= floor)
            assertTrue("$where: onSurface", contrastRatio(c.surface, c.onSurface) >= floor)
            assertTrue("$where: onAccent", contrastRatio(c.accent, c.onAccent) >= floor)
            assertTrue("$where: accent vs container", contrastRatio(c.accent, c.container) >= floor)
        }
    }

    @Test
    fun `a cover with colour still gets colour`() {
        // The other half of the promise: the monochrome path must not swallow real covers.
        val c = expressiveColorsFrom(coverOf(fromHsv(280f, 0.7f, 0.6f)), ColorFlavour.SIGNATURE)
        assertTrue("container should be saturated", hsv(c.container)[1] > 0.3f)
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
        for (flavour in ColorFlavour.entries) for (seed in seeds()) {
            val c = expressiveColorsFrom(coverOf(seed), flavour)
            val s = expressiveColorScheme(c)
            val where = "$flavour seed=${seed.value.toString(16)}"

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
