package com.verza.data.mixes

import kotlinx.coroutines.flow.Flow

/** Listening statistics backed by the play-event log (see [com.verza.data.library.PlayRow]). */
class StatsRepository(
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

    /** Every song ever played, scored by how much and how lately it has been listened to. See [Taste]. */
    suspend fun taste(): List<Taste.ScoredSong> = Taste.songs(dao.playsWithSongs(), System.currentTimeMillis())

    /** Wipes all listening stats. */
    suspend fun reset() = dao.clearAll()

    /** Logs a finished listen. Ignored for trivially short plays (< 5s) to keep stats honest. */
    suspend fun record(songId: String, listenedMs: Long) {
        if (songId.isBlank() || listenedMs < 5_000) return
        dao.insert(
            com.verza.data.library.PlayRow(
                songId = songId,
                playedAt = System.currentTimeMillis(),
                listenedMs = listenedMs,
            )
        )
    }
}
