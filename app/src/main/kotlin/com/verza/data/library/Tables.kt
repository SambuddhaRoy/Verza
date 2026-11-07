package com.verza.data.library

import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.RoomDatabase
import com.verza.data.Song

// The old app's tables, field for field and in the same order. Room keeps a hash of the schema in
// the database and refuses to open one whose hash differs, so this is what lets the new app open the
// old `verza.db` as it is. Change a table only with a new version and a migration.

/** A song that was played, liked or downloaded. */
@Entity(tableName = "songs")
data class SongRow(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val liked: Boolean = false,
    val likedAt: Long? = null,
    val lastPlayedAt: Long? = null,
    /** Where the downloaded file is: a content:// URI or a path. */
    val downloadPath: String? = null,
) {
    fun toSong() = Song(id = id, title = title, artist = artist, art = thumbnailUrl, durationMs = durationMs)
}

/** A playlist made in Verza. [remoteId] is its YouTube Music copy, once it has one. */
@Entity(tableName = "playlists")
data class PlaylistRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val remoteId: String? = null,
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [ForeignKey(entity = PlaylistRow::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playlistId"), Index("songId")],
)
data class PlaylistTrackRow(
    val playlistId: Long,
    val songId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis(),
)

/** One real listen (5s or more), for Your Sound. */
@Entity(tableName = "play_events", indices = [Index("songId")])
data class PlayRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val playedAt: Long,
    val listenedMs: Long,
)

/** A change still to be sent to the YouTube Music account (a like, a new playlist). */
@Entity(tableName = "sync_ops")
data class SyncRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val op: String,
    val videoId: String = "",
    val playlistId: Long = 0,
    val remoteId: String = "",
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
)

@Database(
    entities = [SongRow::class, PlaylistRow::class, PlaylistTrackRow::class, PlayRow::class, SyncRow::class],
    version = 5,
    exportSchema = false,
)
abstract class LibraryDb : RoomDatabase() {
    abstract fun songs(): SongDao
    abstract fun playlists(): PlaylistDao
    abstract fun plays(): PlayDao
    abstract fun sync(): SyncDao
}
