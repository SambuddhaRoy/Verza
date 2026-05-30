package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.StatsRepository
import com.verza.data.db.ArtistStat
import com.verza.data.db.SongStat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

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

    /** Consecutive-day listening streak, anchored at today (or yesterday if nothing today yet). */
    val dayStreak: StateFlow<Int> = stats.playDays
        .map { computeStreak(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

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
