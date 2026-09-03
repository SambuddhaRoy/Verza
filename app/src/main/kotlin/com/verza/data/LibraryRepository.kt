package com.verza.data

import com.verza.data.db.SongDao
import com.verza.data.db.SongEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Local library: play history and liked songs, backed by Room. */
@Singleton
class LibraryRepository @Inject constructor(
    private val dao: SongDao,
    private val sync: YouTubeSync,
) {
    fun recentlyPlayed(): Flow<List<SongEntity>> = dao.recentlyPlayed()
    fun liked(): Flow<List<SongEntity>> = dao.liked()
    fun likedIds(): Flow<List<String>> = dao.likedIds()
    fun downloaded(): Flow<List<SongEntity>> = dao.downloaded()
    fun downloadedIds(): Flow<List<String>> = dao.downloadedIds()

    suspend fun get(id: String): SongEntity? = dao.get(id)

    /**
     * Everything the listener already knows — every song ever played or liked. Discovery radio uses
     * this to skip tracks they've heard and to de-prioritise artists they already listen to.
     */
    suspend fun known(): DiscoveryRadio.Known {
        val all = dao.getAll()
        return DiscoveryRadio.Known(
            videoIds = all.map { it.id }.toSet(),
            artists = all.map { DiscoveryRadio.primaryArtist(it.artist) }.filter { it.isNotEmpty() }.toSet(),
        )
    }

    /** Marks a song as downloaded, recording the absolute path on disk. */
    suspend fun markDownloaded(song: SongEntity, path: String) {
        val existing = dao.get(song.id)
        dao.upsert(
            (existing ?: song).copy(
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl ?: existing?.thumbnailUrl,
                durationMs = if (song.durationMs > 0) song.durationMs else existing?.durationMs ?: 0,
                downloadPath = path,
            )
        )
    }

    /** Clears the [downloadPath] for [id]. The caller deletes the file. */
    suspend fun clearDownloadPath(id: String) {
        val existing = dao.get(id) ?: return
        dao.upsert(existing.copy(downloadPath = null))
    }

    /** Stamps a song as played now, preserving any existing liked state. */
    suspend fun recordPlayed(song: SongEntity) {
        val existing = dao.get(song.id)
        dao.upsert(
            (existing ?: song).copy(
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl ?: existing?.thumbnailUrl,
                durationMs = if (song.durationMs > 0) song.durationMs else existing?.durationMs ?: 0,
                lastPlayedAt = song.lastPlayedAt ?: System.currentTimeMillis(),
            )
        )
    }

    /** Toggles the liked flag for a song, inserting it if it wasn't known yet. */
    suspend fun toggleLike(song: SongEntity) {
        val existing = dao.get(song.id)
        val nowLiked = !(existing?.liked ?: false)
        dao.upsert(
            (existing ?: song).copy(
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl ?: existing?.thumbnailUrl,
                liked = nowLiked,
                likedAt = if (nowLiked) System.currentTimeMillis() else null,
            )
        )
        // Queue it for the account rather than firing it off here. This was an inline call
        // wrapped in runCatching, so a like made with no signal was swallowed and never sent: the
        // app looked synced and was not.
        sync.like(song.id, nowLiked)
    }
}
