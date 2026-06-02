package com.verza.ui.sleeve

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

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
 * A translucent editorial surface for cards and chrome in Sleeve mode. Deliberately low-opacity so
 * the reactive glow behind it shows through — frosted "smoked-glass" with a faint light edge,
 * sitting on the dark Sleeve canvas.
 */
fun Modifier.sleeveSurface(shape: Shape): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = 0.05f))
    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape)
