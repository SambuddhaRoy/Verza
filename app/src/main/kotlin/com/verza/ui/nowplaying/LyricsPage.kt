package com.verza.ui.nowplaying

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
    val lyrics by produceState<Lyrics?>(null, song.id) { value = runCatching { graph.lyrics.get(song) }.getOrDefault(Lyrics.None) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(song.art?.let { artAt(it, 544) }, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(60.dp), alpha = 0.5f)
        when (val l = lyrics) {
            null -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            Lyrics.None -> Text("No lyrics for this song", style = Type.RowTitle, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.Center))
            is Lyrics.Plain -> Text(
                l.text, style = Type.RowTitle.copy(fontSize = 22.sp, lineHeight = 32.sp), color = Color.White,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 96.dp),
            )
            is Lyrics.Synced -> {
                var position by remember { mutableLongStateOf(0L) }
                LaunchedEffect(song.id) { while (true) { position = graph.player.position; delay(200) } }
                val current = l.lines.indexOfLast { it.timeMs <= position }.coerceAtLeast(0)
                val list = rememberLazyListState()
                LaunchedEffect(current) { list.animateScrollToItem((current - 2).coerceAtLeast(0)) }
                LazyColumn(state = list, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 96.dp)) {
                    itemsIndexed(l.lines) { i, line ->
                        val color by animateColorAsState(if (i == current) Color.White else Color.White.copy(alpha = 0.38f), label = "line")
                        Text(
                            line.text.ifBlank { "♪" },
                            style = Type.Title.copy(fontSize = 28.sp, lineHeight = 34.sp), color = color,
                            modifier = Modifier.fillMaxWidth().clickable { graph.player.seek(line.timeMs) }.padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
        IconButton(onClick = onClose, modifier = Modifier.padding(top = 36.dp, start = 8.dp)) {
            Icon(Icons.Rounded.KeyboardArrowDown, "Close", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}
