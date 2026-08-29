package com.verza.ui.expressive

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * A Material [ColorScheme] built from the expressive palette.
 *
 * Everything that reads MaterialTheme.colorScheme — which is most code the redesign has not directly
 * touched — gets colours that belong to the same palette as the background it is drawn on. Without
 * this there were two colour systems running at once: the expressive one painting the canvas, and
 * the old theme deciding what colour the text on it should be, with nothing keeping them in
 * agreement. Text landing the same colour as its background is exactly what that looks like.
 *
 * Every on-colour here comes from the palette's measured pairs rather than being picked by eye, so
 * the contrast guarantee reaches the untouched screens too.
 */
fun expressiveColorScheme(c: ExpressiveColors): ColorScheme = darkColorScheme(
    primary = c.accent,
    onPrimary = c.onAccent,
    // Measured, not inherited: this container is lerped toward the accent, which moved it far
    // enough that the canvas ink stopped clearing the floor on it (caught at 3.87:1).
    primaryContainer = lerp(c.container, c.accent, 0.28f),
    onPrimaryContainer = readableOn(lerp(c.container, c.accent, 0.28f)),

    secondary = c.tertiary,
    onSecondary = c.onTertiary,
    secondaryContainer = c.surfaceHigh,
    onSecondaryContainer = readableOn(c.surfaceHigh),

    tertiary = c.tertiary,
    onTertiary = c.onTertiary,
    tertiaryContainer = c.surfaceHigh,
    onTertiaryContainer = readableOn(c.surfaceHigh),

    background = c.container,
    onBackground = c.onContainer,
    surface = c.container,
    onSurface = c.onContainer,
    surfaceVariant = c.surface,
    // onContainerMuted is measured against the *container*, so it does not necessarily clear on the
    // surface tone. onSurfaceMuted is the one measured against this exact background.
    onSurfaceVariant = c.onSurfaceMuted,

    surfaceContainerLowest = c.surfaceLow,
    surfaceContainerLow = c.surfaceLow,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surfaceHigh,
    surfaceContainerHighest = c.surfaceHighest,

    outline = c.onContainerMuted,
    outlineVariant = c.line,
    scrim = Color.Black,

    inverseSurface = c.onContainer,
    inverseOnSurface = c.container,
    inversePrimary = c.accent,
)
