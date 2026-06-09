package com.verza.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayEventDao {

    @Insert
    suspend fun insert(event: PlayEventEntity)

    @Query("DELETE FROM play_events")
    suspend fun clearAll()

    // ── Backup / export ─────────────────────────────────────────────────────────
    @Query("SELECT * FROM play_events")
    suspend fun getAll(): List<PlayEventEntity>

    /** True if an identical event already exists — keeps re-importing a backup from double-counting. */
    @Query("SELECT EXISTS(SELECT 1 FROM play_events WHERE songId = :songId AND playedAt = :playedAt AND listenedMs = :listenedMs)")
    suspend fun exists(songId: String, playedAt: Long, listenedMs: Long): Boolean

    @Query("SELECT COUNT(*) FROM play_events")
    fun totalPlays(): Flow<Int>

    @Query("SELECT COALESCE(SUM(listenedMs), 0) FROM play_events")
    fun totalListenedMs(): Flow<Long>

    // Top tracks by real listened time. Joins songs for display metadata; a track always has a
    // songs row by the time we log an event (recordPlayed upserts it when the track starts).
    @Query(
        """
        SELECT s.id AS id, s.title AS title, s.artist AS artist, s.thumbnailUrl AS thumbnailUrl,
               SUM(e.listenedMs) AS totalMs, COUNT(*) AS plays
        FROM play_events e
        JOIN songs s ON s.id = e.songId
        GROUP BY e.songId
        ORDER BY totalMs DESC
        LIMIT :limit
        """
    )
    fun topSongs(limit: Int): Flow<List<SongStat>>

    @Query(
        """
        SELECT s.artist AS artist, SUM(e.listenedMs) AS totalMs, COUNT(*) AS plays
        FROM play_events e
        JOIN songs s ON s.id = e.songId
        WHERE s.artist <> ''
        GROUP BY s.artist
        ORDER BY totalMs DESC
        LIMIT :limit
        """
    )
    fun topArtists(limit: Int): Flow<List<ArtistStat>>

    /** Distinct local calendar days that have at least one play, newest first ("yyyy-MM-dd"). */
    @Query(
        "SELECT DISTINCT date(playedAt / 1000, 'unixepoch', 'localtime') FROM play_events ORDER BY 1 DESC"
    )
    fun distinctPlayDays(): Flow<List<String>>

    /** Listened time bucketed by local hour-of-day (0–23) — powers the "when you listen" chart. */
    @Query(
        """
        SELECT CAST(strftime('%H', playedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hour,
               SUM(listenedMs) AS totalMs
        FROM play_events
        GROUP BY hour
        ORDER BY hour
        """
    )
    fun hourlyTotals(): Flow<List<HourStat>>

    /** Tracks you keep coming back to — ranked by play *count* (then time). "Comfort songs." */
    @Query(
        """
        SELECT s.id AS id, s.title AS title, s.artist AS artist, s.thumbnailUrl AS thumbnailUrl,
               SUM(e.listenedMs) AS totalMs, COUNT(*) AS plays
        FROM play_events e
        JOIN songs s ON s.id = e.songId
        GROUP BY e.songId
        HAVING plays >= 2
        ORDER BY plays DESC, totalMs DESC
        LIMIT :limit
        """
    )
    fun mostReplayed(limit: Int): Flow<List<SongStat>>

    /** Epoch-millis of the very first logged play, for the "listening since …" line. */
    @Query("SELECT MIN(playedAt) FROM play_events")
    fun firstPlayedAt(): Flow<Long?>

    // ── Curated mixes (one-shot reads used by MixesRepository) ───────────────────

    /** Top tracks by listened time *within a set of local hours-of-day* — seeds the Daylist mix. */
    @Query(
        """
        SELECT s.id AS id, s.title AS title, s.artist AS artist, s.thumbnailUrl AS thumbnailUrl,
               SUM(e.listenedMs) AS totalMs, COUNT(*) AS plays
        FROM play_events e
        JOIN songs s ON s.id = e.songId
        WHERE CAST(strftime('%H', e.playedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) IN (:hours)
        GROUP BY e.songId
        ORDER BY totalMs DESC
        LIMIT :limit
        """
    )
    suspend fun topSongsInHours(hours: List<Int>, limit: Int): List<SongStat>

    /** Distinct song ids the user has ever played — the "already heard" set for Discover. */
    @Query("SELECT DISTINCT songId FROM play_events")
    suspend fun playedSongIds(): List<String>

    /** One-shot top songs (non-Flow) for background mix generation. */
    @Query(
        """
        SELECT s.id AS id, s.title AS title, s.artist AS artist, s.thumbnailUrl AS thumbnailUrl,
               SUM(e.listenedMs) AS totalMs, COUNT(*) AS plays
        FROM play_events e
        JOIN songs s ON s.id = e.songId
        GROUP BY e.songId
        ORDER BY totalMs DESC
        LIMIT :limit
        """
    )
    suspend fun topSongsOnce(limit: Int): List<SongStat>

    /** One-shot top artists (non-Flow) for background mix generation. */
    @Query(
        """
        SELECT s.artist AS artist, SUM(e.listenedMs) AS totalMs, COUNT(*) AS plays
        FROM play_events e
        JOIN songs s ON s.id = e.songId
        WHERE s.artist <> ''
        GROUP BY s.artist
        ORDER BY totalMs DESC
        LIMIT :limit
        """
    )
    suspend fun topArtistsOnce(limit: Int): List<ArtistStat>
}
