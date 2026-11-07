package com.verza.data.library

import com.verza.data.Album
import com.verza.data.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Your library on the phone: what you played, liked, downloaded and put in playlists. */
class Library(db: LibraryDb) {
    private val songs = db.songs()
    private val lists = db.playlists()
    private val plays = db.plays()
    private val sync = db.sync()

    val recent: Flow<List<Song>> = songs.recent().map { rows -> rows.map { it.toSong() } }
    val liked: Flow<List<Song>> = songs.liked().map { rows -> rows.map { it.toSong() } }
    val downloaded: Flow<List<Song>> = songs.downloaded().map { rows -> rows.map { it.toSong() } }
    val playlists: Flow<List<PlaylistSummary>> = lists.all()

    /** A playlist as a page: its name and songs, kept current as songs are added. */
    fun playlist(id: Long): Flow<Album> = lists.songs(id).map { rows ->
        Album(title = lists.get(id)?.name ?: "Playlist", subtitle = "Playlist", art = rows.firstOrNull()?.thumbnailUrl, songs = rows.map { it.toSong() })
    }

    suspend fun downloadPath(id: String): String? = songs.downloadPath(id)

    /** Downloaded songs' rows, with where each file is. */
    suspend fun downloadRows(): List<SongRow> = songs.downloadRows()

    val likedIds: Flow<Set<String>> = songs.likedIds().map { it.toSet() }

    /**
     * Likes or unlikes [song] here, and queues the same for the YouTube Music account: written down
     * first and sent after, so a like made offline still reaches the account.
     */
    suspend fun setLiked(song: Song, liked: Boolean) {
        val row = songs.get(song.id) ?: SongRow(song.id, song.title, song.artist, song.art, song.durationMs)
        songs.put(row.copy(liked = liked, likedAt = if (liked) System.currentTimeMillis() else null))
        if (!song.onPhone) sync.add(SyncRow(op = if (liked) "like" else "unlike", videoId = song.id))
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
