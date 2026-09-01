package com.verza.playback

import com.verza.data.LibraryRepository
import com.verza.data.db.SongEntity
import com.verza.di.ApplicationScope
import com.verza.player.MediaBrowseTree
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes the library into [MediaBrowseTree] so Android Auto and Assistant have something to
 * browse.
 *
 * Runs at process scope, because a car connects to the media session and never to an Activity. The
 * app process is the service's process, so as long as anything is playing this collector is alive;
 * on a cold connect the service starts the process and this runs with it.
 *
 * Three folders, not the whole library. A browser is a list you read at a glance while doing
 * something else, and a thousand-row folder is unusable in a car whatever the database can manage.
 */
@Singleton
class BrowseTreePublisher @Inject constructor(
    private val library: LibraryRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {

    fun start() {
        scope.launch {
            combine(
                library.recentlyPlayed(),
                library.liked(),
                library.downloaded(),
            ) { recent, liked, downloaded ->
                listOf(
                    folder("verza_recent", "Recently played", recent),
                    folder("verza_liked", "Liked songs", liked),
                    folder("verza_downloaded", "Downloaded", downloaded),
                ).filter { it.children.isNotEmpty() }
            }.collect { MediaBrowseTree.publish(it) }
        }
    }

    private fun folder(id: String, title: String, songs: List<SongEntity>) = MediaBrowseTree.Node(
        id = id,
        title = title,
        subtitle = "${songs.size} tracks",
        // The first cover stands in for the folder, so the list is not three grey squares.
        artworkUri = songs.firstOrNull()?.thumbnailUrl,
        browsable = true,
        children = songs.take(MAX_PER_FOLDER).map { song ->
            MediaBrowseTree.Node(
                // The download path when there is one, so a car plays the local file rather than
                // re-resolving a stream over whatever signal a moving vehicle has.
                id = song.downloadPath ?: song.id,
                title = song.title,
                subtitle = song.artist,
                artworkUri = song.thumbnailUrl,
                browsable = false,
            )
        },
    )

    private companion object {
        const val MAX_PER_FOLDER = 100
    }
}
