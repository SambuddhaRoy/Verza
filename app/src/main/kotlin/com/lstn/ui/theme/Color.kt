package com.lstn.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Extended LSTN-specific color roles ────────────────────────────────────────
// These supplement M3 ColorScheme with roles the HTML defines that have no
// direct M3 equivalent: glass surfaces, muted text, decorative c2/c3 accents.
data class LstnExtendedColors(
    val muted: Color,
    val glass: Color,
    val glassHeavy: Color,
    val borderGlass: Color,
    val brutalBlock: Color,
    val c2: Color,
    val c3: Color,
)

val LocalLstnExtendedColors = staticCompositionLocalOf { NoirExtendedColors }

// ══════════════════════════════════════════════════════════════════════════════
// Color values below are hand-computed sRGB approximations of CSS OKLCH values
// from the reference HTML. Wide-gamut displays may render them slightly more
// vivid than listed here.
// ══════════════════════════════════════════════════════════════════════════════

// ── BAUHAUS — warm paper, light ───────────────────────────────────────────────
// c1 oklch(50% 0.22 30), c2 oklch(55% 0.18 250), c3 oklch(70% 0.18 85)
val BauhausPrimary           = Color(0xFFC03918) // oklch(50% 0.22 30)  red-orange
val BauhausSecondary         = Color(0xFF3B6FC8) // oklch(55% 0.18 250) blue
val BauhausTertiary          = Color(0xFF8A7A00) // oklch(70% 0.18 85)  gold
val BauhausBackground        = Color(0xFFF0EDE8)
val BauhausOnBackground      = Color(0xFF201E1A)
val BauhausSurface           = Color(0xFFF5F2ED)
val BauhausSurfaceVariant    = Color(0xFFEDE9E3)
val BauhausOnSurface         = Color(0xFF201E1A)
val BauhausOnSurfaceVariant  = Color(0xFF726A60) // muted oklch(46% 0.015 80)
val BauhausOutline           = Color(0xFFCFC9C1)
val BauhausOutlineVariant    = Color(0xFFE5E1DB)
val BauhausMuted             = Color(0xFF726A60)
val BauhausGlass             = Color(0x80F5F2ED) // 50% opacity surface
val BauhausGlassHeavy        = Color(0xD9EDE9E3) // 85% opacity surface-variant
val BauhausBorderGlass       = Color(0x1F201E1A) // 12% fg
val BauhausBrutalBlock       = Color(0x0F201E1A) // 6% fg

// ── MALIBU — warm sand, light ─────────────────────────────────────────────────
// c1 oklch(58% 0.22 350), c2 oklch(60% 0.18 200), c3 oklch(70% 0.2 75)
val MalibuPrimary            = Color(0xFFD0258C) // oklch(58% 0.22 350) rose-pink
val MalibuSecondary          = Color(0xFF0099CC) // oklch(60% 0.18 200) cyan-blue
val MalibuTertiary           = Color(0xFF8CAE00) // oklch(70% 0.2 75)   warm yellow
val MalibuBackground         = Color(0xFFF5F0EB)
val MalibuOnBackground       = Color(0xFF251E18)
val MalibuSurface            = Color(0xFFF9F5F0)
val MalibuSurfaceVariant     = Color(0xFFEDE8E0)
val MalibuOnSurface          = Color(0xFF251E18)
val MalibuOnSurfaceVariant   = Color(0xFF766059) // muted oklch(48% 0.02 30)
val MalibuOutline            = Color(0xFFD6CFCA)
val MalibuOutlineVariant     = Color(0xFFE8E2DC)
val MalibuMuted              = Color(0xFF766059)
val MalibuGlass              = Color(0x80F9F5F0)
val MalibuGlassHeavy         = Color(0xD9EDE8E0)
val MalibuBorderGlass        = Color(0x1F251E18)
val MalibuBrutalBlock        = Color(0x0F251E18)

// ── CONCRETE — warm gray, light ───────────────────────────────────────────────
// c1 oklch(45% 0.18 28), c2 oklch(50% 0.12 240), c3 oklch(50% 0.15 120)
val ConcretePrimary          = Color(0xFFA2080C) // oklch(45% 0.18 28)  dark red
val ConcreteSecondary        = Color(0xFF4A7AB0) // oklch(50% 0.12 240) muted blue
val ConcreteTertiary         = Color(0xFF5A8A28) // oklch(50% 0.15 120) olive
val ConcreteBackground       = Color(0xFFE8E4DE)
val ConcreteOnBackground     = Color(0xFF1F1D19)
val ConcreteSurface          = Color(0xFFF2EEE8)
val ConcreteSurfaceVariant   = Color(0xFFEAE6E0)
val ConcreteOnSurface        = Color(0xFF1F1D19)
val ConcreteOnSurfaceVariant = Color(0xFF6F6860) // muted oklch(45% 0.015 60)
val ConcreteOutline          = Color(0xFFCBC5BD)
val ConcreteOutlineVariant   = Color(0xFFDDD9D3)
val ConcreteMuted            = Color(0xFF6F6860)
val ConcreteGlass            = Color(0x80F2EEE8)
val ConcreteGlassHeavy       = Color(0xD9EAE6E0)
val ConcreteBorderGlass      = Color(0x1F1F1D19)
val ConcreteBrutalBlock      = Color(0x0F1F1D19)

