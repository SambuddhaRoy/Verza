package com.verza.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.verza.R

// ── Bundled fonts (no Google Play Services dependency) ───────────────────────────
// All four families ship as OFL-licensed variable fonts in res/font, so the app pulls
// nothing from the proprietary `com.google.android.gms.fonts` provider — a hard
// requirement for F-Droid / IzzyOnDroid inclusion. A single variable file covers every
// weight we reach for; FontVariation.Settings drives the 'wght' axis per declared weight.
// (minSdk 26 ⇒ font variation axes are honoured on every supported device.)

/** A variable-font [Font] that pins the weight axis so one file serves many weights. */
@OptIn(ExperimentalTextApi::class)
private fun varFont(resId: Int, weight: FontWeight, style: FontStyle = FontStyle.Normal) =
    Font(
        resId,
        weight = weight,
        style = style,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

// Cormorant Garamond — heavier editorial serif for the big, bold, sophisticated voice on
// display/headline/title text. Regular/Medium/SemiBold/Bold upright + Regular/SemiBold italic
// for the serif "deck" captions, all from the one variable file (+ its italic companion).
val FontDisplay = FontFamily(
    varFont(R.font.cormorant_garamond_variable, FontWeight.Normal),
    varFont(R.font.cormorant_garamond_variable, FontWeight.Medium),
    varFont(R.font.cormorant_garamond_variable, FontWeight.SemiBold),
    varFont(R.font.cormorant_garamond_variable, FontWeight.Bold),
    varFont(R.font.cormorant_garamond_italic_variable, FontWeight.Normal, FontStyle.Italic),
    varFont(R.font.cormorant_garamond_italic_variable, FontWeight.SemiBold, FontStyle.Italic),
)

// Inter — clean grotesque sans for body/label (the Söhne stand-in).
val FontBody = FontFamily(
    varFont(R.font.inter_variable, FontWeight.Normal),
    varFont(R.font.inter_variable, FontWeight.Medium),
    varFont(R.font.inter_variable, FontWeight.SemiBold),
)

// Newsreader — the Sleeve appearance's serif, exactly matching the UMBRA reference's lead
// direction ("Terracotta"), which sets *every* serif at weight 400 (regular). The editorial,
// high-style voice there comes from large sizes + thin regular weight + tight tracking, NOT from
// bolding — so we deliberately keep Light/Regular variants and never reach for Bold in Sleeve.
// A literary, newspaper-ish serif with a softer, warmer voice than Cormorant.
val FontSleeve = FontFamily(
    varFont(R.font.newsreader_variable, FontWeight.Light),
    varFont(R.font.newsreader_variable, FontWeight.Normal),
    varFont(R.font.newsreader_variable, FontWeight.Medium),
    // Bold is reserved for deliberate emphasis (e.g. the currently-playing track in the queue),
    // not the general UI voice — which stays at weight 400 per the reference.
    varFont(R.font.newsreader_variable, FontWeight.Bold),
    varFont(R.font.newsreader_italic_variable, FontWeight.Light, FontStyle.Italic),
    varFont(R.font.newsreader_italic_variable, FontWeight.Normal, FontStyle.Italic),
    varFont(R.font.newsreader_italic_variable, FontWeight.Medium, FontStyle.Italic),
)

// IBM Plex Mono — reserved for numeric / timecode chrome (kept for accent details).
// Static instances (Plex Mono ships no variable font); Regular + Medium are all we use.
val FontMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

// ── Typography ─────────────────────────────────────────────────────────────────
// Display/Headline/Title → serif (Playfair). Body/Label → sans (Inter).
// Tight, optical letter-spacing on the serif; clean defaults on Inter.

// Tabular numerals — applied to any text style that frequently contains numbers
// (durations, kbps, version codes). Keeps "1:24" and "3:09" the same width so
// progress bars and time chips don't jitter as the seconds tick over.
private const val FEAT_TABULAR = "tnum"

val VerzaTypography = Typography(
    // Display + headline + titleLarge are all Bold in Cormorant Garamond. The font's
    // bold weight reads as architectural rather than shouty — slightly larger sizes
    // are forgiving of the heavier stroke without feeling crammed.
    displayLarge = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Bold,
        fontSize = 52.sp, lineHeight = 56.sp, letterSpacing = (-0.025).sp,
        fontFeatureSettings = FEAT_TABULAR,
    ),
    displayMedium = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.02).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.015).sp,
    ),

    headlineLarge = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.015).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.012).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.008).sp,
    ),

    // titleLarge keeps SemiBold — the credit line ("Sambuddha Roy") and similar
    // attribution-style text shouldn't read as heavy as a page headline.
    titleLarge = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.005).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.sp,
        fontFeatureSettings = FEAT_TABULAR,
    ),

    labelLarge = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.sp,
        fontFeatureSettings = FEAT_TABULAR,
    ),
    // Reserved for the eyebrow accents ("Good evening", section labels) — small, slightly tracked.
    labelSmall = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.08.sp,
        fontFeatureSettings = FEAT_TABULAR,
    ),
)

/**
 * Sleeve-appearance typography — a faithful port of the UMBRA "Terracotta" reference. Every serif
 * slot is **Newsreader at weight 400 (regular)** with tight, optical em-tracking and near-1.0
 * line-heights, so the look comes from scale + thin weight rather than bolding. The serif now
 * reaches down through the *title* slots too (track titles, card titles, list rows) so enabling
 * Sleeve re-sets the whole app as editorial type in one move. Body / label slots keep Inter;
 * numeric chrome uses [FontMono] at point-of-use.
 */
private fun sleeveSerif(size: Int, line: Int, track: Float) = TextStyle(
    fontFamily = FontSleeve, fontWeight = FontWeight.Normal,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = track.em,
)

val VerzaSleeveTypography = VerzaTypography.copy(
    displayLarge  = sleeveSerif(54, 54, -0.020f),
    displayMedium = sleeveSerif(42, 44, -0.020f),
    displaySmall  = sleeveSerif(33, 36, -0.018f),
    headlineLarge  = sleeveSerif(30, 32, -0.016f),
    headlineMedium = sleeveSerif(26, 29, -0.014f),
    headlineSmall  = sleeveSerif(22, 26, -0.012f),
    titleLarge  = sleeveSerif(22, 26, -0.010f),
    // Track / card / list titles become editorial serif as well (reference sets these at ~21/15px,
    // weight 400). Kept a touch larger than the Inter originals to carry the serif gracefully.
    titleMedium = sleeveSerif(18, 23, -0.010f),
    titleSmall  = sleeveSerif(16, 20, -0.008f),
)

// ── Editorial extras (used directly via the style refs below) ─────────────────
// These don't belong in the M3 Typography slots — they're used at point-of-use
// in screens that want the editorial italic voice or tabular monospace timecode.

/** Serif italic, the "deck" voice for short captions and descriptive subtitles. */
val CaptionItalic = TextStyle(
    fontFamily = FontDisplay,
    fontWeight = FontWeight.Normal,
    fontStyle = FontStyle.Italic,
    fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp,
)

/** Monospace timecode — for the Now Playing position/duration readout. */
val MonoTimecode = TextStyle(
    fontFamily = FontMono,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp, lineHeight = 16.sp,
    fontFeatureSettings = FEAT_TABULAR,
)

/** Small-caps-feel eyebrow label — used by [EditorialSectionHeader]. */
val EditorialEyebrow = TextStyle(
    fontFamily = FontBody, fontWeight = FontWeight.Medium,
    fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 1.6.sp,
)
