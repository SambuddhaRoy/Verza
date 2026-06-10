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
 * The glow's visual pattern. [FLUID] is the flowing aurora field; [HALFTONE] re-renders that field as
 * a halftone-print swirl — spiral bands rotating around the glow's focus, screened through a rotated
 * dot grid whose dot sizes track the local brightness (the comic-print look). Pattern is a shader
 * feature, so it only applies on API 33+; the pre-33 gradient fallback always renders the fluid look.
 */
enum class GlowStyle(val displayName: String) {
    FLUID    ("Fluid"),
    HALFTONE ("Halftone"),
}

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

// ── AGSL fluid shader ──────────────────────────────────────────────────────────
// A flowing, domain-warped liquid colour field — the "synthesis"/aurora look. Value-noise
// fbm warps its own coordinate space (one warp pass) to get smooth liquid motion; a sine
// band on the final field gives the bright flowing ribbons. Three theme colours are mixed
// across the field so it adapts to whatever palette is active. Audio bands modulate the warp
// amount, brightness and colour balance. Concentrated toward the upper-centre and faded out
// at the edges so it reads as an ambient glow behind content rather than a fullscreen takeover.
private const val FLUID_SHADER_SRC = """
uniform float2 uResolution;
uniform float uTime;
uniform float uBass;
uniform float uMid;
uniform float uTreble;
uniform float uStrength;
uniform float uPattern;
uniform float3 uColorA;
uniform float3 uColorB;
uniform float3 uColorC;
uniform float3 uBg;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.0 + 7.3;
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / uResolution;
    float2 p = uv;
    p.x *= uResolution.x / uResolution.y;

    float t = uTime * 0.08;
    float bass = uBass;
    float mid = uMid;
    float treble = uTreble;

    // First-pass noise field, animated — the source of the flow.
    float2 q = float2(
        fbm(p * 1.5 + float2(0.0, t)),
        fbm(p * 1.5 + float2(5.2, t * 0.8))
    );

    // Warp the domain by q — this is what gives the liquid, folding motion. Bass deepens it.
    float warpAmt = 2.0 + bass * 2.6;
    float2 p2 = p + warpAmt * (q - 0.5);
    float f = fbm(p2 * 1.8 + float2(t * 0.6, -t * 0.4));

    // ── "Halftone" pattern (uPattern > 0.5): re-renders the field as a halftone-print swirl —
    //    logarithmic spiral bands rotating around the glow's upper-centre focus, screened through a
    //    rotated dot lattice whose dot sizes track the local band brightness (the comic-print look,
    //    after shaders.com's "Halftone Swirl"). The flow field q nudges the spiral so it stays
    //    organic, and bass deepens that wobble and swells the dots, so the print breathes with the
    //    music.
    if (uPattern > 0.5) {
        // Log-spiral band field. The 3-arm count must stay an integer so the sin() phase is
        // continuous across the atan() seam. l is floored well above zero so the spiral doesn't
        // alias into noise right at the focus.
        float2 d = p - float2(0.5 * uResolution.x / uResolution.y, 0.36)
                 + (q - 0.5) * (0.22 + 0.35 * bass);
        float l = max(length(d), 0.05);
        float spiral = 3.0 * atan(d.y, d.x) / 6.2831 - 1.4 * log(l) - t * 2.4;
        float spiralBand = 0.5 + 0.5 * sin(spiral * 6.2831);
        float fieldv = mix(spiralBand, f, 0.30);   // keep a share of the fluid inside the bands

        // Halftone screen: a ~26°-rotated dot lattice. Dot radius grows with sqrt(brightness) so
        // dot *area* tracks the field like real print; bass gives the dots a gentle swell.
        float2 hp = float2(0.8988 * p.x - 0.4384 * p.y, 0.4384 * p.x + 0.8988 * p.y) * 26.0;
        float2 cell = fract(hp) - 0.5;
        float r = 0.62 * sqrt(clamp(fieldv, 0.0, 1.0)) * (1.0 + 0.18 * bass);
        float dotMask = 1.0 - smoothstep(r - 0.09, r + 0.09, length(cell) * 2.0);
        f = dotMask * (0.4 + 0.6 * fieldv);
    }

    // Mix three theme colours across the warped field.
    float m1 = clamp(f * 1.6, 0.0, 1.0);
    float3 col = mix(uColorA, uColorB, m1);
    float m2 = clamp(length(q - 0.5) * 1.4 + treble * 0.5, 0.0, 1.0);
    col = mix(col, uColorC, m2);

    // Flowing bright ribbons — a sine band over the field, drifting with time + mids.
    float band = 0.5 + 0.5 * sin(6.2831 * f + t * 2.0 + mid * 3.0);
    float intensity = mix(0.55, 1.0, band);

    // Spatial shaping: brightest toward the upper-centre, fading down and out so content
    // lower on the screen stays legible.
    float fall = smoothstep(1.25, -0.1, uv.y);
    float vign = smoothstep(1.15, 0.25, distance(uv, float2(0.5, 0.32)));

    float amp = uStrength * (0.8 + 1.2 * bass + 0.4 * mid);
    float mask = clamp(f * intensity * fall * vign, 0.0, 1.0);

    float3 outc = mix(uBg, col, clamp(mask * amp, 0.0, 1.0));
    return half4(half3(outc), 1.0);
}
"""