// ── NOIR — dark blue, dark ────────────────────────────────────────────────────
// c1 oklch(65% 0.18 190), c2 oklch(55% 0.2 290), c3 oklch(60% 0.22 350)
val NoirPrimary              = Color(0xFF00AFA7) // oklch(65% 0.18 190) teal
val NoirSecondary            = Color(0xFF7050E0) // oklch(55% 0.2 290)  purple
val NoirTertiary             = Color(0xFFCF3868) // oklch(60% 0.22 350) pink
val NoirBackground           = Color(0xFF090910)
val NoirOnBackground         = Color(0xFFE0E2E8)
val NoirSurface              = Color(0xFF141620)
val NoirSurfaceVariant       = Color(0xFF1C1E2C)
val NoirOnSurface            = Color(0xFFE0E2E8)
val NoirOnSurfaceVariant     = Color(0xFF6E7080) // muted oklch(48% 0.015 250)
val NoirOutline              = Color(0xFF404258)
val NoirOutlineVariant       = Color(0xFF2A2C3C)
val NoirMuted                = Color(0xFF6E7080)
val NoirGlass                = Color(0x8C141620)
val NoirGlassHeavy           = Color(0xD1101218)
val NoirBorderGlass          = Color(0x2EE0E2E8)
val NoirBrutalBlock          = Color(0x0AE0E2E8)

val NoirExtendedColors = LstnExtendedColors(
    muted = NoirMuted,
    glass = NoirGlass,
    glassHeavy = NoirGlassHeavy,
    borderGlass = NoirBorderGlass,
    brutalBlock = NoirBrutalBlock,
    c2 = NoirSecondary,
    c3 = NoirTertiary,
)

// ── EMBER — warm fire, dark ───────────────────────────────────────────────────
// c1 oklch(58% 0.22 25), c2 oklch(45% 0.2 20), c3 oklch(65% 0.2 75)
val EmberPrimary             = Color(0xFFDF202E) // oklch(58% 0.22 25)  red-orange
val EmberSecondary           = Color(0xFFA03010) // oklch(45% 0.2 20)   dark red
val EmberTertiary            = Color(0xFFB09800) // oklch(65% 0.2 75)   amber
val EmberBackground          = Color(0xFF0F0905)
val EmberOnBackground        = Color(0xFFEDE6E1)
val EmberSurface             = Color(0xFF1E1410)
val EmberSurfaceVariant      = Color(0xFF251A14)
val EmberOnSurface           = Color(0xFFEDE6E1)
val EmberOnSurfaceVariant    = Color(0xFF78655E) // muted oklch(48% 0.018 25)
val EmberOutline             = Color(0xFF4A3830)
val EmberOutlineVariant      = Color(0xFF342620)
val EmberMuted               = Color(0xFF78655E)
val EmberGlass               = Color(0x8C1E1410)
val EmberGlassHeavy          = Color(0xD1180F0C)
val EmberBorderGlass         = Color(0x2EEDE6E1)
val EmberBrutalBlock         = Color(0x0AEDE6E1)

// ── ACID — neon green, dark ───────────────────────────────────────────────────
// c1 oklch(65% 0.22 135), c2 oklch(58% 0.18 240), c3 oklch(60% 0.22 350)
val AcidPrimary              = Color(0xFF4AA900) // oklch(65% 0.22 135) green
val AcidSecondary            = Color(0xFF3A88D4) // oklch(58% 0.18 240) blue
val AcidTertiary             = Color(0xFFCF3868) // oklch(60% 0.22 350) pink
val AcidBackground           = Color(0xFF08100A)
val AcidOnBackground         = Color(0xFFE2ECE2)
val AcidSurface              = Color(0xFF10180E)
val AcidSurfaceVariant       = Color(0xFF162016)
val AcidOnSurface            = Color(0xFFE2ECE2)
val AcidOnSurfaceVariant     = Color(0xFF5E7860) // muted oklch(48% 0.018 135)
val AcidOutline              = Color(0xFF304030)
val AcidOutlineVariant       = Color(0xFF1E2C1E)
val AcidMuted                = Color(0xFF5E7860)
val AcidGlass                = Color(0x8C10180E)
val AcidGlassHeavy           = Color(0xD10C1410)
val AcidBorderGlass          = Color(0x2EE2ECE2)
val AcidBrutalBlock          = Color(0x0AE2ECE2)

// ── MAGENTA — neon magenta, dark ──────────────────────────────────────────────
// c1 oklch(62% 0.24 330), c2 oklch(65% 0.18 190), c3 oklch(70% 0.2 85)
val MagentaPrimary           = Color(0xFFCF39C9) // oklch(62% 0.24 330) magenta
val MagentaSecondary         = Color(0xFF00AFA7) // oklch(65% 0.18 190) teal
val MagentaTertiary          = Color(0xFF8CB800) // oklch(70% 0.2 85)   yellow-green
val MagentaBackground        = Color(0xFF100810)
val MagentaOnBackground      = Color(0xFFECE2EC)
val MagentaSurface           = Color(0xFF1A1018)
val MagentaSurfaceVariant    = Color(0xFF221822)
val MagentaOnSurface         = Color(0xFFECE2EC)
val MagentaOnSurfaceVariant  = Color(0xFF7A5A78) // muted oklch(48% 0.018 330)
val MagentaOutline           = Color(0xFF4A2E48)
val MagentaOutlineVariant    = Color(0xFF30203C)
val MagentaMuted             = Color(0xFF7A5A78)
val MagentaGlass             = Color(0x8C1A1018)
val MagentaGlassHeavy        = Color(0xD1140C18)
val MagentaBorderGlass       = Color(0x2EECE2EC)
val MagentaBrutalBlock       = Color(0x0AECE2EC)
