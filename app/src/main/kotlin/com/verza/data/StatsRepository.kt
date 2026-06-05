package com.verza.data

import com.verza.data.db.ArtistStat
import com.verza.data.db.HourStat
import com.verza.data.db.PlayEventDao
import com.verza.data.db.PlayEventEntity
import com.verza.data.db.SongStat
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Listening statistics backed by the play-event log (see [PlayEventEntity]). */
@Singleton
class StatsRepository @Inject constructor(
    private val dao: PlayEventDao,
) {
    val totalPlays: Flow<Int> = dao.totalPlays()
    val totalListenedMs: Flow<Long> = dao.totalListenedMs()
    val playDays: Flow<List<String>> = dao.distinctPlayDays()

    fun topSongs(limit: Int = 10): Flow<List<SongStat>> = dao.topSongs(limit)
    fun topArtists(limit: Int = 8): Flow<List<ArtistStat>> = dao.topArtists(limit)
    fun hourlyTotals(): Flow<List<HourStat>> = dao.hourlyTotals()
    fun mostReplayed(limit: Int = 5): Flow<List<SongStat>> = dao.mostReplayed(limit)
    val firstPlayedAt: Flow<Long?> = dao.firstPlayedAt()

    /** Wipes all listening stats. */
    suspend fun reset() = dao.clearAll()

    /** Logs a finished listen. Ignored for trivially short plays (< 5s) to keep stats honest. */
    suspend fun record(songId: String, listenedMs: Long) {
        if (songId.isBlank() || listenedMs < 5_000) return
        dao.insert(
            PlayEventEntity(
                songId = songId,
                playedAt = System.currentTimeMillis(),
                listenedMs = listenedMs,
            )
        )
    }
}
