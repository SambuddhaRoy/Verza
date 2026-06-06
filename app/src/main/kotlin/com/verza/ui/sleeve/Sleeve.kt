package com.verza.ui.sleeve

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.verza.ui.theme.LocalCoverColors
import com.verza.ui.theme.VerzaShape

/**
 * Sleeve is Verza's chosen editorial appearance: a poster-style Now Playing plus translucent,
 * cover-tinted surfaces app-wide, all floating over the live, album-coloured reactive glow.
 *
 * [LocalSleeveMode] gates the look. The full cover-sampled palette lives in
 * [com.verza.ui.theme.LocalCoverColors] (provided app-wide), so chrome and cards recolour to
 * whatever's playing.
 */
val LocalSleeveMode = staticCompositionLocalOf { false }

/**
 * A translucent editorial surface for cards and chrome in Sleeve mode — a faint wash of the
 * theme's ink over the reactive glow, with **no border**, so cards read as soft floating panels.
 * Using the ink colour (light on dark themes, dark on light themes) means the wash is *visible in
 * both light and dark Sleeve* — a flat white wash vanished on a light background. Shares [VerzaShape].
 */
@Composable
fun Modifier.sleeveSurface(shape: Shape = VerzaShape): Modifier {
    val cover = LocalCoverColors.current
    return this.clip(shape).background(cover.ink.copy(alpha = 0.07f))
}

/**
 * Tappable chrome (pills, chips, secondary action buttons) in Sleeve: a touch more present than
 * the cards so a button is discernible on its own without a hard outline. Ink-based so it works on
 * both light and dark Sleeve.
 */
@Composable
fun Modifier.sleeveButton(shape: Shape = VerzaShape): Modifier {
    val cover = LocalCoverColors.current
    return this.clip(shape).background(cover.ink.copy(alpha = 0.12f))
}
