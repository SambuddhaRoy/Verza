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

/** True when this device can produce dynamic (Material You) color schemes. */
val DynamicColorSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S


// ── ColorScheme builders ───────────────────────────────────────────────────────

// ── Composable ────────────────────────────────────────────────────────────────

@Composable
fun VerzaTheme(
    scheme: ColorScheme,
    content: @Composable () -> Unit,
) {
    // One scheme, always the cover's.
    //
    // This used to pick between a dozen fixed palettes and only honour the cover-derived scheme when
    // the stored theme happened to be ADAPTIVE. That is why screens which had not been converted
    // kept drawing in the old colours no matter what the canvas behind them did, and why launching
    // stepped through schemes as the stored theme arrived from disk. The flavours replaced the fixed
    // palettes, and a flavour is a variation of the cover rather than an alternative to it, so
    // there is nothing left to choose between here.
    val extended: VerzaExtendedColors = scheme.deriveExtendedColors()

    CompositionLocalProvider(LocalVerzaExtendedColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
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
