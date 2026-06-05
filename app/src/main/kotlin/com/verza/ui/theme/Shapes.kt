package com.verza.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Verza uses a **single corner radius** for every rounded rectangle in the app — cards,
 * thumbnails, sheets, dialogs, buttons, chips and surfaces all share [VerzaCorner] so the UI
 * reads as one consistent system. Genuinely circular elements (icon buttons, avatars, the play
 * control) stay circular; everything else curves by exactly this much.
 */
val VerzaCorner: Dp = 16.dp

/** The one rounded-rectangle shape, derived from [VerzaCorner]. Use this anywhere you'd reach for
 *  `RoundedCornerShape(...)` on a surface/card/button. */
val VerzaShape = RoundedCornerShape(VerzaCorner)

/** Every Material shape slot maps to the single [VerzaShape], so themed components (cards, sheets,
 *  dialogs, menus) are uniform with our hand-styled surfaces. */
val VerzaShapes = Shapes(
    extraSmall = VerzaShape,
    small = VerzaShape,
    medium = VerzaShape,
    large = VerzaShape,
    extraLarge = VerzaShape,
)
