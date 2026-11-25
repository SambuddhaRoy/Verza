package com.verza.ui.look

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.R
import com.verza.data.Section
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.kit.Cover
import com.verza.ui.kit.takesTouches
import com.verza.ui.theme.Palette
import kotlin.math.sqrt

/**
 * Gamification, after SMSY (a neon night of signs and a red HUD over it) and Caffè Design (a room you
 * explore instead of a page you read). Listening is the game: your plays are XP and set your level,
 * days in a row are a combo, milestones are achievements to unlock, and a song in progress is a
 * score. Now Playing is a stage: a retro sun over a grid floor running toward you.
 */
object GameLook : Look("game", "Gamification", "Levels, combos and achievements for listening") {
    private val Night = Color(0xFF0A0716)
    private val Deep = Color(0xFF170C2C)
    private val Ink = Color(0xFFEDE7FF)
    val Red = Color(0xFFFF2D55)
    val Cyan = Color(0xFF2EF2FF)
    private val Magenta = Color(0xFFFF3DF2)
    private val Gold = Color(0xFFFFD23F)
    private val Neons = listOf(Cyan, Magenta, Gold)
    private val Grey = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

    override fun palette(swatches: List<Color>, dark: Boolean) =
        Palette.make(canvas = Night, ink = Ink, accent = Cyan, cell = Deep, fill = Cyan.copy(alpha = 0.1f), line = Cyan.copy(alpha = 0.22f), inkMuted = Color(0xFFB3A9D6))

    override val case = Case.Upper
    override fun cover(size: Dp): Shape = CutCornerShape(if (size in 1.dp..64.dp) 5.dp else 12.dp)
    override val group: Shape = CutCornerShape(14.dp)
    override val panel = androidx.compose.foundation.BorderStroke(1.dp, Cyan.copy(alpha = 0.45f))
    // A dark ground under the floating dock, so the list doesn't show between its panels.
    override val edgeFade = true
    override fun dockHeight(playing: Boolean) = 10.dp + 66.dp + (if (playing) 10.dp + 60.dp else 0.dp) + 10.dp

    private fun glow(c: Color, r: Float = 22f) = Shadow(c.copy(alpha = 0.85f), Offset.Zero, r)

    override val type = Types(
        masthead = TextStyle(fontFamily = Fonts.Orbitron, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = 0.04.em),
        section = TextStyle(fontFamily = Fonts.Orbitron, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.12.em),
        title = TextStyle(fontFamily = Fonts.Orbitron, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 31.sp, letterSpacing = 0.02.em),
        headline = TextStyle(fontFamily = Fonts.Orbitron, fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.08.em),
        rowTitle = TextStyle(fontFamily = Fonts.SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 21.sp),
        rowSubtitle = TextStyle(fontFamily = Fonts.ShareTechMono, fontSize = 14.sp, lineHeight = 18.sp),
        body = TextStyle(fontFamily = Fonts.SpaceGrotesk, fontSize = 15.sp, lineHeight = 21.sp),
        small = TextStyle(fontFamily = Fonts.ShareTechMono, fontSize = 13.sp, lineHeight = 16.sp),
        tab = TextStyle(fontFamily = Fonts.Orbitron, fontWeight = FontWeight.Bold, fontSize = 9.sp, lineHeight = 11.sp, letterSpacing = 0.08.em),
        label = TextStyle(fontFamily = Fonts.ShareTechMono, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.04.em),
    )

    /** Night falling to violet, a neon floor at the foot, stars, and scanlines over all of it. */
    @Composable
    override fun Backdrop(modifier: Modifier) {
        Canvas(modifier) {
            drawRect(Brush.verticalGradient(listOf(Night, Night, Color(0xFF1C0A35))))
            stars(31, 40)
            perspectiveFloor(Magenta.copy(alpha = 0.22f), horizon = 0.72f, rays = 16, rungs = 9)
            scanlines()
        }
    }

