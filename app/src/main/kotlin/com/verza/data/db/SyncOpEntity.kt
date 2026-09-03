package com.verza.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * One change waiting to be sent to YouTube Music.
 *
 * Likes were already pushed to the account, but as fire and forget inside a runCatching: a like
 * made on the underground was swallowed and never sent, and Verza on the desktop simply never saw
 * it. Anything that claims to sync has to survive being offline, so intent is written down first
 * and delivered afterwards.
 *
 * Deliberately a log of intents rather than a diff of state. Working out what changed by comparing
 * two libraries needs a shared idea of which side is newer, which neither end has; recording "the
 * user liked this at 9:04" needs nothing.
 */
@Entity(tableName = "sync_ops")
data class SyncOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** One of [SyncOp]. Stored as a string so an unknown future op is skipped, not a crash. */
    val op: String,
    val videoId: String = "",
    /** The local playlist this belongs to, for playlist operations. */
    val playlistId: Long = 0,
    /** The playlist's YouTube id, once it has one. */
    val remoteId: String = "",
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** Bumped on each failure, so a permanently broken op stops being retried forever. */
    val attempts: Int = 0,
)

/** The operations the queue knows how to deliver. */
object SyncOp {
    const val LIKE = "like"
    const val UNLIKE = "unlike"
    const val CREATE_PLAYLIST = "create_playlist"
    const val ADD_TO_PLAYLIST = "add_to_playlist"

    /** After this many failures an op is dropped rather than retried on every launch forever. */
    const val MAX_ATTEMPTS = 6
}

@Dao
interface SyncOpDao {

    @Insert
    suspend fun add(op: SyncOpEntity): Long

    /** Oldest first, because these are intents in time and replaying them out of order is wrong. */
    @Query("SELECT * FROM sync_ops ORDER BY createdAt ASC, id ASC LIMIT 200")
    suspend fun pending(): List<SyncOpEntity>

    @Query("DELETE FROM sync_ops WHERE id = :id")
    suspend fun remove(id: Long)

    @Query("UPDATE sync_ops SET attempts = attempts + 1 WHERE id = :id")
    suspend fun recordFailure(id: Long)

    @Query("DELETE FROM sync_ops WHERE attempts >= :max")
    suspend fun dropExhausted(max: Int)

    /**
     * A like followed by an unlike of the same track cancels out. Without this a session of
     * tapping the heart on and off arrives at the account as a dozen round trips ending where it
     * started.
     */
    @Query("DELETE FROM sync_ops WHERE videoId = :videoId AND op IN ('like', 'unlike')")
    suspend fun clearLikesFor(videoId: String)

    /** Once a playlist has a remote id, every queued track for it can be addressed. */
    @Query("UPDATE sync_ops SET remoteId = :remoteId WHERE playlistId = :playlistId")
    suspend fun attachRemoteId(playlistId: Long, remoteId: String)

    @Query("SELECT COUNT(*) FROM sync_ops")
    suspend fun count(): Int
}
