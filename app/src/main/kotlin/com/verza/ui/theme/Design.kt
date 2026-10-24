package com.verza.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape

/**
 * Which design the app is drawn in.
 *
 * - **Material** is the app as it was built: rounded, soft, M3 Expressive shapes and springs, in
 *   the cover's colours.
 * - **Glass** is quiet, with the cover's colour in its accent and glass chrome (see [isGlass]).
 *
 * Presentation only. Glass rearranges Library, the bottom bars and Now Playing; no feature or
 * behaviour depends on the design.
 */
enum class DesignScheme(val displayName: String, val blurb: String, val experimental: Boolean = false) {
    MATERIAL("Material", "Rounded shapes and bold colour from the cover"),
    GLASS("Glass", "Clean and quiet, with glass controls"),
    ;

    companion object {
        /**
         * The stored design, or the default when there is none. Someone who finished onboarding
         * before the setting existed was using Material and keeps it; a new install starts in Glass.
         * Onboarding writes the design down when it finishes, so the default never flips under anyone.
         */
        fun fromName(name: String?, onboarded: Boolean = true): DesignScheme =
            entries.firstOrNull { it.name == name } ?: if (onboarded) MATERIAL else GLASS
    }
}

/**
 * Provided once, at the root, by MainActivity. Static because it changes about once in the life
 * of an install, and a change has to reach every shape in the tree anyway.
 */
val LocalDesign = staticCompositionLocalOf { DesignScheme.MATERIAL }

/**
 * The Glass design: black or white with the system, one accent from the cover for anything you
 * can press (glassColors), Inter at display size for headings, and the bottom bars, mini player and
 * Now Playing's buttons drawn as glass that blurs and bends what is behind it (LiquidGlass.kt).
 * The reference was Apple Music. On Now Playing the cover is the background.
 */
@Composable
@ReadOnlyComposable
fun isGlass(): Boolean = LocalDesign.current == DesignScheme.GLASS

/** [material] in Material, [glass] in Glass. */
@Composable
@ReadOnlyComposable
fun designShape(material: Shape, glass: Shape): Shape = if (isGlass()) glass else material