    private fun DrawScope.stars(seed: Int, n: Int) {
        val rnd = java.util.Random(seed.toLong())
        repeat(n) { drawCircle(Color.White.copy(alpha = 0.2f + rnd.nextFloat() * 0.5f), (0.6f + rnd.nextFloat()) * density, Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height * 0.6f)) }
    }

    private fun DrawScope.scanlines() {
        var y = 0f
        val step = 3 * density
        while (y < size.height) { drawLine(Color.Black.copy(alpha = 0.16f), Offset(0f, y), Offset(size.width, y), density * 0.8f); y += step }
    }

    /** HUD corner brackets round whatever this is on. */
    private fun Modifier.brackets(c: Color, arm: Dp = 14.dp) = drawBehind {
        val a = arm.toPx(); val w = 2.dp.toPx()
        for ((x, y, dx, dy) in listOf(Quad4(0f, 0f, 1f, 1f), Quad4(size.width, 0f, -1f, 1f), Quad4(0f, size.height, 1f, -1f), Quad4(size.width, size.height, -1f, -1f))) {
            drawLine(c, Offset(x, y), Offset(x + dx * a, y), w); drawLine(c, Offset(x, y), Offset(x, y + dy * a), w)
        }
    }

    /** The page's name in neon in a HUD frame, the time of day as the stage. */
    @Composable
    override fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) {
        Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 6.dp).brackets(Cyan.copy(alpha = 0.7f)).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("STAGE // ${(dateline ?: "Verza").uppercase()}", style = type.label, color = Cyan)
                Text(title.uppercase(), style = type.masthead.copy(shadow = glow(Red)), color = Color.White, modifier = Modifier.padding(top = 4.dp).semantics { heading() })
            }
            trailing?.invoke()
        }
    }

    @Composable
    override fun SectionTitle(text: String, modifier: Modifier) {
        Row(modifier.padding(top = 28.dp, bottom = 12.dp).semantics { heading() }, verticalAlignment = Alignment.CenterVertically) {
            Text("▸ ${text.uppercase()}", style = type.section.copy(shadow = glow(Cyan, 14f)), color = Cyan)
            Canvas(Modifier.weight(1f).padding(start = 12.dp).height(2.dp)) {
                drawLine(Cyan.copy(alpha = 0.4f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            }
        }
    }

    /** A cut-cornered panel: solid red and glowing for the one to press, a neon outline for the rest. */
    @Composable
    override fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) {
        val primary = color == Cyan
        val source = remember { MutableInteractionSource() }
        val pressed by source.collectIsPressedAsState()
        val flash by animateFloatAsState(if (pressed) 1f else 0f, spring(), label = "flash")
        val shape = CutCornerShape(10.dp)
        Row(
            modifier.heightIn(min = 50.dp).then(if (primary) Modifier.shadow(14.dp, shape, spotColor = Red, ambientColor = Red) else Modifier)
                .clip(shape).background(if (primary) Red else Deep).border(1.5.dp, if (primary) Color.White.copy(alpha = 0.6f) else Cyan, shape)
                .drawBehind { if (flash > 0f) drawRect(Color.White.copy(alpha = 0.25f * flash)) }
                .clickable(source, null, enabled = enabled, onClick = onClick).semantics { role = Role.Button }.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            val ink = if (primary) Color.White else Cyan
            if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text.uppercase(), style = type.headline, color = if (enabled) ink else ink.copy(alpha = 0.4f), maxLines = 1)
        }
    }

    @Composable
    override fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) {
        val shape = CutCornerShape(8.dp)
        Box(
            modifier.size(size).clip(shape).background(if (stain != null) Red else Deep.copy(alpha = 0.9f)).border(1.5.dp, Cyan, shape)
                .clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (stain != null) Color.White else Cyan, modifier = Modifier.size(iconSize)) }
    }

    @Composable
    override fun Divider(start: Dp) {
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(Cyan.copy(alpha = 0.2f), Offset(start.toPx(), 0f), Offset(size.width, 0f), size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
        }
    }

    /** Cartridges: each cover in a cut-cornered neon frame, glowing its own colour, numbered as a slot. */
    @Composable
    override fun Shelf(section: Section, onOpen: (String) -> Unit) {
        LazyRow(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            itemsIndexed(section.cards, key = { _, c -> c.song?.id ?: c.page ?: c.title }) { i, card ->
                val neon = Neons[i % Neons.size]
                val shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
                Column(
                    Modifier.width(150.dp).shadow(12.dp, shape, spotColor = neon, ambientColor = neon).clip(shape).background(Deep).border(1.5.dp, neon, shape)
                        .clickable(onClick = cardAction(card, onOpen)).padding(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("SLOT %02d".format(i + 1), style = type.label.copy(fontSize = 11.sp), color = neon)
                        Spacer(Modifier.weight(1f))
                        Text("▶", style = type.label.copy(fontSize = 11.sp), color = neon)
                    }
                    Cover(card.art?.let { artAt(it, 544) }, 132.dp, Modifier.padding(top = 6.dp))
                    Text(cardTitle(card).uppercase(), style = type.headline.copy(fontSize = 11.sp, lineHeight = 14.sp), color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }

    /** What the play log has earned: a level from plays, XP to the next, the day combo, and achievements. */
    internal data class Progress(val level: Int, val xp: Float, val toNext: Int)

    internal fun progress(plays: Int): Progress {
        // Each level costs a little more than the last: level n starts at 3(n-1)^2 plays.
        val level = (sqrt(plays / 3.0)).toInt() + 1
        val from = 3 * (level - 1) * (level - 1); val to = 3 * level * level
        return Progress(level, (plays - from).toFloat() / (to - from), to - plays)
    }

    private data class Badge(val icon: Int, val name: String, val earned: Boolean)

    @Composable
    override fun HomeHero() {
        val l = rememberListening()
        val p = progress(l.plays)
        val badges = listOf(
            Badge(R.drawable.fx_note, "First play", l.plays >= 1),
            Badge(R.drawable.fx_medal, "10 plays", l.plays >= 10),
            Badge(R.drawable.fx_100, "100 plays", l.plays >= 100),
            Badge(R.drawable.fx_fire, "3 day combo", l.streak >= 3),
            Badge(R.drawable.fx_moon, "Night owl", l.plays >= 10 && l.nightShare >= 0.25f),
            Badge(R.drawable.fx_trophy, "10 hours", l.minutes >= 600),
        )
        val shape = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(shape).background(Deep).border(1.5.dp, Cyan.copy(alpha = 0.6f), shape).padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clip(CutCornerShape(10.dp)).background(Night).border(1.5.dp, Magenta, CutCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon3D(R.drawable.fx_video_game, 42.dp) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("PLAYER 1", style = type.label, color = Cyan)
                    Text("LV ${p.level}", style = type.title.copy(shadow = glow(Gold)), color = Gold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon3D(R.drawable.fx_fire, 22.dp)
                        Text(" x${l.streak}", style = type.headline.copy(fontSize = 16.sp), color = Color.White)
                    }
                    Text("DAY COMBO", style = type.label.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.7f))
                }
            }
            // XP to the next level, in segments.
            Canvas(Modifier.fillMaxWidth().padding(top = 12.dp).height(12.dp).semantics { contentDescription = "${p.toNext} plays to level ${p.level + 1}" }) { segments(p.xp, 24, Gold, 0f) }
            Text("${l.plays} XP · ${p.toNext} TO LV ${p.level + 1}", style = type.label.copy(fontSize = 11.sp), color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 5.dp))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (b in badges) Column(Modifier.width(48.dp).semantics(mergeDescendants = true) { contentDescription = "${b.name}, ${if (b.earned) "unlocked" else "locked"}" }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(b.icon), null, Modifier.size(34.dp).graphicsLayer { alpha = if (b.earned) 1f else 0.3f }, colorFilter = if (b.earned) null else Grey)
                    Text(if (b.earned) b.name.uppercase() else "???", style = type.label.copy(fontSize = 9.sp, lineHeight = 11.sp), color = if (b.earned) Color.White else Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }

    /** A bar of [n] cells, filled to [fraction] in [c] with a glow; the cell at [head] bright white. */
    private fun DrawScope.segments(fraction: Float, n: Int, c: Color, drag: Float, height: Float = size.height) {
        val gap = 2.5f * density
        val w = (size.width - gap * (n - 1)) / n
        val h = height * (1f + 0.3f * drag)
        val top = (size.height - h) / 2
        val filled = (fraction * n).toInt()
        for (i in 0 until n) {
            val x = i * (w + gap)
            when {
                i < filled -> { drawRect(c.copy(alpha = 0.35f), Offset(x - density, top - density), Size(w + 2 * density, h + 2 * density)); drawRect(c, Offset(x, top), Size(w, h)) }
                i == filled -> drawRect(Color.White, Offset(x, top), Size(w * ((fraction * n) - filled).coerceAtLeast(0.15f), h))
                else -> drawRect(c.copy(alpha = 0.35f), Offset(x, top), Size(w, h), style = Stroke(density))
            }
        }
    }

    /** A HUD across the foot: the song with its progress as a health bar, then the tabs, the chosen one lit. */
    @Composable
    override fun Dock(state: DockState, modifier: Modifier) {
        val shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
        Column(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.downloads?.let { DownloadsLine(it.uppercase(), Cyan) }
            state.now.song?.let { song ->
                val played by playedFraction(state.now)
                Row(
                    Modifier.fillMaxWidth().height(60.dp).takesTouches().clip(shape).background(Deep.copy(alpha = 0.96f)).border(1.5.dp, Red, shape)
                        .clickable(onClick = state.openPlayer).semantics { contentDescription = "Now playing: ${song.title}" }.padding(start = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(song.art?.let { artAt(it, 226) }, 40.dp)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(com.verza.data.shownTitle(song.title, song.artist).uppercase(), style = type.headline.copy(fontSize = 11.sp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Canvas(Modifier.fillMaxWidth().padding(top = 6.dp).height(7.dp)) { segments(played, 20, Red, 0f) }
                    }
                    PlayPauseNext(state.now, Color.White, 26.dp)
                }
            }
            Row(Modifier.fillMaxWidth().height(66.dp).takesTouches().clip(shape).background(Deep.copy(alpha = 0.96f)).border(1.5.dp, Cyan.copy(alpha = 0.7f), shape)) {
                for (tab in state.tabs) {
                    val on = tab.route == state.current
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clickable { state.go(tab.route) }.semantics(mergeDescendants = true) { role = Role.Tab; selected = on }
                            .drawBehind { if (on) { drawRect(Brush.verticalGradient(listOf(Color.Transparent, Red.copy(alpha = 0.3f)))); drawRect(Red, Offset(size.width * 0.2f, size.height - 3.dp.toPx()), Size(size.width * 0.6f, 3.dp.toPx())) } },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(tab.icon, null, tint = if (on) Color.White else Cyan.copy(alpha = 0.75f), modifier = Modifier.size(22.dp))
                        Text(tab.label.substringAfterLast(' ').uppercase(), style = type.tab, color = if (on) Color.White else Cyan.copy(alpha = 0.75f), maxLines = 1, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }

    override fun nowPlaying(page: Palette) = page

    /** Synthwave: a striped sun sinking into a grid floor that runs toward you, stars, scanlines. */
    @Composable
    override fun NowPlayingBackground(art: String?) {
        val t by slowTime(30)
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(0f to Night, 0.45f to Color(0xFF2A0B45), 0.6f to Color(0xFF4A0E4E), 0.6001f to Night, 1f to Color(0xFF12061F)))
            stars(9, 60)
            sun(Offset(size.width / 2, size.height * 0.46f), size.width * 0.36f, size.height * 0.6f)
            movingFloor(size.height * 0.6f, (t * 0.6f) % 1f)
            scanlines()
        }
    }

    /** A sun cut by bars of the sky, thicker toward the horizon at [cut]. */
    private fun DrawScope.sun(c: Offset, r: Float, cut: Float) {
        drawCircle(Brush.radialGradient(listOf(Magenta.copy(alpha = 0.35f), Color.Transparent), c, r * 1.6f), r * 1.6f, c)
        drawCircle(Brush.verticalGradient(listOf(Gold, Color(0xFFFF7A3D), Magenta), c.y - r, c.y + r), r, c)
        var y = c.y + r * 0.1f; var bar = 2f * density
        while (y < c.y + r) { drawRect(Color(0xFF3A0D4A), Offset(c.x - r, y), Size(2 * r, bar)); y += bar + 7 * density; bar += 1.6f * density }
        drawRect(Night, Offset(0f, cut), Size(size.width, size.height - cut))
    }

    /** Grid lines from the horizon at [top], the cross lines sliding toward you by [phase]. */
    private fun DrawScope.movingFloor(top: Float, phase: Float) {
        val cx = size.width / 2
        val w = density
        for (i in -18..18) drawLine(Magenta.copy(alpha = 0.5f), Offset(cx + i * size.width / 60f, top), Offset(cx + i * size.width / 7f, size.height), w)
        for (k in 0 until 14) {
            val tt = (k + phase) / 14f
            val y = top + (size.height - top) * tt * tt
            drawLine(Cyan.copy(alpha = 0.15f + 0.6f * tt), Offset(0f, y), Offset(size.width, y), w * (1f + tt))
        }
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Magenta, Color.Transparent)), Offset(0f, top), Offset(size.width, top), 2 * w)
    }

    @Composable
    override fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) {
        val b = bob(2800)
        val shape = CutCornerShape(topStart = 22.dp, bottomEnd = 22.dp)
        Box(
            Modifier.graphicsLayer { scaleX = rest * (1f + swell * 0.04f); scaleY = scaleX; translationY = -fly * size.height * 2f + b * 4 * density; alpha = 1f - fly }
                .size(side * 0.84f).brackets(Cyan, 22.dp).padding(10.dp).shadow(26.dp, shape, spotColor = Magenta, ambientColor = Magenta).clip(shape).border(2.dp, Cyan, shape),
        ) { AsyncImage(art, null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
    }

    /** An arcade button: a red dome in a dark collar, lit from the top left, sinking when pressed. */
    @Composable
    override fun PlayButton(playing: Boolean, onClick: () -> Unit) {
        val source = remember { MutableInteractionSource() }
        val pressed by source.collectIsPressedAsState()
        val sink by animateFloatAsState(if (pressed) 1f else 0f, spring(), label = "sink")
        Box(
            Modifier.size(92.dp).clickable(source, null, onClick = onClick).semantics { role = Role.Button; contentDescription = if (playing) "Pause" else "Play" },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center; val r = size.minDimension / 2
                drawCircle(Red.copy(alpha = 0.35f * (1f - sink)), r, c)
                drawCircle(Color(0xFF2A1640), r * 0.9f, c)
                drawCircle(Color(0xFF14091F), r * 0.9f, c, style = Stroke(3 * density))
                val dome = r * (0.74f - 0.04f * sink)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFF6F8B), Red, Color(0xFFB3002A)), c + Offset(-dome * 0.3f, -dome * 0.3f), dome * 1.5f), dome, c + Offset(0f, 2 * density * sink))
                drawOval(Color.White.copy(alpha = 0.4f * (1f - sink * 0.5f)), Offset(c.x - dome * 0.55f, c.y - dome * 0.7f), Size(dome * 0.7f, dome * 0.38f))
            }
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(38.dp).graphicsLayer { translationY = 2 * density * sink })
        }
    }

    /** The song as a health bar: cells filling red, the one being played bright. */
    override fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) {
        segments(shown, 30, Red, dragging, 12f * density)
    }

    /** The HUD over the stage: the score so far (a hundred a second) and the level. */
    @Composable
    override fun NowPlayingDecor() {
        val now by LocalContext.current.graph.player.state.collectAsState()
        val played by playedFraction(now)
        val l = rememberListening()
        val score = (played * now.durationMs / 10).toLong()
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 52.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("SCORE", style = type.label.copy(fontSize = 11.sp), color = Cyan)
                Text("%07d".format(score), style = type.headline.copy(fontSize = 18.sp, shadow = glow(Cyan, 12f)), color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("PLAYER 1", style = type.label.copy(fontSize = 11.sp), color = Cyan)
                Text("LV ${progress(l.plays).level}", style = type.headline.copy(fontSize = 18.sp, shadow = glow(Gold, 12f)), color = Gold)
            }
        }
    }

    @Composable
    override fun Preview(modifier: Modifier) {
        Box(modifier) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(0f to Night, 0.55f to Color(0xFF4A0E4E), 0.5501f to Night, 1f to Night))
                sun(Offset(size.width / 2, size.height * 0.45f), size.width * 0.3f, size.height * 0.55f)
                movingFloor(size.height * 0.55f, 0.3f)
            }
            Text("LV 1", style = type.headline.copy(shadow = glow(Gold, 12f)), color = Gold, modifier = Modifier.padding(10.dp))
        }
    }
}

private data class Quad4(val x: Float, val y: Float, val dx: Float, val dy: Float)
