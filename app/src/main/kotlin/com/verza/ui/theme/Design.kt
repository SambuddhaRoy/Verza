package com.verza.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

/**
 * Which design the app is drawn in. Both take their colours from the cover; they differ in
 * everything else.
 *
 * - **Material** is the app as it was built: rounded, soft, M3 Expressive shapes and springs.
 * - **Poster** is flat printed slabs (GASS Records was the reference): no rounded edge anywhere,
 *   and a Now Playing that hides its controls and becomes a poster of the cover, its title set in a
 *   different display face for every song.
 *
 * Presentation only. No feature, screen or behaviour outside Now Playing depends on it.
 */
enum class DesignScheme(val displayName: String, val blurb: String, val experimental: Boolean = false) {
    MATERIAL("Material", "Rounded and soft, the way Android draws things"),
    POSTER("Poster", "Square edges, and Now Playing becomes a poster of the cover", experimental = true),
    ;

    companion object {
        fun fromName(name: String?): DesignScheme = entries.firstOrNull { it.name == name } ?: MATERIAL
    }
}

/**
 * Provided once, at the root, by MainActivity. Static because it changes about once in the life
 * of an install, and a change has to reach every shape in the tree anyway.
 */
val LocalDesign = staticCompositionLocalOf { DesignScheme.MATERIAL }

/**
 * Whether Home is the collage (Poster only): every cover packed edge to edge at a different size,
 * names set on the art. Off, Poster's Home is the usual rows of shelves, squared. Experimental, so
 * off unless asked for.
 */
val LocalHomeCollage = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
fun isPoster(): Boolean = LocalDesign.current == DesignScheme.POSTER

/**
 * [shape] in the Material design, a rectangle in the Poster one.
 *
 * Every rounded shape in the app goes through this, so the design switch is one decision rather
 * than one per screen. It has to be read in composition: a shape picked inside a draw or layout
 * lambda would not see the switch change. Read it in the composable and pass the result in.
 */
@Composable
@ReadOnlyComposable
fun squareOr(shape: Shape): Shape = if (isPoster()) RectangleShape else shape
