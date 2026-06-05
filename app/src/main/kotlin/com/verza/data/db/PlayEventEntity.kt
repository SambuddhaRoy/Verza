package com.verza.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per "meaningful" listen of a track — logged when a track is switched away from
 * (or playback stops) and the user actually listened for >= 5s. Powers the "Your Sound"
 * listening-stats page: totals, top tracks/artists by real listened time, and day streaks.
 *
 * Kept as an append-only event log (rather than a counter on [SongEntity]) so we can compute
 * time-based things like streaks and accurate minutes, not just play counts.
 */
@Entity(
    tableName = "play_events",
    indices = [Index("songId")],
)
data class PlayEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val playedAt: Long,
    /** Actual time the user listened to this track in this session, in milliseconds. */
    val listenedMs: Long,
)

/** Aggregate projection: one artist's total listened time + play count. */
data class ArtistStat(
    val artist: String,
    @ColumnInfo("totalMs") val totalMs: Long,
    val plays: Int,
)

/** Aggregate projection: one track's display fields + total listened time + play count. */
data class SongStat(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    @ColumnInfo("totalMs") val totalMs: Long,
    val plays: Int,
)

/** Aggregate projection: total listened time in a given hour-of-day bucket (0–23, local time). */
data class HourStat(
    val hour: Int,
    @ColumnInfo("totalMs") val totalMs: Long,
)
