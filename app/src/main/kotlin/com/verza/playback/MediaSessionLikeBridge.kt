package com.verza.playback

import com.verza.data.LibraryRepository
import com.verza.data.db.SongEntity
import com.verza.di.ApplicationScope
import com.verza.player.NowPlayingBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connects the media-notification "Like" heart (owned by MusicService in :player) to the liked-songs
 * store in :app. Runs at process scope so the heart keeps working when the app has no Activity —
 * the foreground service keeps the process alive, and this collector with it.
 *
 *  - Publishes the current set of liked ids to [NowPlayingBridge] so the service draws the heart
 *    filled or outline for the playing track.
 *  - Persists taps coming back from the notification (and mirrors them to the signed-in account via
 *    [LibraryRepository.toggleLike]).
 *
 * Started once from [com.verza.VerzaApp.onCreate].
 */
@Singleton
class MediaSessionLikeBridge @Inject constructor(
    private val libraryRepository: LibraryRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            libraryRepository.likedIds().collect { ids ->
                NowPlayingBridge.publishLikedIds(ids.toSet())
            }
        }
        scope.launch {
            NowPlayingBridge.likeToggleRequests.collect { req ->
                libraryRepository.toggleLike(
                    SongEntity(
                        id = req.mediaId,
                        title = req.title.ifBlank { "Unknown" },
                        artist = req.artist,
                        thumbnailUrl = req.artworkUri,
                        // Real duration / lastPlayedAt are preserved by toggleLike when the row
                        // already exists; these defaults only apply to a never-seen track.
                        durationMs = 0L,
                        lastPlayedAt = System.currentTimeMillis(),
                    )
                )
            }
        }
    }
}
