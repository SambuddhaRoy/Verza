package com.verza.ui.sleeve

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.verza.ui.theme.CoverColors
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.FontSleeve

// ────────────────────────────────────────────────────────────────────────────────
// Sleeve's atmospheric primitives — a Compose port of the UMBRA reference's
// umbra-lib.jsx building blocks: film grain, vignette, chromatic-aberration type,
// mono eyebrows & superscripts, moody cover gradients, editorial track rows, and the
// thin-bordered accent transport. These are what give Sleeve its print/photographic
// feel rather than a flat dark theme.
// ────────────────────────────────────────────────────────────────────────────────

// ── Film grain ──────────────────────────────────────────────────────────────────
// A small tile of per-pixel random grey noise, generated once and repeated across the
// surface. Drawn with plain source-over at low alpha: over the deep Sleeve canvas the
// grey speckles lift toward light (visible grain); over bright cover art they pull
// slightly toward grey (a fine photographic tooth). Cheap — one tiled rect per frame.
private val grainTile: ImageBitmap by lazy(LazyThreadSafetyMode.NONE) {
    val n = 128
    val pixels = IntArray(n * n)
    val rnd = java.util.Random(7)
    for (i in pixels.indices) {
        val g = rnd.nextInt(256)
        val a = rnd.nextInt(256)
        pixels[i] = (a shl 24) or (g shl 16) or (g shl 8) or g
    }
    val bmp = android.graphics.Bitmap.createBitmap(n, n, android.graphics.Bitmap.Config.ARGB_8888)
    bmp.setPixels(pixels, 0, n, 0, 0, n, n)
    bmp.asImageBitmap()
}

/** Overlays a fine, repeating film-grain texture on top of this node's content. */
fun Modifier.grain(opacity: Float = 0.06f): Modifier = this.drawWithCache {
    val brush = ShaderBrush(ImageShader(grainTile, TileMode.Repeated, TileMode.Repeated))
    onDrawWithContent {
        drawContent()
        drawRect(brush = brush, alpha = opacity)
    }
}

/** Darkens the frame's edges with a soft radial vignette — anchored a little above centre. */
fun Modifier.vignette(strength: Float = 0.5f): Modifier = this.drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.radialGradient(
            0.42f to Color.Transparent,
            1.0f to Color.Black.copy(alpha = strength),
            center = Offset(size.width * 0.5f, size.height * 0.40f),
            radius = size.maxDimension * 0.78f,
        ),
    )
}

/**
 * A moody, dark-photography backdrop synthesised from the cover palette (stands in for an
 * album cover where there isn't a single representative image — e.g. local playlists, or as a
 * fallback before art loads). Two offset radial glows over a near-black base, optionally graded
 * down into [CoverColors.bg] so a header melts into the page below it.
 */
fun Modifier.moodyBackdrop(cover: CoverColors, gradeToBg: Boolean = false): Modifier = this.drawBehind {
    val deep = lerp(cover.accent, Color.Black, 0.62f)
    drawRect(cover.bg)
    drawRect(
        Brush.radialGradient(
            listOf(cover.accent.copy(alpha = 0.26f), Color.Transparent),
            center = Offset(size.width * 0.30f, size.height * 0.20f),
            radius = size.maxDimension * 0.85f,
        ),
    )
    drawRect(
        Brush.radialGradient(
            listOf(deep.copy(alpha = 0.60f), Color.Transparent),
            center = Offset(size.width * 0.72f, size.height * 0.86f),
            radius = size.maxDimension * 0.80f,
        ),
    )
    if (gradeToBg) {
        drawRect(
            Brush.verticalGradient(
                0.40f to Color.Transparent,
                1.0f to cover.bg,
            ),
        )
    }
}

// ── Editorial type helpers ───────────────────────────────────────────────────────

/** Wide-tracked uppercase monospace eyebrow — the reference's catalogue/label voice. */
val SleeveEyebrow = TextStyle(
    fontFamily = FontMono, fontWeight = FontWeight.Normal,
    fontSize = 10.5.sp, lineHeight = 14.sp, letterSpacing = 0.22.em,
)

