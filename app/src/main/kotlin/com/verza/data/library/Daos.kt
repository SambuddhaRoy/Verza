package com.verza.data.library

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 100")
    fun recent(): Flow<List<SongRow>>

    @Query("SELECT * FROM songs WHERE liked = 1 ORDER BY likedAt DESC")
    fun liked(): Flow<List<SongRow>>

    @Query("SELECT * FROM songs WHERE downloadPath IS NOT NULL ORDER BY title COLLATE NOCASE")
    fun downloaded(): Flow<List<SongRow>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun get(id: String): SongRow?

    @Query("SELECT id FROM songs WHERE liked = 1")
    fun likedIds(): Flow<List<String>>

    @Query("SELECT * FROM songs WHERE downloadPath IS NOT NULL")
    suspend fun downloadRows(): List<SongRow>

    @Query("SELECT downloadPath FROM songs WHERE id = :id")
    suspend fun downloadPath(id: String): String?

    @Query("SELECT id FROM songs WHERE downloadPath IS NOT NULL")
    fun downloadedIds(): Flow<List<String>>

    @Query("UPDATE songs SET downloadPath = NULL WHERE id = :id")
    suspend fun forgetDownload(id: String)

    @Upsert
    suspend fun put(song: SongRow)

    @Query("SELECT * FROM songs")
    suspend fun all(): List<SongRow>
}

/** A playlist with what its row needs: the first song's cover and how many songs. */
data class PlaylistSummary(
    @Embedded val playlist: PlaylistRow,
    val cover: String?,
    val count: Int,
)

@Dao
interface PlaylistDao {
    @Query(
        """SELECT p.*, (SELECT s.thumbnailUrl FROM playlist_tracks t JOIN songs s ON s.id = t.songId
            WHERE t.playlistId = p.id ORDER BY t.position LIMIT 1) AS cover,
           (SELECT COUNT(*) FROM playlist_tracks t WHERE t.playlistId = p.id) AS count
           FROM playlists p ORDER BY p.createdAt DESC""",
    )
    fun all(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun get(id: Long): PlaylistRow?

    @Query("SELECT name FROM playlists WHERE id = :id")
    fun name(id: Long): Flow<String?>

    @Query("SELECT * FROM playlists ORDER BY createdAt")
    suspend fun allOnce(): List<PlaylistRow>

    @Query("SELECT songId FROM playlist_tracks WHERE playlistId = :id ORDER BY position")
    suspend fun songIds(id: Long): List<String>

    @Query("SELECT id FROM playlists WHERE name = :name LIMIT 1")
    suspend fun idByName(name: String): Long?

    @Query("UPDATE playlists SET remoteId = :remoteId WHERE id = :id")
    suspend fun setRemoteId(id: Long, remoteId: String)

    @Query("SELECT s.* FROM playlist_tracks t JOIN songs s ON s.id = t.songId WHERE t.playlistId = :id ORDER BY t.position")
    fun songs(id: Long): Flow<List<SongRow>>

    @Insert
    suspend fun create(playlist: PlaylistRow): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun add(track: PlaylistTrackRow)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM playlist_tracks WHERE playlistId = :id")
    suspend fun nextPosition(id: Long): Int

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id AND songId = :songId")
    suspend fun remove(id: Long, songId: String)

    @Query("UPDATE playlists SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    // Its songs by hand: the foreign key's cascade only runs where SQLite enforces foreign keys
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id")
    suspend fun clear(id: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PlayDao {
    @Insert
    suspend fun add(play: PlayRow)
}

@Dao
interface SyncDao {
    @Insert
    suspend fun add(op: SyncRow)

    @Query("SELECT * FROM sync_ops ORDER BY createdAt, id LIMIT 200")
    suspend fun pending(): List<SyncRow>

    @Query("DELETE FROM sync_ops WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("UPDATE sync_ops SET attempts = attempts + 1 WHERE id = :id")
    suspend fun failed(id: Long)

    @Query("DELETE FROM sync_ops WHERE attempts >= :max")
    suspend fun dropExhausted(max: Int)

    /** Whatever was still waiting to be sent for a playlist that's been deleted. */
    @Query("DELETE FROM sync_ops WHERE playlistId = :playlistId")
    suspend fun dropFor(playlistId: Long)

    /** Once a playlist exists on the account, every song queued for it can be sent. */
    @Query("UPDATE sync_ops SET remoteId = :remoteId WHERE playlistId = :playlistId")
    suspend fun attachRemoteId(playlistId: Long, remoteId: String)
}