/**
 * Renders the background glow behind app content.
 *
 *  - **API 33+**: a real AGSL [RuntimeShader] flowing-fluid field (see [FLUID_SHADER_SRC]).
 *  - **API 26–32**: an enhanced multi-radial-gradient fallback (no RuntimeShader available).
 *
 * The effect always animates with an ambient drift; when [signal] carries non-zero audio
 * energy (reactivity enabled + a song playing) the motion, brightness and colour balance
 * intensify with the music. No-op when [enabled] is false or the theme is light.
 */
@Composable
fun GlowBackground(
    enabled: Boolean,
    triad: GlowTriad,
    intensity: GlowIntensity,
    modifier: Modifier = Modifier,
    // The reactive signal is passed as a *flow*, not a value, and collected deep inside the glow
    // (below) — so the visualizer's ~30 Hz updates only re-draw the shader, never recompose the
    // whole app tree (which a value read at the call site would have triggered).
    signalFlow: StateFlow<VisualizerSignal>? = null,
    style: GlowStyle = GlowStyle.FLUID,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // The shader / gradient mix the saturated triad *toward* the background, which reads as gentle
    // colour washes on a light canvas just as it glows on a dark one — so we show it in both light
    // and dark schemes (it used to be hidden on light, which left light mode flat and bare).
    val show = enabled
    val bg = scheme.background

    Box(modifier = modifier.fillMaxSize()) {
        if (show) {
            // Build the RuntimeShader defensively — if the AGSL fails to compile on this device
            // (or we're below API 33), fall back to the gradient glow rather than crashing.
            val shader: RuntimeShader? = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    runCatching { RuntimeShader(FLUID_SHADER_SRC) }.getOrNull()
                else null
            }
            if (shader != null) {
                FluidShaderGlow(shader, triad.a, triad.b, triad.c, bg, intensity.shaderStrength, signalFlow, style == GlowStyle.HALFTONE)
            } else {
                GradientGlowFallback(triad, bg, intensity, signalFlow)
            }
        }
        content()
    }
}

/** Monotonic, continuously-increasing seconds since first frame — drives shader/gradient motion. */
@Composable
private fun rememberFrameTimeSeconds(): State<Float> {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var start = 0L
        while (true) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                seconds.floatValue = (now - start) / 1_000_000_000f
            }
        }
    }
    return seconds
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun FluidShaderGlow(
    shader: RuntimeShader,
    colorA: Color,
    colorB: Color,
    colorC: Color,
    bg: Color,
    strength: Float,
    signalFlow: StateFlow<VisualizerSignal>?,
    halftone: Boolean,
) {
    val brush = remember(shader) { ShaderBrush(shader) }
    val time by rememberFrameTimeSeconds()

    // Collected here (not at the call site) so the ~30 Hz signal only recomposes this composable.
    // One last smoothing layer on top of the AudioVisualizer's own filtering, so percussive
    // transients glide rather than snap.
    val safe = signalFlow?.collectAsState()?.value ?: VisualizerSignal()
    val bass by animateFloatAsState(safe.bass, tween(120), label = "fluidBass")
    val mid by animateFloatAsState(safe.mid, tween(160), label = "fluidMid")
    val treble by animateFloatAsState(safe.treble, tween(100), label = "fluidTreble")
    // Mood envelope: a slow (2.5 s) smoothing of overall energy. Drives the per-song "temperature"
    // — energetic tracks warm + brighten the field, calm ones cool + settle it. Only meaningful
    // when a live signal is present (reactivity / haptics running).
    val mood by animateFloatAsState(safe.energy, tween(2500), label = "fluidMood")

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val adaptedStrength = strength * (0.72f + 0.56f * mood)
                val cA = moodTint(colorA, mood)
                val cB = moodTint(colorB, mood)
                val cC = moodTint(colorC, mood)
                shader.setFloatUniform("uResolution", size.width, size.height)
                shader.setFloatUniform("uTime", time)
                shader.setFloatUniform("uBass", bass)
                shader.setFloatUniform("uMid", mid)
                shader.setFloatUniform("uTreble", treble)
                shader.setFloatUniform("uStrength", adaptedStrength)
                shader.setFloatUniform("uPattern", if (halftone) 1f else 0f)
                shader.setFloatUniform("uColorA", cA.red, cA.green, cA.blue)
                shader.setFloatUniform("uColorB", cB.red, cB.green, cB.blue)
                shader.setFloatUniform("uColorC", cC.red, cC.green, cC.blue)
                shader.setFloatUniform("uBg", bg.red, bg.green, bg.blue)
                drawRect(brush = brush)
            },
    )
}

