package com.verza.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.verza.data.Song
import com.verza.data.artAt
import com.verza.data.mixes.StatsRepository
import com.verza.graph
import com.verza.ui.kit.Empty
import com.verza.ui.kit.ListRow
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.SectionTitle
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import java.time.LocalDate

/** Your Sound: how much you've listened, your days in a row, top songs and artists, and when you listen. */
@Composable
fun YourSound() {
    val graph = LocalContext.current.graph
    val stats = remember { StatsRepository(graph.db.stats()) }
    val plays by stats.totalPlays.collectAsState(initial = null)
    val ms by stats.totalListenedMs.collectAsState(initial = 0L)
    val days by stats.playDays.collectAsState(initial = emptyList())
    val songs by stats.topSongs(10).collectAsState(initial = emptyList())
    val artists by stats.topArtists(8).collectAsState(initial = emptyList())
    val hours by stats.hourlyTotals().collectAsState(initial = emptyList())
    val p = LocalPalette.current
    if (plays == 0) { Empty("Listen for a while and your stats appear here"); return }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp + LocalDockHeight.current)) {
        item {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Figure("${ms / 3_600_000}", "hours", Modifier.weight(1f))
                Figure("${plays ?: 0}", "plays", Modifier.weight(1f))
                Figure("${streak(days)}", "days in a row", Modifier.weight(1f))
            }
        }
        if (hours.isNotEmpty()) item {
            SectionTitle("when you listen")
            // A bar for each hour of the day.
            val max = hours.maxOf { it.totalMs }.coerceAtLeast(1)
            val byHour = hours.associate { it.hour to it.totalMs }
            Canvas(Modifier.fillMaxWidth().height(80.dp)) {
                val w = size.width / 24
                for (h in 0 until 24) {
                    val v = (byHour[h] ?: 0L).toFloat() / max
                    drawRect(p.accent.copy(alpha = 0.35f + 0.65f * v), Offset(h * w + 1, size.height * (1 - v)), Size(w - 2, size.height * v))
                }
            }
            Row(Modifier.fillMaxWidth()) {
                for (label in listOf("midnight", "6am", "noon", "6pm")) Text(label, style = Type.Small, color = p.inkMuted, modifier = Modifier.weight(1f))
            }
        }
        if (songs.isNotEmpty()) item { SectionTitle("top songs") }
        items(songs, key = { "s" + it.id }) { s ->
            val song = Song(s.id, s.title, s.artist, art = s.thumbnailUrl)
            ListRow(s.title, "${s.artist} · ${s.plays} plays", art = s.thumbnailUrl?.let { artAt(it, 226) }, onClick = { graph.player.play(listOf(song)) })
        }
        if (artists.isNotEmpty()) item { SectionTitle("top artists") }
        items(artists, key = { "a" + it.artist }) { a ->
            Text("${a.artist}  ·  ${a.totalMs / 60_000} min", style = Type.RowTitle, color = p.ink, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun Figure(value: String, label: String, modifier: Modifier) {
    val p = LocalPalette.current
    Column(modifier) {
        Text(value, style = Type.Masthead.copy(fontSize = Type.Section.fontSize * 1.4f), color = p.ink)
        Text(label, style = Type.RowSubtitle, color = p.inkMuted)
    }
}

/** Days in a row up to today (or yesterday, if today has no listening yet). [days] are yyyy-MM-dd. */
private fun streak(days: List<String>): Int {
    val set = days.toSet()
    var day = LocalDate.now().let { if (it.toString() in set) it else it.minusDays(1) }
    var n = 0
    while (day.toString() in set) { n++; day = day.minusDays(1) }
    return n
}
