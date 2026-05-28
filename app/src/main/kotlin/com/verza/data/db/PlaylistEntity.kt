package com.verza.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user-created local playlist (lives entirely in Room, no server sync). */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Join row tying a song to a playlist. The song row may live independently in `songs` (e.g. it
 * was liked / downloaded / played) — we cascade-delete here only on playlist removal so the song
 * record is preserved.
 */
@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId"), Index("songId")],
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val songId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis(),
)

/** Convenience tuple — a playlist with its first track's artwork for the card thumbnail. */
data class PlaylistWithCover(
    @androidx.room.Embedded val playlist: PlaylistEntity,
    val coverUrl: String?,
    val trackCount: Int,
)