// ── Mood-adaptive theming ──────────────────────────────────────────────────────
// A slow energy reading warms the glow toward amber on energetic passages and cools it toward
// slate on calm ones, alongside an intensity lift — so the atmosphere tracks the song's feel, not
// just its cover colour. Kept gentle (≤ ~22% tint) so it reads as a temperature shift, not a
// recolour.
private val MoodWarm = Color(0xFFFF8A3C)
private val MoodCool = Color(0xFF5C84C0)

private fun moodTint(c: Color, mood: Float): Color = when {
    mood >= 0.5f -> lerp(c, MoodWarm, ((mood - 0.5f) * 2f).coerceIn(0f, 1f) * 0.22f)
    else -> lerp(c, MoodCool, ((0.5f - mood) * 2f).coerceIn(0f, 1f) * 0.16f)
}

/**
 * Pre-API-33 fallback: three orbiting radial gradients. Not a true fluid field, but with the
 * bumped alphas and audio-driven motion it gives a clearly-visible, organic glow that morphs.
 */
@Composable
private fun GradientGlowFallback(
    triad: GlowTriad,
    bg: Color,
    intensity: GlowIntensity,
    signalFlow: StateFlow<VisualizerSignal>?,
) {
    val time by rememberFrameTimeSeconds()
    val safe = signalFlow?.collectAsState()?.value ?: VisualizerSignal()
    val bass by animateFloatAsState(safe.bass, tween(120), label = "fbBass")
    val mid by animateFloatAsState(safe.mid, tween(160), label = "fbMid")
    val treble by animateFloatAsState(safe.treble, tween(100), label = "fbTreble")
    val mood by animateFloatAsState(safe.energy, tween(2500), label = "fbMood")

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(bg)
                // Map the (shader-tuned) strength into gradient alphas, lifted by the mood envelope.
                val base = intensity.shaderStrength * 0.32f * (0.72f + 0.56f * mood)
                val cA = moodTint(triad.a, mood)
                val cB = moodTint(triad.b, mood)
                val cC = moodTint(triad.c, mood)
                val twoPi = (2.0 * Math.PI).toFloat()
                val t = time * 0.18f * twoPi

                // Primary — drifts horizontally with mids, swells + brightens with bass.
                val cx1 = size.width * (0.5f + 0.16f * sin(t) + 0.10f * mid)
                val cy1 = size.height * (0.20f + 0.06f * cos(t * 0.6f))
                val r1 = size.width * (1.05f + 0.30f * bass)
                val a1 = (base * (1.0f + 0.9f * bass)).coerceAtMost(0.6f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(cA.copy(alpha = a1), Color.Transparent),
                        center = Offset(cx1, cy1),
                        radius = r1,
                    ),
                )

                // Secondary — faster figure-eight, triad's second colour, treble-driven shimmer.
                val t2 = time * 0.18f * twoPi * 1.7f
                val cx2 = size.width * (0.5f - 0.20f * sin(t2) - 0.12f * treble)
                val cy2 = size.height * (0.34f + 0.12f * sin(t2 * 0.5f) * cos(t2))
                val r2 = size.width * (0.70f + 0.22f * (mid + treble))
                val a2 = (base * 0.7f * (0.7f + treble)).coerceAtMost(0.45f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(cB.copy(alpha = a2), Color.Transparent),
                        center = Offset(cx2, cy2),
                        radius = r2,
                    ),
                )

                // Tertiary — slow, large, triad's third colour, base ambient fill.
                val t3 = time * 0.18f * twoPi * 0.4f
                val cx3 = size.width * (0.5f + 0.12f * cos(t3))
                val cy3 = size.height * (0.10f + 0.05f * sin(t3))
                val r3 = size.width * (1.25f + 0.20f * bass)
                val a3 = (base * 0.6f).coerceAtMost(0.4f)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(cC.copy(alpha = a3), Color.Transparent),
                        center = Offset(cx3, cy3),
                        radius = r3,
                    ),
                )
            },
    )
}
