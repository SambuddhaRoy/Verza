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
// Inter (a clean modern grotesque) and IBM Plex Mono ship as OFL variable/static fonts in
// res/font, so the app pulls nothing from the proprietary `com.google.android.gms.fonts`
// provider — a requirement for F-Droid / IzzyOnDroid. The one variable Inter file covers
// every weight; FontVariation.Settings drives the 'wght' axis per declared weight.
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

// Inter — the app's single text typeface. A clean, highly readable modern sans used for
// everything: display, headline, title, body and label. (No serif anywhere — see the
// aliases below.) One variable file, four weights.
val FontBody = FontFamily(
    varFont(R.font.inter_variable, FontWeight.Normal),
    varFont(R.font.inter_variable, FontWeight.Medium),
    varFont(R.font.inter_variable, FontWeight.SemiBold),
    varFont(R.font.inter_variable, FontWeight.Bold),
)

// Display + Sleeve type are aliases of the sans now. Verza used an editorial serif (Cormorant /
// Newsreader) for headlines and the Sleeve appearance; that's been dropped in favour of the
// simpler, more readable modern sans. The names are kept so call-sites don't churn.
val FontDisplay = FontBody
val FontSleeve = FontBody

// IBM Plex Mono — reserved for numeric / timecode chrome (durations, indices, datelines).
// Static instances (Plex Mono ships no variable font); Regular + Medium are all we use.
val FontMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

// ── Typography ─────────────────────────────────────────────────────────────────
// Everything is set in Inter (a clean modern sans) — display, headline, title, body, label.
// Tight, optical letter-spacing at the larger sizes; clean defaults at text sizes.

// Tabular numerals — applied to any text style that frequently contains numbers
// (durations, kbps, version codes). Keeps "1:24" and "3:09" the same width so
// progress bars and time chips don't jitter as the seconds tick over.
private const val FEAT_TABULAR = "tnum"

val VerzaTypography = Typography(
    // Display + headline + titleLarge are Bold Inter — confident without the heavy serif stroke.
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
 * Sleeve-appearance typography. Sleeve keeps its cover-driven colours, poster layout, grain and
 * mono chrome, but its type is now the same modern sans as the rest of the app — set a touch larger
 * with tight, optical em-tracking and near-1.0 line-heights so titles still read as a confident,
 * poster-like display voice (just without the serif). Body / label slots keep Inter; numeric chrome
 * uses [FontMono] at point-of-use.
 */
private fun sleeveTitle(size: Int, line: Int, track: Float) = TextStyle(
    fontFamily = FontSleeve, fontWeight = FontWeight.Normal,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = track.em,
)

val VerzaSleeveTypography = VerzaTypography.copy(
    displayLarge  = sleeveTitle(54, 54, -0.020f),
    displayMedium = sleeveTitle(42, 44, -0.020f),
    displaySmall  = sleeveTitle(33, 36, -0.018f),
    headlineLarge  = sleeveTitle(30, 32, -0.016f),
    headlineMedium = sleeveTitle(26, 29, -0.014f),
    headlineSmall  = sleeveTitle(22, 26, -0.012f),
    titleLarge  = sleeveTitle(22, 26, -0.010f),
    // Track / card / list titles get the same display sans, a touch larger than the body originals.
    titleMedium = sleeveTitle(18, 23, -0.010f),
    titleSmall  = sleeveTitle(16, 20, -0.008f),
)

// ── Editorial extras (used directly via the style refs below) ─────────────────
// These don't belong in the M3 Typography slots — they're used at point-of-use
// in screens that want the editorial italic voice or tabular monospace timecode.

/** Italic "deck" voice for short captions and descriptive subtitles (sans italic). */
val CaptionItalic = TextStyle(
    fontFamily = FontBody,
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
