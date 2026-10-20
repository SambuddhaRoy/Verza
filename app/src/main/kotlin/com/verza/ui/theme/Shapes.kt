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
 *
 * In the Poster design (see [DesignScheme]) none of it curves at all.
 */
val VerzaCorner: Dp = 20.dp

private val RoundedVerza = RoundedCornerShape(VerzaCorner)

/** A card: the 20dp of the scale above. Square in the Poster design. */
val VerzaShape: Shape
    @Composable @ReadOnlyComposable get() = squareOr(RoundedVerza)

/** Material's own components on the same scale: menus, snackbars and text fields at 12, cards at
 *  20, sheets and dialogs at 28. */
val VerzaShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedVerza,
    large = RoundedVerza,
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * The same slots, square, for the Poster design. This reaches sheets, dialogs, menus and cards.
 * It does not reach buttons, switches or slider thumbs, which Material hardwires to a full circle
 * whatever the theme says; those go through [squareOr] at the call site.
 *
 * A zero-radius RoundedCornerShape rather than RectangleShape because the slots are typed
 * CornerBasedShape; it draws the same.
 */
private val Square = RoundedCornerShape(0.dp)
val SquareShapes = Shapes(
    extraSmall = Square,
    small = Square,
    medium = Square,
    large = Square,
    extraLarge = Square,
)
