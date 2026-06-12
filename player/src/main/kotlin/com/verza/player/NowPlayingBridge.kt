package com.verza.player

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide bridge for the media-notification "Like" control. The media session + system
 * notification live in :player, but the liked-songs store (Room) lives in :app and :player can't
 * depend on :app — so, exactly like [PlayerSettings] and [AudioSessionRegistry]:
 *
 *  - :app publishes the current set of liked song ids here ([publishLikedIds]); MusicService reads
 *    [likedIds] to draw the heart filled or outline for whatever is currently playing.
 *  - When the user taps the heart in the notification / lock screen / always-on display, the
 *    service posts a [LikeRequest] ([requestLikeToggle]); :app collects [likeToggleRequests] and
 *    persists the toggle (and mirrors it to the signed-in account).
 *
 * Both run at process scope, so the heart works even when no Activity/ViewModel is alive — the
 * foreground service keeps the process (and the app-side collector) running.
 */
object NowPlayingBridge {

    /** Enough metadata to upsert + like a song the app may not have seen yet. */
    data class LikeRequest(
        val mediaId: String,
        val title: String,
        val artist: String,
        val artworkUri: String?,
    )

    private val _likedIds = MutableStateFlow<Set<String>>(emptySet())
    val likedIds: StateFlow<Set<String>> = _likedIds.asStateFlow()

    fun publishLikedIds(ids: Set<String>) {
        _likedIds.value = ids
    }

    // replay 0, small buffer so a tap is never dropped even if the collector is momentarily busy.
    private val _likeToggleRequests = MutableSharedFlow<LikeRequest>(extraBufferCapacity = 8)
    val likeToggleRequests: SharedFlow<LikeRequest> = _likeToggleRequests.asSharedFlow()

    fun requestLikeToggle(request: LikeRequest) {
        _likeToggleRequests.tryEmit(request)
    }
}
