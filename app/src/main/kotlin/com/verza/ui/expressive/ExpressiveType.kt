package com.verza.ui.expressive

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
val HeroDisplay = TextStyle(
    fontFamily = FontHero,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 64.sp,
    lineHeight = 58.sp,
    letterSpacing = (-0.02).em,
)

/** The same voice one step down, for section heroes on Home and Library. */
val HeroTitle = TextStyle(
    fontFamily = FontHero,
    fontStyle = FontStyle.Italic,
    fontWeight = FontWeight.Normal,
    fontSize = 40.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.015).em,
)

/** Upright serif, for card titles that want the editorial voice without the slant. */
val SerifTitle = TextStyle(
    fontFamily = FontHero,
    fontWeight = FontWeight.Normal,
    fontSize = 26.sp,
    lineHeight = 28.sp,
)

/** Big tabular numerals — durations, counts, the "07 30" register in the reference. */
val NumeralLarge = TextStyle(
    fontFamily = FontMono,
    fontWeight = FontWeight.Medium,
    fontSize = 40.sp,
    lineHeight = 42.sp,
    letterSpacing = (-0.03).em,
    fontFeatureSettings = "tnum",
)

/** Label/value metadata, the "Where / Echo Bridge" columns. */
val MetaLabel = TextStyle(
    fontFamily = FontMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.04.em,
)

val MetaValue = TextStyle(
    fontFamily = FontMono,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp,
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
