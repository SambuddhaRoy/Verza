package com.verza.data.library

import com.verza.data.Album
import com.verza.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Your library on the phone: what you played, liked, downloaded and put in playlists. */
class Library(db: LibraryDb, scope: CoroutineScope) {
    private val songs = db.songs()
    private val lists = db.playlists()
    private val plays = db.plays()
    private val sync = db.sync()

    val recent: Flow<List<Song>> = songs.recent().map { rows -> rows.map { it.toSong() } }
    val liked: Flow<List<Song>> = songs.liked().map { rows -> rows.map { it.toSong() } }
    val downloaded: Flow<List<Song>> = songs.downloaded().map { rows -> rows.map { it.toSong() } }
    val playlists: Flow<List<PlaylistSummary>> = lists.all()

    /** A playlist as a page: its name and songs, kept current as it's renamed and songs come and go. */
    fun playlist(id: Long): Flow<Album> = combine(lists.name(id), lists.songs(id)) { name, rows ->
        Album(title = name ?: "Playlist", subtitle = "Playlist", art = rows.firstOrNull()?.thumbnailUrl, songs = rows.map { it.toSong() })
    }

    suspend fun downloadPath(id: String): String? = songs.downloadPath(id)

    /** Downloaded songs' rows, with where each file is. */
    suspend fun downloadRows(): List<SongRow> = songs.downloadRows()

    // One query each for the whole app: every song row's menu reads these.
    val likedIds: StateFlow<Set<String>> = songs.likedIds().map { it.toSet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())
    val downloadedIds: StateFlow<Set<String>> = songs.downloadedIds().map { it.toSet() }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    /**
     * Likes or unlikes [song] here, and queues the same for the YouTube Music account: written down
     * first and sent after, so a like made offline still reaches the account.
     */
    suspend fun setLiked(song: Song, liked: Boolean) {
        val row = songs.get(song.id) ?: SongRow(song.id, song.title, song.artist, song.art, song.durationMs)
        songs.put(row.copy(liked = liked, likedAt = if (liked) System.currentTimeMillis() else null))
        if (!song.onPhone) sync.add(SyncRow(op = if (liked) "like" else "unlike", videoId = song.id))
    }

    /** A new playlist, made here and queued to be made on the account too. Its id. */
    suspend fun createPlaylist(name: String): Long {
        val id = lists.create(PlaylistRow(name = name))
        sync.add(SyncRow(op = "create_playlist", playlistId = id, title = name))
        return id
    }

    /** [song] at the end of the playlist, here and, once the playlist is on the account, there. */
    suspend fun addToPlaylist(playlistId: Long, song: Song) {
        if (songs.get(song.id) == null) songs.put(SongRow(song.id, song.title, song.artist, song.art, song.durationMs))
        lists.add(PlaylistTrackRow(playlistId, song.id, lists.nextPosition(playlistId)))
        if (!song.onPhone) sync.add(SyncRow(op = "add_to_playlist", videoId = song.id, playlistId = playlistId, remoteId = lists.get(playlistId)?.remoteId.orEmpty()))
    }

    /** [songId] out of the playlist, here and on the account's copy. */
    suspend fun removeFromPlaylist(playlistId: Long, songId: String) {
        lists.remove(playlistId, songId)
        if (!songId.startsWith("content://") && !songId.startsWith("file://"))
            sync.add(SyncRow(op = "remove_from_playlist", videoId = songId, playlistId = playlistId, remoteId = lists.get(playlistId)?.remoteId.orEmpty()))
    }

    suspend fun renamePlaylist(id: Long, name: String) {
        lists.rename(id, name)
        sync.add(SyncRow(op = "rename_playlist", playlistId = id, title = name, remoteId = lists.get(id)?.remoteId.orEmpty()))
    }

    /** Gone here, and from the account once it was made there; never made there, nothing is sent. */
    suspend fun deletePlaylist(id: Long) {
        val remote = lists.get(id)?.remoteId
        lists.clear(id)
        lists.delete(id)
        sync.dropFor(id)
        if (!remote.isNullOrEmpty()) sync.add(SyncRow(op = "delete_playlist", remoteId = remote))
    }

    suspend fun forgetDownload(id: String) = songs.forgetDownload(id)

    /** [song] is saved at [path] (a content:// URI or a file path), and plays from there. */
    suspend fun downloaded(song: Song, path: String) {
        val row = songs.get(song.id) ?: SongRow(song.id, song.title, song.artist, song.art, song.durationMs)
        songs.put(row.copy(downloadPath = path))
    }

    /** The song started playing: it goes to the top of Recent, keeping whatever else it had. */
    suspend fun played(song: Song) {
        val row = songs.get(song.id)
        songs.put(
            (row ?: SongRow(song.id, song.title, song.artist, song.art, song.durationMs)).copy(
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.art ?: row?.thumbnailUrl,
                lastPlayedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** A listen of [ms], counted for Your Sound when it's at least five seconds. */
    suspend fun listened(id: String, ms: Long) {
        if (ms >= 5_000) plays.add(PlayRow(songId = id, playedAt = System.currentTimeMillis() - ms, listenedMs = ms))
    }
}
