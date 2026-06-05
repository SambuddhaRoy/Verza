package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.StatsRepository
import com.verza.data.db.ArtistStat
import com.verza.data.db.HourStat
import com.verza.data.db.SongStat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/** The shape of *when* a person listens across the day, plus a one-line personality. */
data class ListeningFingerprint(
    /** 24 values (one per hour, 0..23), each normalised 0..1 against the peak hour. */
    val shape: List<Float> = emptyList(),
    val peakHour: Int = 0,
    val daypartLabel: String = "",
    val hasData: Boolean = false,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    stats: StatsRepository,
) : ViewModel() {

    val totalPlays: StateFlow<Int> = stats.totalPlays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val totalListenedMs: StateFlow<Long> = stats.totalListenedMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val topSongs: StateFlow<List<SongStat>> = stats.topSongs(10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topArtists: StateFlow<List<ArtistStat>> = stats.topArtists(8)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Tracks you keep coming back to (ranked by play count). */
    val comfortSongs: StateFlow<List<SongStat>> = stats.mostReplayed(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Your listening "fingerprint": a 24-hour shape + the peak hour + a day-part personality. */
    val fingerprint: StateFlow<ListeningFingerprint> = stats.hourlyTotals()
        .map { buildFingerprint(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListeningFingerprint())

    /** Epoch-millis of the first ever play, for "listening since …". */
    val firstPlayedAt: StateFlow<Long?> = stats.firstPlayedAt
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Consecutive-day listening streak, anchored at today (or yesterday if nothing today yet). */
    val dayStreak: StateFlow<Int> = stats.playDays
        .map { computeStreak(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private fun buildFingerprint(hours: List<HourStat>): ListeningFingerprint {
        val byHour = LongArray(24)
        hours.forEach { if (it.hour in 0..23) byHour[it.hour] = it.totalMs }
        val max = byHour.maxOrNull() ?: 0L
        if (max <= 0L) return ListeningFingerprint()
        val shape = byHour.map { (it.toFloat() / max).coerceIn(0f, 1f) }
        val peak = byHour.indices.maxByOrNull { byHour[it] } ?: 0

        fun sum(hs: IntRange) = hs.sumOf { byHour[it] }
        val morning = sum(5..11)
        val afternoon = sum(12..16)
        val evening = sum(17..21)
        val night = sum(22..23) + sum(0..4)
        val label = listOf(
            "an early riser" to morning,
            "a daytime listener" to afternoon,
            "an evening listener" to evening,
            "a night owl" to night,
        ).maxByOrNull { it.second }?.first ?: ""

        return ListeningFingerprint(shape = shape, peakHour = peak, daypartLabel = label, hasData = true)
    }

    private fun computeStreak(daysDesc: List<String>): Int {
        if (daysDesc.isEmpty()) return 0
        val days = daysDesc.toHashSet()
        val today = LocalDate.now()
        // Allow the streak to count from today, or from yesterday if the user hasn't played today
        // yet — otherwise an active streak would appear broken until the first play of the day.
        var cursor = when {
            days.contains(today.toString()) -> today
            days.contains(today.minusDays(1).toString()) -> today.minusDays(1)
            else -> return 0
        }
        var count = 0
        while (days.contains(cursor.toString())) {
            count++
            cursor = cursor.minusDays(1)
        }
        return count
    }
}
