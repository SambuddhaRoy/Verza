package com.verza.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.verza.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

// Playfair Display — serif for display/headline/title (editorial feel).
private val playfairDisplay = GoogleFont("Playfair Display")
val FontDisplay = FontFamily(
    Font(googleFont = playfairDisplay, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = playfairDisplay, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = playfairDisplay, fontProvider = provider, weight = FontWeight.SemiBold),
)

// Inter — clean grotesque sans for body/label (the Söhne stand-in).
private val inter = GoogleFont("Inter")
val FontBody = FontFamily(
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = inter, fontProvider = provider, weight = FontWeight.SemiBold),
)

// IBM Plex Mono — reserved for numeric / timecode chrome (kept for accent details).
private val plexMono = GoogleFont("IBM Plex Mono")
val FontMono = FontFamily(
    Font(googleFont = plexMono, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = plexMono, fontProvider = provider, weight = FontWeight.Medium),
)

// ── Typography ─────────────────────────────────────────────────────────────────
// Display/Headline/Title → serif (Playfair). Body/Label → sans (Inter).
// Tight, optical letter-spacing on the serif; clean defaults on Inter.

val VerzaTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Normal,
        fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-0.02).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Normal,
        fontSize = 36.sp, lineHeight = 40.sp, letterSpacing = (-0.015).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.01).sp,
    ),

    headlineLarge = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Normal,
        fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.01).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Normal,
        fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.01).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Normal,
        fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.005).sp,
    ),

    titleLarge = TextStyle(
        fontFamily = FontDisplay, fontWeight = FontWeight.Normal,
        fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = (-0.005).sp,
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
    ),

    labelLarge = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.sp,
    ),
    // Reserved for the eyebrow accents ("Good evening", section labels) — small, slightly tracked.
    labelSmall = TextStyle(
        fontFamily = FontBody, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.08.sp,
    ),
)
