package com.verza.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The Muse shape scale — soft, rounded, editorial.
 * Replaces the previous brutalist RectangleShape everywhere through M3's shape system.
 *  extraSmall  — chips, small tags                    (8 dp)
 *  small       — list rows, thumbnails                (12 dp)
 *  medium      — cards, mini player, surfaces         (16 dp)
 *  large       — large cards, now-playing art         (24 dp)
 *  extraLarge  — full-bleed sheets / dialogs          (32 dp)
 */
val VerzaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
