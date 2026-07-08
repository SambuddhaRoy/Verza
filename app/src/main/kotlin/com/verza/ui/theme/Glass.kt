package com.verza.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The standard-appearance "liquid glass" panel — a translucent surface floating over the flowing
 * cover-art wash ([GlowStyle.COVER]), with a hairline border. This is the mobile translation of
 * the desktop `backdrop-filter` glass: NOT a real-time blur of the animated background (too costly
 * on a moving Compose layer), just a translucent panel colour so the wash supplies the colour +
 * motion, plus a 1 dp border so the edge reads. Drop-in like [com.verza.ui.sleeve.sleeveSurface].
 *
 * Colours come from the active theme's extended palette ([LocalVerzaExtendedColors]) so every theme
 * gets a coherent glass, and the default VERZA theme gets the neutral-black-over-wash desktop look.
 *
 * @param heavy use the near-opaque [VerzaExtendedColors.glassHeavy] for legibility-critical chrome
 *   (bottom nav, sheets); the default lighter glass lets more of the wash through (cards, mini-player).
 */
@Composable
fun Modifier.glassSurface(shape: Shape = VerzaShape, heavy: Boolean = false): Modifier {
    val ext = LocalVerzaExtendedColors.current
    return this
        .clip(shape)
        .background(if (heavy) ext.glassHeavy else ext.glass)
        .border(1.dp, ext.borderGlass, shape)
}
