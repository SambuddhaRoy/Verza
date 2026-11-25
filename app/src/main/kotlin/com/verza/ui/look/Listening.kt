package com.verza.ui.look

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.verza.data.mixes.StatsRepository
import com.verza.graph

/** What the play log says, for the looks that show it: the grid's figures, the game's level. */
@Immutable
data class Listening(
    val plays: Int,
    val minutes: Long,
    /** Days in a row, up to today or yesterday. */
    val streak: Int,
    /** The artist played most. */
    val topArtist: String?,
    /** Of all listening, how much was between 10pm and 4am. */
    val nightShare: Float,
)

@Composable
fun rememberListening(): Listening {
    val graph = LocalContext.current.graph
    val stats = remember { StatsRepository(graph.db.stats()) }
    val plays by stats.totalPlays.collectAsState(initial = 0)
    val ms by stats.totalListenedMs.collectAsState(initial = 0L)
    val days by stats.playDays.collectAsState(initial = emptyList())
    val artists by stats.topArtists(1).collectAsState(initial = emptyList())
    val hours by stats.hourlyTotals().collectAsState(initial = emptyList())
    val total = hours.sumOf { it.totalMs }.coerceAtLeast(1)
    val night = hours.filter { it.hour >= 22 || it.hour < 4 }.sumOf { it.totalMs }
    return Listening(plays, ms / 60_000, com.verza.ui.library.streak(days), artists.firstOrNull()?.artist, night.toFloat() / total)
}
