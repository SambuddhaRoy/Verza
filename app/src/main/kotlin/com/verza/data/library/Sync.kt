package com.verza.data.library

import com.verza.data.Account
import com.verza.data.youtube.YouTube
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Sends the library's changes to the YouTube Music account: likes, and playlists made, renamed,
 * deleted, and songs added to or taken out of them. Each change was written down when it was made (`sync_ops`), so one made offline arrives later.
 * Oldest first, since they're things the listener did in order. A change that keeps failing is
 * dropped after six tries rather than retried on every launch for ever.
 */
class Sync(db: LibraryDb, private val youtube: YouTube, private val account: Account) {
    private val ops = db.sync()
    private val lists = db.playlists()
    private val running = Mutex()

    suspend fun flush() {
        if (!account.signedIn || running.isLocked) return
        running.withLock {
            for (op in ops.pending()) {
                val done = runCatching {
                    when (op.op) {
                        "like" -> { youtube.setLiked(op.videoId, true); true }
                        "unlike" -> { youtube.setLiked(op.videoId, false); true }
                        "create_playlist" -> youtube.createPlaylist(op.title)?.also { lists.setRemoteId(op.playlistId, it); ops.attachRemoteId(op.playlistId, it) } != null
                        "add_to_playlist" -> op.remoteId.isNotEmpty() && youtube.addToPlaylist(op.remoteId, op.videoId)
                        "remove_from_playlist" -> op.remoteId.isNotEmpty() && youtube.removeFromPlaylist(op.remoteId, op.videoId)
                        "rename_playlist" -> op.remoteId.isNotEmpty() && youtube.renamePlaylist(op.remoteId, op.title)
                        "delete_playlist" -> youtube.deletePlaylist(op.remoteId)
                        else -> true // an operation this version doesn't know: skipped, not retried
                    }
                }.getOrDefault(false)
                if (done) ops.remove(op.id) else ops.failed(op.id)
            }
            ops.dropExhausted(6)
        }
    }
}
