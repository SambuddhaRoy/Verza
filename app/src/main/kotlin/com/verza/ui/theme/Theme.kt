package com.verza.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class VerzaTheme(val displayName: String, val isLight: Boolean) {
    DYNAMIC ("Dynamic (Material You)", isLight = false),  // colors derived from the system wallpaper
    BAUHAUS ("Bauhaus",  isLight = true),
    MALIBU  ("Malibu",  isLight = true),
    CONCRETE("Concrete", isLight = true),
    NOIR    ("Noir",    isLight = false),
    EMBER   ("Ember",   isLight = false),
    ACID    ("Acid",    isLight = false),
    MAGENTA ("Magenta", isLight = false),
}

/** True when this device can produce dynamic (Material You) color schemes. */
val DynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

val LocalVerzaTheme = staticCompositionLocalOf { VerzaTheme.NOIR }

// ── ColorScheme builders ───────────────────────────────────────────────────────

fun VerzaTheme.toColorScheme(): ColorScheme = when (this) {
    // DYNAMIC is resolved at runtime inside [VerzaTheme]; this is just a swatch fallback.
    VerzaTheme.DYNAMIC -> VerzaTheme.NOIR.toColorScheme()
    VerzaTheme.BAUHAUS -> lightColorScheme(
        primary              = BauhausPrimary,
        onPrimary            = Color.White,
        primaryContainer     = Color(0xFFFFDDD6),
        onPrimaryContainer   = Color(0xFF3A0A00),
        secondary            = BauhausSecondary,
        onSecondary          = Color.White,
        secondaryContainer   = Color(0xFFD6E3FF),
        onSecondaryContainer = Color(0xFF001C46),
        tertiary             = BauhausTertiary,
        onTertiary           = Color.White,
        tertiaryContainer    = Color(0xFFEDE29A),
        onTertiaryContainer  = Color(0xFF211C00),
        background           = BauhausBackground,
        onBackground         = BauhausOnBackground,
        surface              = BauhausSurface,
        onSurface            = BauhausOnSurface,
        surfaceVariant       = BauhausSurfaceVariant,
        onSurfaceVariant     = BauhausOnSurfaceVariant,
        outline              = BauhausOutline,
        outlineVariant       = BauhausOutlineVariant,
        scrim                = Color.Black,
        inverseSurface       = Color(0xFF332F2B),
        inverseOnSurface     = Color(0xFFF8F5F0),
        inversePrimary       = Color(0xFFFFB4A0),
        error                = Color(0xFFBA1A1A),
        onError              = Color.White,
        errorContainer       = Color(0xFFFFDAD6),
        onErrorContainer     = Color(0xFF410002),
    )
    VerzaTheme.MALIBU -> lightColorScheme(
        primary              = MalibuPrimary,
        onPrimary            = Color.White,
        primaryContainer     = Color(0xFFFFD8EC),
        onPrimaryContainer   = Color(0xFF3E002A),
        secondary            = MalibuSecondary,
        onSecondary          = Color.White,
        secondaryContainer   = Color(0xFFBFE9FF),
        onSecondaryContainer = Color(0xFF001E2C),
        tertiary             = MalibuTertiary,
        onTertiary           = Color.White,
        tertiaryContainer    = Color(0xFFDCF0A0),
        onTertiaryContainer  = Color(0xFF1A2500),
        background           = MalibuBackground,
        onBackground         = MalibuOnBackground,
        surface              = MalibuSurface,
        onSurface            = MalibuOnSurface,
        surfaceVariant       = MalibuSurfaceVariant,
        onSurfaceVariant     = MalibuOnSurfaceVariant,
        outline              = MalibuOutline,
        outlineVariant       = MalibuOutlineVariant,
        scrim                = Color.Black,
        inverseSurface       = Color(0xFF3A2F28),
        inverseOnSurface     = Color(0xFFFAF0EA),
        inversePrimary       = Color(0xFFFFADD8),
        error                = Color(0xFFBA1A1A),
        onError              = Color.White,
        errorContainer       = Color(0xFFFFDAD6),
        onErrorContainer     = Color(0xFF410002),
    )
    VerzaTheme.CONCRETE -> lightColorScheme(
        primary              = ConcretePrimary,
        onPrimary            = Color.White,
        primaryContainer     = Color(0xFFFFDAD6),
        onPrimaryContainer   = Color(0xFF410002),
        secondary            = ConcreteSecondary,
        onSecondary          = Color.White,
        secondaryContainer   = Color(0xFFD1E4FF),
        onSecondaryContainer = Color(0xFF001C3B),
        tertiary             = ConcreteTertiary,
        onTertiary           = Color.White,
        tertiaryContainer    = Color(0xFFBEEDA0),
        onTertiaryContainer  = Color(0xFF0C2000),
        background           = ConcreteBackground,
        onBackground         = ConcreteOnBackground,
        surface              = ConcreteSurface,
        onSurface            = ConcreteOnSurface,
        surfaceVariant       = ConcreteSurfaceVariant,
        onSurfaceVariant     = ConcreteOnSurfaceVariant,
        outline              = ConcreteOutline,
        outlineVariant       = ConcreteOutlineVariant,
        scrim                = Color.Black,
        inverseSurface       = Color(0xFF34302C),
        inverseOnSurface     = Color(0xFFF5F0EC),
        inversePrimary       = Color(0xFFFFB4AC),
        error                = Color(0xFFBA1A1A),
        onError              = Color.White,
        errorContainer       = Color(0xFFFFDAD6),
        onErrorContainer     = Color(0xFF410002),
    )
    VerzaTheme.NOIR -> darkColorScheme(
        primary              = NoirPrimary,
        onPrimary            = Color.Black,
        primaryContainer     = Color(0xFF00504D),
        onPrimaryContainer   = Color(0xFF8CF5F0),
        secondary            = NoirSecondary,
        onSecondary          = Color.White,
        secondaryContainer   = Color(0xFF2A0D80),
        onSecondaryContainer = Color(0xFFCCB8FF),
        tertiary             = NoirTertiary,
        onTertiary           = Color.White,
        tertiaryContainer    = Color(0xFF70002E),
        onTertiaryContainer  = Color(0xFFFFB0C5),
        background           = NoirBackground,
        onBackground         = NoirOnBackground,
        surface              = NoirSurface,
        onSurface            = NoirOnSurface,
        surfaceVariant       = NoirSurfaceVariant,
        onSurfaceVariant     = NoirOnSurfaceVariant,
        outline              = NoirOutline,
        outlineVariant       = NoirOutlineVariant,
        scrim                = Color.Black,
        inverseSurface       = Color(0xFFE0E2E8),
        inverseOnSurface     = NoirBackground,
        inversePrimary       = Color(0xFF006B67),
        error                = Color(0xFFFFB4AB),
        onError              = Color(0xFF690005),
        errorContainer       = Color(0xFF93000A),
        onErrorContainer     = Color(0xFFFFDAD6),
    )
    VerzaTheme.EMBER -> darkColorScheme(
        primary              = EmberPrimary,
        onPrimary            = Color.Black,
        primaryContainer     = Color(0xFF5A0510),
        onPrimaryContainer   = Color(0xFFFFB3AE),
        secondary            = EmberSecondary,
        onSecondary          = Color.White,
        secondaryContainer   = Color(0xFF400C00),
        onSecondaryContainer = Color(0xFFFFB4A6),
        tertiary             = EmberTertiary,
        onTertiary           = Color.Black,
        tertiaryContainer    = Color(0xFF4A3C00),
        onTertiaryContainer  = Color(0xFFEEDA80),
        background           = EmberBackground,
        onBackground         = EmberOnBackground,
        surface              = EmberSurface,
        onSurface            = EmberOnSurface,
        surfaceVariant       = EmberSurfaceVariant,
        onSurfaceVariant     = EmberOnSurfaceVariant,
        outline              = EmberOutline,
        outlineVariant       = EmberOutlineVariant,
        scrim                = Color.Black,
        inverseSurface       = Color(0xFFEDE6E1),
        inverseOnSurface     = EmberBackground,
        inversePrimary       = Color(0xFF8A1020),
        error                = Color(0xFFFFB4AB),
        onError              = Color(0xFF690005),
        errorContainer       = Color(0xFF93000A),
        onErrorContainer     = Color(0xFFFFDAD6),
    )
    VerzaTheme.ACID -> darkColorScheme(
        primary              = AcidPrimary,
        onPrimary            = Color.Black,
        primaryContainer     = Color(0xFF1A4800),
        onPrimaryContainer   = Color(0xFF9EF078),
        secondary            = AcidSecondary,
        onSecondary          = Color.White,
        secondaryContainer   = Color(0xFF003B6A),
        onSecondaryContainer = Color(0xFFB0D4FF),
        tertiary             = AcidTertiary,
        onTertiary           = Color.White,
        tertiaryContainer    = Color(0xFF70002E),
        onTertiaryContainer  = Color(0xFFFFB0C5),
        background           = AcidBackground,
        onBackground         = AcidOnBackground,
        surface              = AcidSurface,
        onSurface            = AcidOnSurface,
        surfaceVariant       = AcidSurfaceVariant,
        onSurfaceVariant     = AcidOnSurfaceVariant,
        outline              = AcidOutline,
        outlineVariant       = AcidOutlineVariant,
        scrim                = Color.Black,
        inverseSurface       = Color(0xFFE2ECE2),
        inverseOnSurface     = AcidBackground,
        inversePrimary       = Color(0xFF286400),
        error                = Color(0xFFFFB4AB),
        onError              = Color(0xFF690005),
        errorContainer       = Color(0xFF93000A),
        onErrorContainer     = Color(0xFFFFDAD6),
    )
    VerzaTheme.MAGENTA -> darkColorScheme(
        primary              = MagentaPrimary,
        onPrimary            = Color.Black,
        primaryContainer     = Color(0xFF5A0058),
        onPrimaryContainer   = Color(0xFFFFADF8),
        secondary            = MagentaSecondary,
        onSecondary          = Color.Black,
        secondaryContainer   = Color(0xFF00504D),
        onSecondaryContainer = Color(0xFF8CF5F0),
        tertiary             = MagentaTertiary,
        onTertiary           = Color.Black,
        tertiaryContainer    = Color(0xFF3A4800),
        onTertiaryContainer  = Color(0xFFD6F380),
        background           = MagentaBackground,
        onBackground         = MagentaOnBackground,
        surface              = MagentaSurface,
        onSurface            = MagentaOnSurface,
        surfaceVariant       = MagentaSurfaceVariant,
        onSurfaceVariant     = MagentaOnSurfaceVariant,
        outline              = MagentaOutline,
        outlineVariant       = MagentaOutlineVariant,
        scrim                = Color.Black,
        inverseSurface       = Color(0xFFECE2EC),
        inverseOnSurface     = MagentaBackground,
        inversePrimary       = Color(0xFF7A0076),
        error                = Color(0xFFFFB4AB),
        onError              = Color(0xFF690005),
        errorContainer       = Color(0xFF93000A),
        onErrorContainer     = Color(0xFFFFDAD6),
    )
}

