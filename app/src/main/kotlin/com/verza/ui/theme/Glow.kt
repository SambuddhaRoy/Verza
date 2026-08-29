package com.verza.ui.theme

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import com.verza.audio.VisualizerSignal
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * The live audio-reactive signal, app-wide. MainActivity provides the visualizer's band flow here
 * whenever reactivity is running (permission granted + playback active), so any composable — the
 * play button, the mini-player art, the Now Playing cover — can move with the music. Null when
 * the visualizer is off; consumers should treat that as a still signal.
 */
val LocalAudioSignal = staticCompositionLocalOf<StateFlow<VisualizerSignal>?> { null }

/**
 * User-selectable colour for the dark-theme background glow. The default WARM_AMBER
 * matches the Atelier accent; SYSTEM pulls from the device's Material You scheme on Android 12+.
 */
enum class GlowColorPreset(val displayName: String) {
    ALBUM_ART  ("From album art"),
    WARM_AMBER ("Warm amber"),
    HONEY      ("Honey"),
    EMBERS     ("Embers"),
    COOL_SLATE ("Cool slate"),
    FOREST     ("Forest"),
    SYSTEM     ("Use system color"),
}

/**
 * Three discrete intensity stops. [shaderStrength] is the multiplier fed into the fluid
 * shader's brightness — deliberately punchy (even SUBTLE is clearly visible) since the whole
 * point of the redesign was to make the effect read, not hide.
 */
enum class GlowIntensity(val displayName: String, val shaderStrength: Float) {
    SUBTLE ("Subtle", shaderStrength = 0.65f),
    MEDIUM ("Medium", shaderStrength = 0.92f),
    BOLD   ("Bold",   shaderStrength = 1.25f),
}

/**
 * The glow's visual pattern. [FLUID] is the flowing aurora field; [HALFTONE] re-renders it as a
 * drifting blob of colour in a sea of darkness — fine comic-print dots that wander and pulse across
 * the background, always present somewhere (at its smallest, a sliver in a corner). Pattern is a
 * shader feature, so it only applies on API 33+; the pre-33 gradient fallback always renders fluid.
 */
enum class GlowStyle(val displayName: String) {
    FLUID    ("Fluid"),
    HALFTONE ("Halftone"),
    COVER    ("Cover"),   // flowing, blurred, domain-warped wash of the current album art
}

// The glow renderer that used to live here is gone: the app canvas is a flat cover-derived colour
// painted at the root now, so nothing composed it. What is left is the audio signal every screen
// rides and the preference enums Settings still stores.

@Composable
fun GlowColorPreset.resolveColor(): Color {
    val context = LocalContext.current
    return when (this) {
        // ALBUM_ART has no fixed seed — its colours come from the cover at runtime. We return
        // the theme primary as the fallback seed used when no artwork is available.
        GlowColorPreset.ALBUM_ART  -> MaterialTheme.colorScheme.primary
        GlowColorPreset.WARM_AMBER -> Color(0xFFD67950)
        GlowColorPreset.HONEY      -> Color(0xFFE8B14A)
        GlowColorPreset.EMBERS     -> Color(0xFFB44520)
        GlowColorPreset.COOL_SLATE -> Color(0xFF6B8BA8)
        GlowColorPreset.FOREST     -> Color(0xFF5A8068)
        GlowColorPreset.SYSTEM ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                dynamicDarkColorScheme(context).primary
            else
                Color(0xFFD67950) // pre-S devices fall back to warm amber
    }
}
