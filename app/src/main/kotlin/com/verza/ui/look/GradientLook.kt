package com.verza.ui.look

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.R
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.ui.kit.Cover
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin

/**
 * Beautiful gradients, after Klook's out-of-office campaign (a page of blues, violets, oranges and
 * teals moving into each other, soft 3D things floating, white cards of photos fanned out) and
 * Stökt (a 3D mark in shades of one colour). White words over colour, white cards to hold things.
 */
object GradientLook : Look("gradient", "Gradients", "Colour moving under everything, white cards") {
    private val Indigo = Color(0xFF3528C9)
    private val Ink = Color(0xFF1B1547)
    private val Sun = Color(0xFFFFD84D)
    /** The blobs the page is mixed from: blue, violet, orange, teal, pink. */
    private val Blobs = listOf(Color(0xFF3B4BFF), Color(0xFF9B3BFF), Color(0xFFFF7A1A), Color(0xFF18D6C0), Color(0xFFFF4FA0))
    private val Warm = Brush.linearGradient(listOf(Color(0xFF4B6BFF), Color(0xFFFF4FA0), Color(0xFFFF9A3D)))

    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Indigo, ink = Color.White, accent = Sun, cell = Color.White.copy(alpha = 0.14f), fill = Color.White.copy(alpha = 0.18f), line = Color.White.copy(alpha = 0.28f), inkMuted = Color.White.copy(alpha = 0.78f))

    override fun cover(size: Dp): Shape = RoundedCornerShape(if (size in 1.dp..64.dp) 10.dp else 18.dp)
    override val group: Shape = RoundedCornerShape(24.dp)
    override val edgeFade = false
    override fun dockHeight(playing: Boolean) = 10.dp + 64.dp + (if (playing) 10.dp + 64.dp else 0.dp) + 12.dp

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.02).em),
        section = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        title = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.01).em),
        headline = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
        rowTitle = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.Poppins, fontSize = 13.sp, lineHeight = 18.sp),
        body = TextStyle(fontFamily = Fonts.Poppins, fontSize = 15.sp, lineHeight = 22.sp),
        small = TextStyle(fontFamily = Fonts.Poppins, fontSize = 12.sp, lineHeight = 17.sp),
        tab = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp),
        label = TextStyle(fontFamily = Fonts.Poppins, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    )

    /** Five soft blobs of colour drifting slowly round each other, under a breath of shade for the white words. */
    @Composable
    override fun Backdrop(modifier: Modifier) {
        val t by slowTime(15)
        Canvas(modifier.background(Indigo)) { mesh(t * (2 * Math.PI.toFloat()) / 26f) }
    }

    private fun DrawScope.mesh(t: Float) {
        val w = size.width; val h = size.height
        val spots = listOf(
            Offset(0.15f + 0.12f * sin(t), 0.12f + 0.08f * cos(t * 1.3f)),
            Offset(0.85f + 0.1f * cos(t * 0.8f), 0.25f + 0.1f * sin(t)),
            Offset(0.7f + 0.15f * sin(t * 1.1f), 0.75f + 0.08f * cos(t)),
            Offset(0.2f + 0.1f * cos(t * 1.2f), 0.6f + 0.12f * sin(t * 0.9f)),
            Offset(0.5f + 0.2f * sin(t * 0.7f), 0.42f + 0.1f * cos(t * 1.4f)),
        )
        spots.forEachIndexed { i, s ->
            val c = Offset(s.x * w, s.y * h)
            val r = maxOf(w, h) * 0.55f
            drawCircle(Brush.radialGradient(listOf(Blobs[i], Blobs[i].copy(alpha = 0f)), c, r), r, c)
        }
        drawRect(Color.Black.copy(alpha = 0.12f))
    }

    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                if (dateline != null) Text(dateline, style = type.label, color = Ink, modifier = Modifier.background(Color.White, CircleShape).padding(horizontal = 12.dp, vertical = 4.dp))
                Text(case.of(title), style = type.masthead, color = Color.White, modifier = Modifier.padding(top = 8.dp).semantics { heading() })
            }
            trailing?.invoke()
        }
    }

    /** A white pill with the words in ink and a little coloured circle with an arrow at its end. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == LocalPalette.current.accent
        Row(
            modifier.heightIn(min = 50.dp).shadow(if (primary) 10.dp else 0.dp, CircleShape, spotColor = Color(0xFF2A0A7A))
                .background(if (primary) Color.White else Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable(enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(start = 18.dp, end = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) { Icon(icon, null, tint = if (primary) Ink else Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
                Text(case.of(text), style = type.headline, color = (if (primary) Ink else Color.White).copy(alpha = if (enabled) 1f else 0.4f), maxLines = 1)
            }
            Box(Modifier.padding(start = 10.dp).size(36.dp).background(Warm, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }

    /** White cards of photographs fanned out, each turned a little, as postcards in a hand. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        LazyRow(contentPadding = PaddingValues(start = 20.dp, end = 30.dp, top = 8.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                val turn = listOf(-3f, 2f, -1.5f, 3f)[i % 4]
                Column(
                    Modifier.width(150.dp).graphicsLayer { rotationZ = turn }.shadow(14.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF14005A))
                        .background(Color.White, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp)).clickable(onClick = cardAction(card, onOpen)).padding(8.dp),
                ) {
                    Cover(card.art?.let { artAt(it, 544) }, 134.dp)
                    Text(cardTitle(card), style = type.headline.copy(fontSize = 13.sp, lineHeight = 17.sp), color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
                    Text(card.subtitle.substringBefore(" • "), style = type.small, color = Ink.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                }
            }
        }
    }

    /** Soft 3D things drifting over the colour: a cloud, headphones, notes. */
    @Composable
    override fun HomeHero() {
        val a = bob(3600); val b = bob(4400); val c = bob(5200)
        Box(Modifier.fillMaxWidth().height(118.dp)) {
            Icon3D(R.drawable.fx_cloud, 96.dp, Modifier.align(Alignment.CenterStart).offset(x = 12.dp, y = (a * 8).dp))
            Icon3D(R.drawable.fx_headphone, 84.dp, Modifier.align(Alignment.Center).offset(y = (b * 10).dp).graphicsLayer { rotationZ = -12f + b * 6f })
            Icon3D(R.drawable.fx_notes, 64.dp, Modifier.align(Alignment.CenterEnd).offset(x = (-26).dp, y = (c * 9 - 10).dp))
        }
    }

    /** A white pill floating at the foot, the chosen tab in a circle of warm colour; the song on a white card above. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        Column(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 14.dp, end = 14.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.downloads?.let { DownloadsLine(it, Color.White) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.fillMaxWidth().height(64.dp).takesTouches().shadow(16.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFF14005A)).background(Color.White, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp)).clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }
                        .drawWithContentProgress(played).padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 44.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist), style = type.headline, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, style = type.small, color = Ink.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    PlayPauseNext(state.now, Ink, 28.dp)
                }
            }
            Row(
                Modifier.fillMaxWidth().height(64.dp).takesTouches().shadow(16.dp, CircleShape, spotColor = Color(0xFF14005A)).background(Color.White, CircleShape).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(CircleShape).clickable { state.go(tab.route) }.semantics(mergeDescendants = true) { role = Role.Tab; selected = on },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Box(Modifier.size(32.dp).then(if (on) Modifier.background(Warm, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                            Icon(tab.icon, null, tint = if (on) Color.White else Ink, modifier = Modifier.size(20.dp))
                        }
                        Text(tab.label, style = type.tab, color = Ink, maxLines = 1)
                    }
                }
            }
        }
    }

    override fun nowPlaying(page: Palette) = page

    @Composable
    override fun NowPlayingBackground(art: String?) {
        val t by slowTime(24)
        Canvas(Modifier.fillMaxSize().background(Indigo)) { mesh(t * (2 * Math.PI.toFloat()) / 18f) }
    }

    /** The cover on a white card, turned a little, a small caption under it. */
    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        Column(
            Modifier.graphicsLayer { scaleX = rest * (1f + swell * 0.03f); scaleY = scaleX; rotationZ = -3f; translationY = -fly * size.height * 2f; alpha = 1f - fly }.width(side * 0.86f)
                .shadow(26.dp, RoundedCornerShape(28.dp), spotColor = Color(0xFF14005A)).background(Color.White, RoundedCornerShape(28.dp)).padding(12.dp),
        ) {
            AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(side * 0.86f - 24.dp).clip(RoundedCornerShape(18.dp)))
            Row(Modifier.padding(top = 10.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(Warm, CircleShape))
                Text("  Now playing", style = type.label, color = Ink)
            }
        }
    }

    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        Box(
            Modifier.size(84.dp).shadow(18.dp, CircleShape, spotColor = Color(0xFF14005A)).background(Color.White, CircleShape).clickable(onClick = onClick)
                .semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Indigo, modifier = Modifier.size(46.dp)) }
    }

    /** A soft white track, warm colour as far as the song has played, a white knob. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        val h = (8f + 4f * dragging) * density
        val top = Offset(0f, size.height / 2 - h / 2)
        drawRoundRect(Color.White.copy(alpha = 0.3f), top, Size(size.width, h), CornerRadius(h / 2))
        drawRoundRect(Brush.horizontalGradient(listOf(Sun, Color(0xFFFF4FA0))), top, Size(size.width * shown, h), CornerRadius(h / 2))
        drawCircle(Color.White, (9f + 3f * dragging) * density, Offset(size.width * shown, size.height / 2))
    }

    @Composable
    override fun NowPlayingDecor() {
        val a = bob(4800); val b = bob(3900)
        Box(Modifier.fillMaxSize()) {
            Icon3D(R.drawable.fx_cloud, 120.dp, Modifier.align(Alignment.CenterStart).offset(x = (-40).dp, y = (40 + a * 10).dp))
            Icon3D(R.drawable.fx_sparkles, 54.dp, Modifier.align(Alignment.TopEnd).offset(x = (-20).dp, y = (90 + b * 8).dp))
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Box(modifier) {
            Canvas(Modifier.fillMaxSize().background(Indigo)) { mesh(1.2f) }
            Column(Modifier.padding(12.dp)) {
                Text("Colour", style = type.masthead.copy(fontSize = 26.sp, lineHeight = 30.sp), color = Color.White)
                Box(Modifier.padding(top = 10.dp).size(width = 80.dp, height = 30.dp).background(Color.White, CircleShape))
            }
            Icon3D(R.drawable.fx_cloud, 64.dp, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        }
    }
}

/** A warm line along an element's foot, [played] of its width. */
internal fun Modifier.drawWithContentProgress(played: Float) = this.drawWithContent {
    drawContent()
    drawRect(Brush.horizontalGradient(listOf(Color(0xFF4B6BFF), Color(0xFFFF4FA0), Color(0xFFFF9A3D))), Offset(0f, size.height - 3.dp.toPx()), Size(size.width * played, 3.dp.toPx()))
}
