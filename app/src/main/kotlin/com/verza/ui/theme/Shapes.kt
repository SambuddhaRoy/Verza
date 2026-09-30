package com.verza.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

/**
 * Verza has no rounded edges.
 *
 * Every surface is a hard-cornered block: cards, thumbnails, sheets, dialogs, buttons, chips, the
 * play control, the artist photos. The look this is going for (GASS Records, the flat poster slab)
 * gets its energy from colour meeting colour at a straight line, and a corner radius, of any size,
 * is what turns a slab back into a component.
 *
 * [VerzaShape] stays as a name so call sites read as "the Verza surface" rather than as a
 * primitive, and so the day a single radius comes back it is one line.
 */
val VerzaShape = RectangleShape

/**
 * Every Material shape slot, square. This reaches sheets, dialogs, menus and cards. It does **not**
 * reach buttons, switches or slider thumbs, which Material hardwires to a full circle regardless of
 * the theme; those take an explicit shape at the call site.
 */
private val Square = RoundedCornerShape(0.dp)

val VerzaShapes = Shapes(
    extraSmall = Square,
    small = Square,
    medium = Square,
    large = Square,
    extraLarge = Square,
)
