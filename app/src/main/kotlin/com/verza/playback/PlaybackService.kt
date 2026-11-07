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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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

    private fun saveQueue() {
        if (player.mediaItemCount == 0) return
        val songs = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).toSong() }
        val saved = com.verza.data.SavedQueue(songs.map { it.toSaved() }, player.currentMediaItemIndex, player.currentPosition)
        scope.launch(Dispatchers.IO) { graph.settings.savedQueue.set(saved) }
    }

    /**
     * Fades the volume into and out of each song, over the listener's setting. Not an overlap of the
     * two songs, which would need a second player behind the session; it takes the hard cut away.
     * Fading in is capped at two seconds: a twelve-second ramp at the start of a song is a fault.
     */
    private suspend fun fade() {
        var window = 0L
        scope.launch { graph.settings.fadeSeconds.flow.collect { window = it * 1000L } }
        var current: String? = null
        var enteredAt = 0L
        while (true) {
            delay(120)
            if (window == 0L) { if (player.volume != 1f) player.volume = 1f; continue }
            val id = player.currentMediaItem?.mediaId
            if (id != current) { current = id; enteredAt = SystemClock.elapsedRealtime() }
            if (id == null || !player.isPlaying) continue
            val rampIn = minOf(window, 2_000L)
            val sinceStart = SystemClock.elapsedRealtime() - enteredAt
            val inGain = if (sinceStart >= rampIn) 1f else sinceStart.toFloat() / rampIn
            val left = if (player.duration > 0) player.duration - player.currentPosition else Long.MAX_VALUE
            val outGain = if (left >= window) 1f else left.toFloat() / window
            player.volume = minOf(inGain, outGain).coerceIn(0f, 1f)
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

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
            LibraryResult.ofItem(
                MediaItem.Builder().setMediaId("root")
                    .setMediaMetadata(MediaMetadata.Builder().setIsBrowsable(true).setIsPlayable(false).build()).build(),
                params,
            ),
        )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
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
