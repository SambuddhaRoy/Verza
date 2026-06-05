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
}
