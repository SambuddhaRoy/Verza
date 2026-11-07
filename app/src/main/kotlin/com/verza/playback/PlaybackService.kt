package com.verza.playback

import android.app.PendingIntent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.verza.MainActivity
import com.verza.R
import com.verza.data.Song
import com.verza.data.toSaved
import com.verza.graph
import com.verza.widget.NowPlayingWidgetUpdater
import com.verza.widget.WidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * The player, living in the service so it plays on with no screen open. It also records what
 * plays: Recent, the listening time Your Sound counts, and the queue to reopen with.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService() {

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession
    private lateinit var resolver: Resolver
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // How long the current song has actually played, for Your Sound.
    private var listening: String? = null
    private var listenedMs = 0L
    private var playingSince = 0L
    private var failuresInARow = 0

    override fun onCreate() {
        super.onCreate()
        val graph = graph
        resolver = Resolver(graph)
        val upstream = DefaultDataSource.Factory(this, OkHttpDataSource.Factory(graph.http))
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(ResolvingDataSource.Factory(upstream, resolver)))
            .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA).build(), true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(Events())

        setMediaNotificationProvider(DefaultMediaNotificationProvider.Builder(this).build().apply { setSmallIcon(R.drawable.ic_notification) })
        val open = PendingIntent.getActivity(this, 0, android.content.Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        session = MediaLibrarySession.Builder(this, player, Callback())
            .setSessionActivity(open)
            // Covers for the notification and lock screen through the app's own client.
            .setBitmapLoader(DataSourceBitmapLoader(MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()), OkHttpDataSource.Factory(graph.http)))
            .build()

        scope.launch { graph.settings.skipSilence.flow.collect { player.skipSilenceEnabled = it } }
        scope.launch { graph.sourceList.collect { resolver.forgetSources() } }
        scope.launch { fade() }
        // A timer for the end of the song: the player pauses there itself.
        scope.launch { graph.player.sleep.collect { player.pauseAtEndOfMediaItems = it != null && it.atMs == null } }
        // The listener's sound, on whatever audio session the player has now.
        graph.effects.bind(player.audioSessionId)
        scope.launch {
            val s = graph.settings
            kotlinx.coroutines.flow.combine(s.eqOn.flow, s.eqBands.flow, s.bass.flow, s.loudness.flow) { on, bands, bass, loud -> EqConfig(on, bands, bass, loud) }
                .collect { graph.effects.apply(it) }
        }
        scope.launch { while (true) { delay(20_000); saveQueue() } }
    }

    private inner class Events : Player.Listener {
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            countListen()
            listening = item?.mediaId
            item?.let { scope.launch(Dispatchers.IO) { graph.library.played(it.toSong()) } }
            lookAhead()
            saveQueue()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_MEDIA_METADATA_CHANGED)) widgets()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) = graph.effects.bind(audioSessionId)

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) graph.player.sleep.value = null
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) { playingSince = SystemClock.elapsedRealtime(); failuresInARow = 0 } else countListen()
        }

        // A new queue, or shuffle turned on, changes which songs come next.
        override fun onTimelineChanged(timeline: Timeline, reason: Int) = lookAhead()
        override fun onShuffleModeEnabledChanged(enabled: Boolean) = lookAhead()

        override fun onPlayerError(error: PlaybackException) {
            val id = player.currentMediaItem?.mediaId ?: return
            // A source's file that won't play: the same song from YouTube, from the same spot.
            if (resolver.failed(id)) { player.prepare(); return }
            // Offline, a failure pauses: skipping would walk the whole queue away while there's no
            // network, and whatever loaded when it came back would start by itself.
            if (!online()) return
            Toast.makeText(this@PlaybackService, "Couldn't play this song", Toast.LENGTH_SHORT).show()
            if (++failuresInARow < 3 && player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.prepare(); player.play() }
        }
    }

    /** Adds the time the current song has played since it last started. */
    private fun countListen() {
        val id = listening ?: return
        if (playingSince > 0) listenedMs += SystemClock.elapsedRealtime() - playingSince
        playingSince = if (player.isPlaying) SystemClock.elapsedRealtime() else 0
        if (player.currentMediaItem?.mediaId != id) {
            val ms = listenedMs
            listenedMs = 0
            scope.launch(Dispatchers.IO) { graph.library.listened(id, ms) }
        }
    }

    /** The next songs in the order they'll play, looked up while this one plays. */
    private fun lookAhead() {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return
        val repeat = if (player.repeatMode == Player.REPEAT_MODE_ONE) Player.REPEAT_MODE_OFF else player.repeatMode
        var index = player.currentMediaItemIndex
        val next = ArrayList<android.net.Uri>()
        while (next.size < LOOK_AHEAD) {
            index = timeline.getNextWindowIndex(index, repeat, player.shuffleModeEnabled)
            // The end of the queue; or, repeating a short one, back round to where it started.
            if (index == C.INDEX_UNSET || index == player.currentMediaItemIndex) break
            player.getMediaItemAt(index).localConfiguration?.uri?.let(next::add) ?: break
        }
        resolver.lookAhead(next)
    }

    /** The home-screen widgets show what plays; they can't watch the player themselves. */
    private fun widgets() {
        val m = player.mediaMetadata
        NowPlayingWidgetUpdater.publish(this, scope, WidgetState(m.title?.toString().orEmpty(), m.artist?.toString().orEmpty(), m.artworkUri?.toString(), player.isPlaying))
    }

    private fun saveQueue() {
        if (player.mediaItemCount == 0) return
        val songs = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).toSong() }
        val saved = com.verza.data.SavedQueue(songs.map { it.toSaved() }, player.currentMediaItemIndex, player.currentPosition)
        scope.launch(Dispatchers.IO) { graph.settings.savedQueue.set(saved) }
    }

    /**
     * The one hand on the volume. Fades into and out of each song, over the listener's setting: not
     * an overlap of the two songs, which would need a second player behind the session, but it takes
     * the hard cut away. Fading in is capped at two seconds: a twelve-second ramp at the start of a
     * song is a fault. And the sleep timer, falling to silence and pausing.
     */
    private suspend fun fade() {
        var window = 0L
        scope.launch { graph.settings.fadeSeconds.flow.collect { window = it * 1000L } }
        var current: String? = null
        var enteredAt = 0L
        while (true) {
            delay(120)
            val now = SystemClock.elapsedRealtime()
            val id = player.currentMediaItem?.mediaId
            if (id != current) { current = id; enteredAt = now }
            var gain = 1f
            if (window > 0 && id != null && player.isPlaying) {
                val rampIn = minOf(window, 2_000L)
                val inGain = if (now - enteredAt >= rampIn) 1f else (now - enteredAt).toFloat() / rampIn
                val left = if (player.duration > 0) player.duration - player.currentPosition else Long.MAX_VALUE
                gain = minOf(inGain, if (left >= window) 1f else left.toFloat() / window)
            }
            val sleep = graph.player.sleep.value
            if (sleep?.atMs != null) {
                val left = sleep.atMs - now
                if (left <= 0) { player.pause(); graph.player.sleep.value = null }
                else gain = minOf(gain, left.toFloat() / sleep.fadeMs)
            }
            gain = gain.coerceIn(0f, 1f)
            if (player.volume != gain) player.volume = gain
        }
    }

    private fun online(): Boolean {
        val cm = getSystemService(ConnectivityManager::class.java)
        return cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
    }

    override fun onGetSession(controller: MediaSession.ControllerInfo): MediaLibrarySession = session

    override fun onDestroy() {
        saveQueue()
        scope.cancel()
        resolver.close()
        graph.effects.bind(0)
        session.release()
        player.release()
        super.onDestroy()
    }

    private inner class Callback : MediaLibrarySession.Callback {
        /**
         * Items lose their URI crossing from a controller to the session, so it's made again from the
         * id here. Only this app may name a file on the phone: for anyone else an id is a YouTube id,
         * or an outside app could have the player open a file in this app's own storage.
         */
        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            items: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val ours = controller.packageName == packageName
            return Futures.immediateFuture(items.map { item ->
                val id = item.mediaId
                val uri = if (ours && (id.startsWith("content://") || id.startsWith("file://"))) android.net.Uri.parse(id)
                else item.mediaMetadata.let { placeholder(id.takeIf(::isVideoId) ?: "invalid", it.title.toString(), it.artist.toString(), it.durationMs ?: 0) }
                item.buildUpon().setUri(uri).build()
            }.toMutableList())
        }

        /**
         * Play pressed with nothing queued, from a widget, headphones or the system's media card after
         * the app was closed: the queue saved last time, where it stopped.
         */
        private var lastFolder: String? = null

        /** A folder's songs: YouTube ones only, since an id from outside can't name a file here. */
        private suspend fun songsIn(id: String): List<Song> {
            val library = graph.library
            val songs = when {
                id == "recent" -> library.recent.first()
                id == "liked" -> library.liked.first()
                id == "downloaded" -> library.downloaded.first()
                id.startsWith("pl:") -> id.removePrefix("pl:").toLongOrNull()?.let { library.playlist(it).first().songs }.orEmpty()
                else -> emptyList()
            }
            lastFolder = id
            return songs.filter { !it.onPhone }
        }

        private fun folder(id: String, title: String) = MediaItem.Builder().setMediaId(id)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setIsBrowsable(true).setIsPlayable(false).setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).build())
            .build()

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            val saved = graph.settings.savedQueue.get()
            check(saved.tracks.isNotEmpty()) { "Nothing to resume" }
            MediaSession.MediaItemsWithStartPosition(saved.tracks.map { item(it.toSong()) }, saved.index.coerceIn(0, saved.tracks.lastIndex), saved.positionMs)
        }

        // What a car (Android Auto) browses: the library, as folders of songs.

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            // The system's media card asks for "recent" to offer picking up where you left off.
            Futures.immediateFuture(LibraryResult.ofItem(if (params?.isRecent == true) folder("last", "Verza") else folder("root", "Verza"), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            val library = graph.library
            val items = when {
                parentId == "root" -> listOf(folder("recent", "Recently played"), folder("liked", "Liked songs"), folder("downloaded", "Downloaded"), folder("playlists", "Playlists"))
                parentId == "playlists" -> library.playlists.first().map { folder("pl:${it.playlist.id}", it.playlist.name) }
                // The song the saved queue stopped on, for the system's media card.
                parentId == "last" -> graph.settings.savedQueue.get().let { q -> listOfNotNull(q.tracks.getOrNull(q.index)?.toSong()?.let(::item)) }
                else -> songsIn(parentId).map(::item)
            }
            LibraryResult.ofItemList(ImmutableList.copyOf(items), params)
        }

        /**
         * A song chosen in a folder plays with the rest of that folder after it, rather than alone.
         * Only one song arrives from the car, so its folder is found again by id.
         */
        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val one = mediaItems.singleOrNull()?.mediaId
            val folder = lastFolder
            if (one == null || folder == null || controller.packageName == packageName) return super.onSetMediaItems(mediaSession, controller, mediaItems, startIndex, startPositionMs)
            return scope.future {
                val songs = songsIn(folder)
                val at = songs.indexOfFirst { it.id == one }
                if (at < 0) MediaSession.MediaItemsWithStartPosition(onAddMediaItems(mediaSession, controller, mediaItems).await(), startIndex, startPositionMs)
                else MediaSession.MediaItemsWithStartPosition(songs.map(::item), at, 0)
            }
        }
    }

    private companion object {
        const val LOOK_AHEAD = 3
        fun isVideoId(id: String) = id.length in 1..64 && id.all { it.isLetterOrDigit() || it == '_' || it == '-' }
    }
}

/** A queued item back as a song. */
fun MediaItem.toSong() = Song(
    id = mediaId,
    title = mediaMetadata.title?.toString().orEmpty(),
    artist = mediaMetadata.artist?.toString().orEmpty(),
    album = mediaMetadata.albumTitle?.toString(),
    art = mediaMetadata.artworkUri?.toString(),
    durationMs = mediaMetadata.durationMs ?: 0,
)
