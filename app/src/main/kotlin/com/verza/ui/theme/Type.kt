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

/**
 * A variable-font [Font] that pins the weight axis so one file serves many weights, and the optical
 * size axis ([opsz], 14 to 32 in Inter) to the size it will be read at.
 */
@OptIn(ExperimentalTextApi::class)
private fun varFont(resId: Int, weight: FontWeight, style: FontStyle = FontStyle.Normal, opsz: Float = 14f) =
    Font(
        resId,
        weight = weight,
        style = style,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight),
            FontVariation.Setting("opsz", opsz),
        ),
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

/**
 * The small-caps metadata voice: eyebrows, timecodes, counts, indices.
 *
 * This was IBM Plex Mono. A typewriter face at 9 to 12sp reads as technical rather than as
 * quiet, and beside Inter everywhere else it looked like a different app pasted in. It is Inter
 * now, with the letterspacing at each call site doing the work the monospacing used to.
 *
 * Digits stay steady without a monospaced face because the styles showing numbers ask for
 * tabular figures, which Inter has.
 */
val FontMono = FontBody

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

/** Small-caps-feel eyebrow label — used by [EditorialSectionHeader]. */
val EditorialEyebrow = TextStyle(
    fontFamily = FontBody, fontWeight = FontWeight.Medium,
    fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 1.6.sp,
)

/**
 * Inter's display cut: the same file at the far end of its optical-size axis, with tighter spacing
 * and finer detail, for type read at 20sp and up. Text sizes stay on [FontBody], whose cut is
 * opened up and sturdier. Optical sizes are what keep a large title from looking like body text
 * zoomed in, which is most of the difference between Inter and a system face that has them.
 */
val FontDisplayCut = FontFamily(
    varFont(R.font.inter_variable, FontWeight.Normal, opsz = 32f),
    varFont(R.font.inter_variable, FontWeight.Medium, opsz = 32f),
    varFont(R.font.inter_variable, FontWeight.SemiBold, opsz = 32f),
    varFont(R.font.inter_variable, FontWeight.Bold, opsz = 32f),
)

/** The Glass design's type: display, headline and the large title in the display cut. */
val GlassTypography: Typography = VerzaTypography.run {
    fun TextStyle.display() = copy(fontFamily = FontDisplayCut)
    copy(
        displayLarge = displayLarge.display(),
        displayMedium = displayMedium.display(),
        displaySmall = displaySmall.display(),
        headlineLarge = headlineLarge.display(),
        headlineMedium = headlineMedium.display(),
        headlineSmall = headlineSmall.display(),
        titleLarge = titleLarge.display(),
    )
}
