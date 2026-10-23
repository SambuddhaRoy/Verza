package com.verza.ui.expressive

import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.verza.ui.theme.isGlass
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.verza.R
import com.verza.ui.theme.FontBody
import com.verza.ui.theme.FontMono

/**
 * The expressive type voice.
 *
 * The reference does not set everything in one family. A hero name is a huge high-contrast italic
 * serif, metadata is monospaced in label/value columns, numbers are large and tabular, and body copy
 * is a plain grotesque. Mixing families deliberately is most of what separates the style from a
 * normal dark theme, and the app had been narrowed to Inter for everything.
 *
 * Instrument Serif (OFL, ~70KB per file) fills the display slot. Bundled rather than pulled from the
 * Google Fonts provider, because the app deliberately avoids `com.google.android.gms.fonts` so it
 * can ship on F-Droid — see the note in Type.kt.
 */
val FontHero = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic),
)

/**
 * The track title on Now Playing. Enormous, italic, and allowed to dominate — in the reference it is
 * the single largest thing on the screen by a wide margin.
 *
 * Line height is deliberately tighter than the font size: a two-line title should set as a block,
 * the way a poster would, rather than as two loosely stacked lines.
 */
private val MaterialHeroDisplay = TextStyle(
    fontFamily = FontHero,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 44.sp,
    lineHeight = 42.sp,
    letterSpacing = (-0.02).em,
)

private val MaterialHeroTitle = TextStyle(
    fontFamily = FontHero,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 32.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.015).em,
)

// The Glass design's heads: Inter's display cut at its heaviest declared weight, tracked in, the
// way a large title is set on a phone that is not trying to be a magazine.
private val GlassHeroDisplay = TextStyle(
    fontFamily = com.verza.ui.theme.FontDisplayCut,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 27.sp,
    letterSpacing = (-0.015).em,
)

private val GlassHeroTitle = TextStyle(
    fontFamily = com.verza.ui.theme.FontDisplayCut,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = (-0.025).em,
)

/**
 * Whether the decorative faces are on (Settings > Appearance > Expressive type): Instrument Serif for
 * Material's heads, a face of its own for every heading ([variedFace]) and every song title on Now
 * Playing. Off by default, which is Inter throughout; provided by MainActivity.
 */
val LocalExpressiveType = androidx.compose.runtime.staticCompositionLocalOf { false }

// Material's heads in Inter: the serif's sizes, set upright and heavy in the display cut.
private val PlainHeroDisplay = MaterialHeroDisplay.copy(
    fontFamily = com.verza.ui.theme.FontDisplayCut,
    fontStyle = FontStyle.Normal,
    fontWeight = FontWeight.Bold,
    lineHeight = 46.sp,
    letterSpacing = (-0.03).em,
)

private val PlainHeroTitle = MaterialHeroTitle.copy(
    fontFamily = com.verza.ui.theme.FontDisplayCut,
    fontStyle = FontStyle.Normal,
    fontWeight = FontWeight.Bold,
    lineHeight = 38.sp,
    letterSpacing = (-0.025).em,
)

/** The track title, in the design's display voice. See [MaterialHeroDisplay]'s doc above. */
val HeroDisplay: TextStyle
    @Composable @ReadOnlyComposable get() = when {
        isGlass() -> GlassHeroDisplay
        LocalExpressiveType.current -> MaterialHeroDisplay
        else -> PlainHeroDisplay
    }

/** The same voice one step down, for section heroes on Home and Library. */
val HeroTitle: TextStyle
    @Composable @ReadOnlyComposable get() = when {
        isGlass() -> GlassHeroTitle
        LocalExpressiveType.current -> MaterialHeroTitle
        else -> PlainHeroTitle
    }

/** A section heading in the Glass design: "Recently played ›". */
val GlassSectionTitle = TextStyle(
    fontFamily = com.verza.ui.theme.FontDisplayCut,
    fontWeight = FontWeight.Bold,
    fontSize = 21.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.012).em,
)

/** Label/value metadata, the "Where / Echo Bridge" columns. */
val MetaLabel = TextStyle(
    fontFamily = FontMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.04.em,
)

/** Timecode either side of the seek bar. */
val Timecode = TextStyle(
    fontFamily = FontMono,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 16.sp,
    fontFeatureSettings = "tnum",
)

/** The label inside a filled pill — "PLAY". Wide tracking, because it is set in caps. */
val PillLabel = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Medium,
    fontSize = 22.sp,
    lineHeight = 24.sp,
    letterSpacing = 0.06.em,
)

val BodyText = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 20.sp,
)

val BodyStrong = TextStyle(
    fontFamily = FontBody,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 21.sp,
)

/**
 * This style set in a face of its own: one of the legible display faces, chosen by [key] so a given
 * row or heading always looks the same, and different from its neighbours. In Material;
 * left as it is if no legible face can set all of [text], and always in Glass, which is one face.
 */
@Composable
fun TextStyle.variedFace(key: String, text: String = key): TextStyle {
    if (isGlass() || !LocalExpressiveType.current) return this
    val context = LocalContext.current
    val family = remember(key, text) { TitleFonts.legibleFor(context, key, text) } ?: return this
    return copy(fontFamily = family, fontWeight = FontWeight.Normal, fontStyle = FontStyle.Normal)
}
