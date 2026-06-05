package com.verza.ui.sleeve

import androidx.compose.foundation.background
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
 * A translucent editorial surface for cards and chrome in Sleeve mode — a faint light wash over
 * the reactive glow, with **no border**, so cards read as soft floating panels rather than
 * outlined boxes. All surfaces share [VerzaShape] for one consistent corner radius.
 */
fun Modifier.sleeveSurface(shape: Shape = VerzaShape): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = 0.06f))

/**
 * Tappable chrome (pills, chips, secondary action buttons) in Sleeve: transparent but a touch
 * lighter than both the background and the cards, so a button is discernible on its own without a
 * hard outline.
 */
fun Modifier.sleeveButton(shape: Shape = VerzaShape): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = 0.10f))