fun VerzaTheme.toExtendedColors(): VerzaExtendedColors = when (this) {
    VerzaTheme.DYNAMIC  -> VerzaTheme.NOIR.toExtendedColors() // overridden at runtime
    VerzaTheme.BAUHAUS  -> VerzaExtendedColors(BauhausMuted,   BauhausGlass,   BauhausGlassHeavy,   BauhausBorderGlass,   BauhausBrutalBlock,   BauhausSecondary,  BauhausTertiary)
    VerzaTheme.MALIBU   -> VerzaExtendedColors(MalibuMuted,    MalibuGlass,    MalibuGlassHeavy,    MalibuBorderGlass,    MalibuBrutalBlock,    MalibuSecondary,   MalibuTertiary)
    VerzaTheme.CONCRETE -> VerzaExtendedColors(ConcreteMuted,  ConcreteGlass,  ConcreteGlassHeavy,  ConcreteBorderGlass,  ConcreteBrutalBlock,  ConcreteSecondary, ConcreteTertiary)
    VerzaTheme.NOIR     -> VerzaExtendedColors(NoirMuted,      NoirGlass,      NoirGlassHeavy,      NoirBorderGlass,      NoirBrutalBlock,      NoirSecondary,     NoirTertiary)
    VerzaTheme.EMBER    -> VerzaExtendedColors(EmberMuted,     EmberGlass,     EmberGlassHeavy,     EmberBorderGlass,     EmberBrutalBlock,     EmberSecondary,    EmberTertiary)
    VerzaTheme.ACID     -> VerzaExtendedColors(AcidMuted,      AcidGlass,      AcidGlassHeavy,      AcidBorderGlass,      AcidBrutalBlock,      AcidSecondary,     AcidTertiary)
    VerzaTheme.MAGENTA  -> VerzaExtendedColors(MagentaMuted,   MagentaGlass,   MagentaGlassHeavy,   MagentaBorderGlass,   MagentaBrutalBlock,   MagentaSecondary,  MagentaTertiary)
}

