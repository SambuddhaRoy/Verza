package com.lstn.player

/**
 * Abstraction the [MusicService] uses to ask "do we have this track on disk?" without depending
 * on the app's Room layer (which would create a circular module dependency). Implemented in :app
 * against [com.lstn.data.db.SongDao] and bound via Hilt.
 */
interface DownloadLookup {
    /** Absolute path to a locally-downloaded copy of [videoId], or null if not downloaded. */
    suspend fun pathFor(videoId: String): String?
}
