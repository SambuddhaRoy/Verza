package com.verza.ui.nowplaying

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import com.verza.ui.kit.ChromeButton
import com.verza.ui.kit.Glassed
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.ui.kit.takesTouches
import com.verza.data.Lyrics
import com.verza.data.artAt
import com.verza.graph
import com.verza.ui.theme.Type
import kotlinx.coroutines.delay

/**
 * The song's lyrics on its cover's wash. Synced ones follow the song, the current line lit and kept
 * in view, and a tap on a line goes there; plain ones just scroll.
 */
@Composable
fun LyricsPage(onClose: () -> Unit) {
    val graph = LocalContext.current.graph
    val now by graph.player.state.collectAsState()
    val song = now.song ?: return
    BackHandler(onBack = onClose)
    val look = com.verza.ui.look.LocalLook.current
    val p = look.nowPlaying(com.verza.ui.theme.LocalPalette.current)
    com.verza.ui.kit.BarIcons(light = p.dark)
    val lyrics by produceState<Lyrics?>(null, song.id) { value = runCatching { graph.lyrics.get(song) }.getOrDefault(Lyrics.None) }

    // Over the cover's colours, as Now Playing is, and dark like it whatever the theme.
    CompositionLocalProvider(com.verza.ui.theme.LocalPalette provides p) { Glassed(
        ground = Color.Black,
        modifier = Modifier.fillMaxSize().takesTouches(),
        content = { look.NowPlayingBackground(song.art?.let { artAt(it, 226) }) },
    ) {
        // The lines fade into the top rather than running into the status bar.
        val fade = Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
            drawContent()
            drawRect(Brush.verticalGradient(0f to Color.Transparent, 96.dp.toPx() / size.height to Color.Black), blendMode = BlendMode.DstIn)
        }
        when (val l = lyrics) {
            null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = p.ink)
            Lyrics.None -> Text("No lyrics for this song", style = Type.RowTitle, color = p.ink.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Center))
            is Lyrics.Plain -> Text(
                l.text, style = Type.Headline.copy(fontSize = 22.sp, lineHeight = 32.sp), color = p.ink,
                modifier = Modifier.fillMaxSize().statusBarsPadding().then(fade).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 96.dp),
            )
            is Lyrics.Synced -> {
                var position by remember { mutableLongStateOf(0L) }
                LaunchedEffect(song.id) { while (true) { position = graph.player.position; delay(200) } }
                // A blank line is a ♪ only where the music really goes on alone for a while; LRCLIB has
                // one after nearly every line of some songs, and Yellow read ♪ between each of them.
                val lines = remember(l) { musicalGaps(l.lines) }
                val current = lines.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
                val list = rememberLazyListState()
                LaunchedEffect(current) { list.animateScrollToItem((current - 2).coerceAtLeast(0)) }
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().statusBarsPadding().then(fade), contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 72.dp, bottom = 240.dp)) {
                    itemsIndexed(lines) { i, line ->
                        val color by animateColorAsState(if (i == current) p.ink else p.ink.copy(alpha = 0.34f), label = "line")
                        Text(
                            line.text.ifBlank { "♪" },
                            style = Type.Title.copy(fontSize = 30.sp, lineHeight = 37.sp), color = color,
                            modifier = Modifier.fillMaxWidth().clickable { graph.player.seek(line.timeMs) }.padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
        ChromeButton(Icons.Rounded.KeyboardArrowDown, "Close", Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), onClick = onClose)
    } }
}

/** Drops blank lines but those before a gap of 5s or more, which show as ♪. */
internal fun musicalGaps(lines: List<com.verza.data.LyricLine>): List<com.verza.data.LyricLine> = lines.filterIndexed { i, line ->
    if (line.text.isNotBlank()) return@filterIndexed true
    val next = lines.getOrNull(i + 1)?.timeMs ?: return@filterIndexed false
    next - line.timeMs >= 5_000
}
