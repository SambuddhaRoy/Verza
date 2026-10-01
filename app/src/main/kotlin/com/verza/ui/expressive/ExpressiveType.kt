package com.verza.ui.expressive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.verza.ui.theme.isPoster
import com.verza.ui.theme.FontPosterHead
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

// The Poster design's heads: Frick, one of the Uncut display faces, condensed and heavy. Upright and
// at its own single weight; asking it for italic or bold only gets a synthesised imitation. A touch
// larger than the serif, because a condensed face at the same size reads smaller.
private val PosterHeroDisplay = TextStyle(
    fontFamily = FontPosterHead,
    fontWeight = FontWeight.Normal,
    fontSize = 52.sp,
    lineHeight = 48.sp,
)

private val PosterHeroTitle = TextStyle(
    fontFamily = FontPosterHead,
    fontWeight = FontWeight.Normal,
    fontSize = 38.sp,
    lineHeight = 36.sp,
)

/** The track title, in the design's display voice. See [MaterialHeroDisplay]'s doc above. */
val HeroDisplay: TextStyle
    @Composable @ReadOnlyComposable get() = if (isPoster()) PosterHeroDisplay else MaterialHeroDisplay

/** The same voice one step down, for section heroes on Home and Library. */
val HeroTitle: TextStyle
    @Composable @ReadOnlyComposable get() = if (isPoster()) PosterHeroTitle else MaterialHeroTitle

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
