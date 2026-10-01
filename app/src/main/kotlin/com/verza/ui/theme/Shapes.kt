package com.verza.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Verza uses a **single corner radius** for every rounded rectangle in the app — cards,
 * thumbnails, sheets, dialogs, buttons, chips and surfaces all share [VerzaCorner] so the UI
 * reads as one consistent system. Genuinely circular elements (icon buttons, avatars, the play
 * control) stay circular; everything else curves by exactly this much.
 *
 * In the Poster design (see [DesignScheme]) none of it curves at all: [VerzaShape] is a rectangle.
 */
val VerzaCorner: Dp = 14.dp

private val RoundedVerza = RoundedCornerShape(VerzaCorner)

/** The one rounded-rectangle shape, derived from [VerzaCorner]. Use this anywhere you'd reach for
 *  `RoundedCornerShape(...)` on a surface/card/button. Square in the Poster design. */
val VerzaShape: Shape
    @Composable @ReadOnlyComposable get() = squareOr(RoundedVerza)

/** Every Material shape slot maps to the single [VerzaShape], so themed components (cards, sheets,
 *  dialogs, menus) are uniform with our hand-styled surfaces. */
val VerzaShapes = Shapes(
    extraSmall = RoundedVerza,
    small = RoundedVerza,
    medium = RoundedVerza,
    large = RoundedVerza,
    extraLarge = RoundedVerza,
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
