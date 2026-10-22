package com.verza.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corners come from three radii and a pill (the tokens are in ExpressiveShapes.kt):
 * - **28dp** ([com.verza.ui.expressive.ShapeExtraLarge]): sheets, dialogs, the Now Playing cover,
 *   the mini player.
 * - **20dp** ([VerzaShape], `ShapeLargeIncreased`): cards and card rows, and any picture the size
 *   of a card.
 * - **12dp** (`ShapeMedium`): thumbnails inside rows, text fields, press highlights.
 * - **Pill**: buttons, chips, the nav bar. Circles for artists and icon buttons.
 *
 * Something inside a rounded container is concentric with it: its radius is the container's
 * minus the inset (a 20dp row with an 8dp inset holds a 12dp thumbnail). Different radii side by
 * side, or a thumbnail curving more tightly than its row, is what made the app look unmatched.
 */
val VerzaCorner: Dp = 20.dp

private val RoundedVerza = RoundedCornerShape(VerzaCorner)

/** A card: the 20dp of the scale above (14, continuous, in Glass). */
val VerzaShape: Shape
    @Composable @ReadOnlyComposable get() = designShape(RoundedVerza, com.verza.ui.expressive.SquircleShape(14.dp))

/** Material's own components on the same scale: menus, snackbars and text fields at 12, cards at
 *  20, sheets and dialogs at 28. */
val VerzaShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedVerza,
    large = RoundedVerza,
    extraLarge = RoundedCornerShape(28.dp),
)

/** Glass: menus and fields at 10, cards at 12, sheets and dialogs at 28. */
val GlassShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