/** Renders [text] uppercased in the [SleeveEyebrow] mono voice. */
@Composable
fun Eyebrow(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = SleeveEyebrow,
) {
    Text(
        text = text.uppercase(),
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * Builds a serif headline with a small superscript monospace numeral/label trailing it — the
 * reference's signature "Cinder⁰⁴⁴" / "Umbra ᴱᴰ·⁰⁴" treatment.
 */
fun supLabel(
    main: String,
    sup: String,
    supColor: Color,
    supSize: TextUnit = 11.sp,
): AnnotatedString = buildAnnotatedString {
    append(main)
    withStyle(
        SpanStyle(
            fontFamily = FontMono,
            fontSize = supSize,
            baselineShift = BaselineShift(0.42f),
            letterSpacing = 0.04.em,
            color = supColor,
        ),
    ) { append(sup) }
}

/**
 * Chromatic-aberration headline: the same text drawn in a red and a cyan ghost, nudged apart by
 * [intensity] em, with the solid [color] on top. Subtle RGB split that reads as analogue print
 * misregistration. Keep [intensity] tiny (≈0.012–0.02).
 */
@Composable
fun ChromaticText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    intensity: Float = 0.016f,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val fs = style.fontSize
    val dx = if (fs.isSp) with(LocalDensity.current) { fs.toPx() } * intensity else 0f
    Box(modifier) {
        Text(
            text, style = style.copy(color = Color(0xFFFF2A54).copy(alpha = 0.55f)),
            maxLines = maxLines, overflow = overflow,
            modifier = Modifier.graphicsLayer { translationX = -dx },
        )
        Text(
            text, style = style.copy(color = Color(0xFF28AAFF).copy(alpha = 0.55f)),
            maxLines = maxLines, overflow = overflow,
            modifier = Modifier.graphicsLayer { translationX = dx },
        )
        Text(text, style = style.copy(color = color), maxLines = maxLines, overflow = overflow)
    }
}

// ── Chrome controls ──────────────────────────────────────────────────────────────

/** A rounded mono pill — used for the tab affordances at the top of the poster Now Playing. */
@Composable
fun SleevePill(
    text: String,
    onClick: () -> Unit,
    cover: CoverColors,
    filled: Boolean = false,
) {
    val shape = RoundedCornerShape(50)
    val base = if (filled) {
        Modifier.clip(shape).background(cover.ink.copy(alpha = 0.92f))
    } else {
        Modifier.clip(shape).border(1.dp, cover.line, shape)
    }
    Box(modifier = base.clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 7.dp)) {
        Text(
            text,
            style = TextStyle(fontFamily = FontMono, fontSize = 12.5.sp, letterSpacing = 0.02.em),
            color = if (filled) cover.bg else cover.sub,
        )
    }
}

/** The accent-filled circular play button shared by the Album / Playlist action rows. */
@Composable
fun SleeveAccentPlay(
    cover: CoverColors,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    size: Int = 48,
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(50))
            .background(cover.accent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = onAccent(cover.accent),
            modifier = Modifier.size((size * 0.42f).dp),
        )
    }
}

/** A thin-outlined circular icon button, mono/secondary toned — the reference's quiet actions. */
@Composable
fun SleeveOutlineAction(
    cover: CoverColors,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    size: Int = 44,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(shape)
            .border(1.dp, cover.line, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = cover.sub,
            modifier = Modifier.size((size * 0.40f).dp),
        )
    }
}

/** Best on-accent ink (near-black or white) for an accent-filled control. */
fun onAccent(accent: Color): Color = if (accent.luminanceApprox() > 0.55f) Color(0xFF150A05) else Color.White

private fun Color.luminanceApprox(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

// ── Editorial track row ──────────────────────────────────────────────────────────

/**
 * The reference's track listing row: a mono index, a serif (Newsreader 400) title that turns
 * accent + italic when it's the current track, an optional mono "feat." note, an optional mono
 * duration, and a [trailing] slot for the overflow menu. Closed with a hairline rule so a list
 * reads like a printed tracklist rather than a stack of tiles.
 */
@Composable
fun SleeveTrackRow(
    index: Int,
    title: String,
    cover: CoverColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    duration: String? = null,
    isCurrent: Boolean = false,
    trailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "%02d".format(index),
                style = TextStyle(fontFamily = FontMono, fontSize = 11.sp, letterSpacing = 0.04.em),
                color = if (isCurrent) cover.accent else cover.faint,
                modifier = Modifier.width(18.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontSleeve,
                        fontWeight = FontWeight.Normal,
                        fontStyle = if (isCurrent) FontStyle.Italic else FontStyle.Normal,
                        fontSize = 20.sp,
                        lineHeight = 24.sp,
                        letterSpacing = (-0.01).em,
                    ),
                    color = if (isCurrent) cover.accent else cover.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle.uppercase(),
                        style = TextStyle(fontFamily = FontMono, fontSize = 9.5.sp, letterSpacing = 0.05.em),
                        color = cover.faint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (duration != null) {
                Text(
                    text = duration,
                    style = TextStyle(fontFamily = FontMono, fontSize = 11.5.sp),
                    color = cover.sub,
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(2.dp))
                trailing()
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = cover.line,
            modifier = Modifier.padding(horizontal = 22.dp),
        )
    }
}
