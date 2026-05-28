package com.verza.data

import com.verza.data.db.PlaylistDao
import com.verza.data.db.PlaylistWithCover
import com.verza.data.db.SongDao
import com.verza.data.db.SongEntity
import com.verza.innertube.models.MusicItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
) {
    fun all(): Flow<List<PlaylistWithCover>> = playlistDao.all()

    fun tracksOf(playlistId: Long): Flow<List<SongEntity>> = playlistDao.tracksOf(playlistId)

    suspend fun nameOf(id: Long): String? = playlistDao.nameOf(id)

    suspend fun create(name: String): Long =
        playlistDao.create(com.verza.data.db.PlaylistEntity(name = name.trim().ifBlank { "Untitled" }))

    suspend fun rename(id: Long, name: String) =
        playlistDao.rename(id, name.trim().ifBlank { "Untitled" })

    suspend fun delete(id: Long) = playlistDao.delete(id)

    /**
     * Adds a track to [playlistId]. We mirror the track into `songs` first so the join table never
     * dangles — and so the row can render even if the song was never separately liked/played.
     */
    suspend fun addTrack(playlistId: Long, item: MusicItem) {
        val existing = songDao.get(item.id)
        songDao.upsert(
            (existing ?: SongEntity(
                id = item.id,
                title = item.title,
                artist = item.artist,
                thumbnailUrl = item.thumbnailUrl,
                durationMs = item.durationMs,
            )).copy(
                // Refresh title/artist/thumbnail metadata in case it's improved since last cached.
                title = item.title,
                artist = item.artist,
                thumbnailUrl = item.thumbnailUrl ?: existing?.thumbnailUrl,
                durationMs = if (item.durationMs > 0) item.durationMs else existing?.durationMs ?: 0,
            )
        )
        playlistDao.addTrack(playlistId, item.id)
    }

    suspend fun removeTrack(playlistId: Long, songId: String) =
        playlistDao.removeTrack(playlistId, songId)
}
