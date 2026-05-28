package com.verza.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert
    suspend fun create(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /**
     * All playlists, each with the first track's thumbnail (if any) and a track count, so the
     * Library can render rich playlist cards in one query.
     */
    @Query(
        """
        SELECT p.id, p.name, p.createdAt,
               (SELECT s.thumbnailUrl FROM playlist_tracks pt
                 JOIN songs s ON s.id = pt.songId
                 WHERE pt.playlistId = p.id
                 ORDER BY pt.position ASC LIMIT 1) AS coverUrl,
               (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount
          FROM playlists p
         ORDER BY p.createdAt DESC
        """
    )
    fun all(): Flow<List<PlaylistWithCover>>

    @Query(
        """
        SELECT s.* FROM playlist_tracks pt
          JOIN songs s ON s.id = pt.songId
         WHERE pt.playlistId = :playlistId
         ORDER BY pt.position ASC
        """
    )
    fun tracksOf(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT name FROM playlists WHERE id = :id")
    suspend fun nameOf(id: Long): String?

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackEntry(entry: PlaylistTrackEntity)

    @Transaction
    suspend fun addTrack(playlistId: Long, songId: String) {
        addTrackEntry(
            PlaylistTrackEntity(
                playlistId = playlistId,
                songId = songId,
                position = nextPosition(playlistId),
            )
        )
    }

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeTrack(playlistId: Long, songId: String)
}
