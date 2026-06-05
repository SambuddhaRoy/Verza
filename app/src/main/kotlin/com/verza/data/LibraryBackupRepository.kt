package com.verza.data

import com.verza.data.db.PlayEventDao
import com.verza.data.db.PlayEventEntity
import com.verza.data.db.PlaylistDao
import com.verza.data.db.PlaylistEntity
import com.verza.data.db.SongDao
import com.verza.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

// ── Portable backup format ────────────────────────────────────────────────────
// A plain, human-readable JSON snapshot of everything the user has built up locally: their songs
// (with like/last-played state), their playlists, and their listening-stats event log. Deliberately
// excludes device-specific bits (download paths) and anything sensitive (the sign-in cookie is
// never touched). "Your library is a file you own."

@Serializable
data class LibraryBackup(
    val format: String = "verza-library",
    val version: Int = 1,
    val exportedAt: Long,
    val songs: List<BackupSong> = emptyList(),
    val playlists: List<BackupPlaylist> = emptyList(),
    val playEvents: List<BackupPlayEvent> = emptyList(),
)

@Serializable
data class BackupSong(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0,
    val liked: Boolean = false,
    val likedAt: Long? = null,
    val lastPlayedAt: Long? = null,
)

@Serializable
data class BackupPlaylist(
    val name: String,
    val createdAt: Long,
    val trackIds: List<String> = emptyList(),
)

@Serializable
data class BackupPlayEvent(
    val songId: String,
    val playedAt: Long,
    val listenedMs: Long,
)

/** What an import actually changed, for a friendly confirmation message. */
data class ImportSummary(val songs: Int, val playlists: Int, val plays: Int)

@Singleton
class LibraryBackupRepository @Inject constructor(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
    private val playEventDao: PlayEventDao,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    /** Serialises the whole local library to a JSON string. */
    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val songs = songDao.getAll().map {
            BackupSong(it.id, it.title, it.artist, it.thumbnailUrl, it.durationMs, it.liked, it.likedAt, it.lastPlayedAt)
        }
        val playlists = playlistDao.allOnce().map { p ->
            BackupPlaylist(p.name, p.createdAt, playlistDao.trackEntriesOf(p.id).map { it.songId })
        }
        val events = playEventDao.getAll().map { BackupPlayEvent(it.songId, it.playedAt, it.listenedMs) }
        json.encodeToString(LibraryBackup(exportedAt = System.currentTimeMillis(), songs = songs, playlists = playlists, playEvents = events))
    }

    /**
     * Merges a backup into the current library: upserts songs (unioning like state and keeping the
     * newest last-played), recreates playlists that don't already exist by name, and adds any
     * play-events not already present. Non-destructive and idempotent.
     */
    suspend fun importJson(text: String): ImportSummary = withContext(Dispatchers.IO) {
        val backup = json.decodeFromString<LibraryBackup>(text)

        backup.songs.forEach { s ->
            val existing = songDao.get(s.id)
            songDao.upsert(
                SongEntity(
                    id = s.id,
                    title = s.title.ifBlank { existing?.title ?: "Unknown" },
                    artist = s.artist.ifBlank { existing?.artist ?: "" },
                    thumbnailUrl = s.thumbnailUrl ?: existing?.thumbnailUrl,
                    durationMs = if (s.durationMs > 0) s.durationMs else existing?.durationMs ?: 0,
                    liked = s.liked || (existing?.liked ?: false),
                    likedAt = s.likedAt ?: existing?.likedAt,
                    lastPlayedAt = maxOf(s.lastPlayedAt ?: 0L, existing?.lastPlayedAt ?: 0L).takeIf { it > 0L },
                    downloadPath = existing?.downloadPath, // device-specific — never imported
                )
            )
        }

        var playlistsAdded = 0
        backup.playlists.forEach { pl ->
            if (playlistDao.idByName(pl.name) == null) {
                val id = playlistDao.create(PlaylistEntity(name = pl.name, createdAt = pl.createdAt))
                pl.trackIds.forEach { songId ->
                    if (songDao.get(songId) != null) playlistDao.addTrack(id, songId)
                }
                playlistsAdded++
            }
        }

        var playsAdded = 0
        backup.playEvents.forEach { e ->
            if (songDao.get(e.songId) != null && !playEventDao.exists(e.songId, e.playedAt, e.listenedMs)) {
                playEventDao.insert(PlayEventEntity(songId = e.songId, playedAt = e.playedAt, listenedMs = e.listenedMs))
                playsAdded++
            }
        }

        ImportSummary(songs = backup.songs.size, playlists = playlistsAdded, plays = playsAdded)
    }
}