// ── Composable ────────────────────────────────────────────────────────────────

@Composable
fun VerzaTheme(
    theme: VerzaTheme = VerzaTheme.NOIR,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()

    val colorScheme: ColorScheme = when {
        theme == VerzaTheme.DYNAMIC && DynamicColorSupported ->
            if (isSystemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        theme == VerzaTheme.DYNAMIC -> VerzaTheme.NOIR.toColorScheme() // older OS fallback
        else -> theme.toColorScheme()
    }

    // Derive the extended (Verza-specific) colors. For DYNAMIC we synthesise them from the
    // wallpaper-derived scheme so the whole UI stays on-palette.
    val extended: VerzaExtendedColors =
        if (theme == VerzaTheme.DYNAMIC) colorScheme.deriveExtendedColors()
        else theme.toExtendedColors()

    CompositionLocalProvider(
        LocalVerzaTheme provides theme,
        LocalVerzaExtendedColors provides extended,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VerzaTypography,
            shapes = VerzaShapes,
            content = content,
        )
    }
}

/** Maps an M3 ColorScheme onto our Verza extended palette (used by the Dynamic theme). */
private fun ColorScheme.deriveExtendedColors() = VerzaExtendedColors(
    muted = onSurfaceVariant,
    glass = surfaceVariant,
    glassHeavy = surface,
    borderGlass = outlineVariant,
    brutalBlock = primary,
    c2 = secondary,
    c3 = tertiary,
)
