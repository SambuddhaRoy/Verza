package com.verza.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One radius, everywhere: covers, cards, buttons, chips, fields, sheets, dialogs, the dock. Anything
 * smaller than twice this comes out as a pill or a circle on its own, so a progress bar or a dot needs
 * no exception. [com.verza.ui.expressive.CornerConsistencyTest] fails on a corner written any other way.
 */
val VerzaCorner: Dp = 12.dp

val CornerShape: Shape = RoundedCornerShape(VerzaCorner)

/** A card. */
val VerzaShape: Shape get() = CornerShape

/** Material's own components: menus, snackbars, fields, sheets, dialogs. */
val VerzaShapes = Shapes(
    extraSmall = RoundedCornerShape(VerzaCorner),
    small = RoundedCornerShape(VerzaCorner),
    medium = RoundedCornerShape(VerzaCorner),
    large = RoundedCornerShape(VerzaCorner),
    extraLarge = RoundedCornerShape(VerzaCorner),
)

val GlassShapes = VerzaShapes
