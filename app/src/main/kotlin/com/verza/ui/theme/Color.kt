package com.verza.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Extended Verza-specific color roles ────────────────────────────────────────
// These supplement M3 ColorScheme with roles the HTML defines that have no
// direct M3 equivalent: glass surfaces, muted text, decorative c2/c3 accents.
data class VerzaExtendedColors(
    val muted: Color,
    val glass: Color,
    val glassHeavy: Color,
    val borderGlass: Color,
    val brutalBlock: Color,
    val c2: Color,
    val c3: Color,
)

val LocalVerzaExtendedColors = staticCompositionLocalOf { NoirExtendedColors }

// ══════════════════════════════════════════════════════════════════════════════
// Color values below are hand-computed sRGB approximations of CSS OKLCH values
// from the reference HTML. Wide-gamut displays may render them slightly more
// vivid than listed here.
// ══════════════════════════════════════════════════════════════════════════════

val NoirSecondary            = Color(0xFF7050E0) // oklch(55% 0.2 290)  purple
val NoirTertiary             = Color(0xFFCF3868) // oklch(60% 0.22 350) pink
val NoirMuted                = Color(0xFF6E7080)
val NoirGlass                = Color(0x8C141620)
val NoirGlassHeavy           = Color(0xD1101218)
val NoirBorderGlass          = Color(0x2EE0E2E8)
val NoirBrutalBlock          = Color(0x0AE0E2E8)

val NoirExtendedColors = VerzaExtendedColors(
    muted = NoirMuted,
    glass = NoirGlass,
    glassHeavy = NoirGlassHeavy,
    borderGlass = NoirBorderGlass,
    brutalBlock = NoirBrutalBlock,
    c2 = NoirSecondary,
    c3 = NoirTertiary,
)

