package com.verza.data

import com.verza.data.db.PlaylistDao
import com.verza.data.db.SyncOp
import com.verza.data.db.SyncOpDao
import com.verza.data.db.SyncOpEntity
import com.verza.di.ApplicationScope
import com.verza.innertube.InnerTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends likes and playlists to the signed-in YouTube Music account, so the same library shows up in
 * Verza on the desktop.
 *
 * Changes are written to a queue first and delivered afterwards. Likes used to be pushed inline
 * inside a runCatching, which meant a like made with no signal was swallowed and never sent: the
 * app looked like it had synced and had not. Recording the intent first makes the difference between
 * "we tried once" and "this will arrive".
 *
 * Ordering is preserved because these are intents in time. Creating a playlist and adding three
 * tracks to it only works in that order, and a like followed by an unlike has to end unliked.
 */
@Singleton
class YouTubeSync @Inject constructor(
    private val ops: SyncOpDao,
    private val playlists: PlaylistDao,
    private val prefs: PreferencesRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    // One drain at a time. Two concurrent passes would deliver the same op twice, and for
    // ADD_TO_PLAYLIST that means a duplicate track on the account.
    private val draining = Mutex()

    fun start() {
        scope.launch {
            ops.dropExhausted(SyncOp.MAX_ATTEMPTS)
            drain()
        }
    }

    /** Queue a like or an unlike, replacing anything already pending for the same track. */
    fun like(videoId: String, liked: Boolean) = scope.launch {
        // The last word wins. Tapping the heart on and off five times should reach the account as
        // one change, not ten round trips ending where they started.
        ops.clearLikesFor(videoId)
        ops.add(SyncOpEntity(op = if (liked) SyncOp.LIKE else SyncOp.UNLIKE, videoId = videoId))
        drain()
    }

    /** Queue the creation of [name] on the account for the local playlist [playlistId]. */
    fun createPlaylist(playlistId: Long, name: String) = scope.launch {
        ops.add(SyncOpEntity(op = SyncOp.CREATE_PLAYLIST, playlistId = playlistId, title = name))
        drain()
    }

    /** Queue a track for a playlist. Safe before the playlist exists remotely; ordering handles it. */
    fun addToPlaylist(playlistId: Long, videoId: String) = scope.launch {
        // Local files have no YouTube id and nothing to add.
        if (videoId.startsWith("content://") || videoId.startsWith("file://")) return@launch
        ops.add(SyncOpEntity(op = SyncOp.ADD_TO_PLAYLIST, playlistId = playlistId, videoId = videoId))
        drain()
    }

    /**
     * Deliver whatever is pending.
     *
     * Stops at the first failure rather than skipping past it, because the queue is ordered and
     * carrying on would apply later changes on top of an earlier one that never landed. The
     * remainder waits for the next attempt.
     */
    suspend fun drain() = draining.withLock {
        if (prefs.cookieFlow.first().isNullOrBlank()) return@withLock
        for (op in ops.pending()) {
            val delivered = withContext(Dispatchers.IO) { runCatching { deliver(op) }.getOrDefault(false) }
            if (delivered) {
                ops.remove(op.id)
            } else {
                ops.recordFailure(op.id)
                ops.dropExhausted(SyncOp.MAX_ATTEMPTS)
                return@withLock
            }
        }
    }

    private suspend fun deliver(op: SyncOpEntity): Boolean = when (op.op) {
        SyncOp.LIKE -> { InnerTube.setLikeStatus(op.videoId, true); true }
        SyncOp.UNLIKE -> { InnerTube.setLikeStatus(op.videoId, false); true }

        SyncOp.CREATE_PLAYLIST -> {
            val remote = InnerTube.createPlaylist(op.title)
            if (remote.isNullOrBlank()) {
                false
            } else {
                playlists.setRemoteId(op.playlistId, remote)
                // Every track already queued for this playlist can now be addressed.
                ops.attachRemoteId(op.playlistId, remote)
                true
            }
        }

        SyncOp.ADD_TO_PLAYLIST -> {
            val remote = op.remoteId.ifBlank { playlists.remoteIdOf(op.playlistId).orEmpty() }
            // No remote playlist yet means the create is still ahead of this in the queue, which is
            // a reason to wait rather than a reason to fail permanently.
            if (remote.isBlank()) false else InnerTube.addToPlaylist(remote, op.videoId)
        }

        // An op written by a newer version. Drop it rather than blocking everything behind it.
        else -> true
    }
}
