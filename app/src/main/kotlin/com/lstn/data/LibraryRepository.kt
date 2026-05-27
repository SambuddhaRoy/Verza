package com.lstn.data

import com.lstn.data.db.SongDao
import com.lstn.data.db.SongEntity
import com.lstn.innertube.InnerTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Local library: play history and liked songs, backed by Room. */
@Singleton
class LibraryRepository @Inject constructor(
    private val dao: SongDao,
) {
    fun recentlyPlayed(): Flow<List<SongEntity>> = dao.recentlyPlayed()
    fun liked(): Flow<List<SongEntity>> = dao.liked()
    fun likedIds(): Flow<List<String>> = dao.likedIds()
    fun downloaded(): Flow<List<SongEntity>> = dao.downloaded()
    fun downloadedIds(): Flow<List<String>> = dao.downloadedIds()

    suspend fun get(id: String): SongEntity? = dao.get(id)

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
        // Mirror the like to the signed-in YouTube Music account (no-op when signed out).
        runCatching { withContext(Dispatchers.IO) { InnerTube.setLikeStatus(song.id, nowLiked) } }
    }
}
