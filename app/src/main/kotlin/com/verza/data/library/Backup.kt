package com.verza.data.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The old app's backup file, field for field, so a backup made there imports here. Everything you
// built up: songs (liked and last played), playlists, and the listening log. Never the sign-in
// cookie, and never where a download is, which only means something on the phone that made it.

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
    val id: String, val title: String, val artist: String, val thumbnailUrl: String? = null, val durationMs: Long = 0,
    val liked: Boolean = false, val likedAt: Long? = null, val lastPlayedAt: Long? = null,
)

@Serializable
data class BackupPlaylist(val name: String, val createdAt: Long, val trackIds: List<String> = emptyList())

@Serializable
data class BackupPlayEvent(val songId: String, val playedAt: Long, val listenedMs: Long)

/** What an import added, for the message after it. */
data class Imported(val songs: Int, val playlists: Int, val plays: Int)

class Backup(db: LibraryDb) {
    private val songs = db.songs()
    private val lists = db.playlists()
    private val stats = db.stats()
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(): String = withContext(Dispatchers.IO) {
        json.encodeToString(LibraryBackup.serializer(), LibraryBackup(
            exportedAt = System.currentTimeMillis(),
            songs = songs.all().map { BackupSong(it.id, it.title, it.artist, it.thumbnailUrl, it.durationMs, it.liked, it.likedAt, it.lastPlayedAt) },
            playlists = lists.allOnce().map { p -> BackupPlaylist(p.name, p.createdAt, lists.songIds(p.id)) },
            playEvents = stats.getAll().map { BackupPlayEvent(it.songId, it.playedAt, it.listenedMs) },
        ))
    }

    /**
     * Merges a backup in, taking nothing away: a song keeps either side's like and the later last
     * play, a playlist is added unless one of that name exists, and a play already logged isn't
     * logged twice. Importing the same file again changes nothing.
     */
    suspend fun import(text: String): Imported = withContext(Dispatchers.IO) {
        val backup = json.decodeFromString(LibraryBackup.serializer(), text)
        for (s in backup.songs) {
            val had = songs.get(s.id)
            songs.put(SongRow(
                id = s.id,
                title = s.title.ifBlank { had?.title ?: "Unknown" },
                artist = s.artist.ifBlank { had?.artist.orEmpty() },
                thumbnailUrl = s.thumbnailUrl ?: had?.thumbnailUrl,
                durationMs = if (s.durationMs > 0) s.durationMs else had?.durationMs ?: 0,
                liked = s.liked || had?.liked == true,
                likedAt = s.likedAt ?: had?.likedAt,
                lastPlayedAt = maxOf(s.lastPlayedAt ?: 0, had?.lastPlayedAt ?: 0).takeIf { it > 0 },
                downloadPath = had?.downloadPath,
            ))
        }
        var added = 0
        for (pl in backup.playlists) {
            if (lists.idByName(pl.name) != null) continue
            val id = lists.create(PlaylistRow(name = pl.name, createdAt = pl.createdAt))
            pl.trackIds.forEach { songId -> if (songs.get(songId) != null) lists.add(PlaylistTrackRow(id, songId, lists.nextPosition(id))) }
            added++
        }
        var plays = 0
        for (e in backup.playEvents) {
            if (songs.get(e.songId) != null && !stats.exists(e.songId, e.playedAt, e.listenedMs)) {
                stats.insert(PlayRow(songId = e.songId, playedAt = e.playedAt, listenedMs = e.listenedMs))
                plays++
            }
        }
        Imported(backup.songs.size, added, plays)
    }
}
