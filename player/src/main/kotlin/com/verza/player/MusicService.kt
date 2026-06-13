package com.verza.player

import android.app.PendingIntent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.Executors
import com.verza.innertube.InnerTube
import com.verza.player.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

private const val INNERTUBE_SCHEME = "innertube://"
// Custom session command behind the notification / lock-screen / always-on "Like" heart.
private const val ACTION_TOGGLE_LIKE = "com.verza.player.TOGGLE_LIKE"

@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var downloadLookup: DownloadLookup

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession

    // Loads cover bitmaps (via the app's OkHttp client) for the notification AND for the embedded
    // artwork below.
    private lateinit var artworkBitmapLoader: DataSourceBitmapLoader
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    // Service-lifetime scope for observing app-pushed playback options (see PlayerSettings).
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Short-TTL cache of resolved stream URLs, keyed by videoId — so seeks / re-opens don't re-run
    // the expensive NewPipe extraction. Concurrent because ExoPlayer resolves on loader threads.
    private data class CachedStreamUrl(val url: String, val resolvedAt: Long)
    private val streamUrlCache = java.util.concurrent.ConcurrentHashMap<String, CachedStreamUrl>()
    private val STREAM_URL_TTL_MS = 5 * 60 * 60 * 1000L // 5h; googlevideo URLs last ~6h

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Base HTTP source for the actual googlevideo stream bytes.
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)

        // Intercept our placeholder innertube://<videoId> URIs and swap in a freshly
        // resolved stream URL right before ExoPlayer opens the connection. Resolution
        // runs on ExoPlayer's loader thread (never the main thread), so blocking is fine.
        //
        // On failure the user sees a short generic toast and the player surfaces an error state.
        // Detailed diagnostics (videoId, stream URL, resolver breakdown) are logged ONLY in debug
        // builds — logcat is readable over ADB / by privileged apps, so we don't leak what the
        // user is playing in release.
        val mainHandler = Handler(Looper.getMainLooper())
        val resolver = ResolvingDataSource.Resolver { dataSpec ->
            val raw = dataSpec.uri.toString()
            // Only innertube:// placeholders need resolving. Local file:// / content:// URIs
            // (on-device music, downloaded files) pass straight through to the upstream
            // DefaultDataSource, which opens them natively.
            if (!raw.startsWith(INNERTUBE_SCHEME)) return@Resolver dataSpec
            val videoId = raw.removePrefix(INNERTUBE_SCHEME)
            if (BuildConfig.DEBUG) Log.i("VerzaPlayback", "Resolving $videoId …")

            try {
                // Prefer a downloaded copy if one exists — instant start, works offline.
                val cached = runBlocking { downloadLookup.pathFor(videoId) }
                if (!cached.isNullOrBlank()) {
                    val file = File(cached)
                    if (file.exists()) {
                        if (BuildConfig.DEBUG) Log.i("VerzaPlayback", "Using cached file: ${file.absolutePath}")
                        return@Resolver dataSpec.withUri(Uri.fromFile(file))
                    }
                }

                // Re-use a recently-resolved stream URL. ResolvingDataSource calls this resolver on
                // *every* open() — including each seek and re-buffer — so without a cache, seeking
                // re-ran the (multi-second) NewPipe extraction every time, which is why seeks were
                // slow. googlevideo URLs stay valid for hours, so a short-TTL cache makes seeks
                // instant while still re-resolving once the URL would have expired.
                streamUrlCache[videoId]?.let { c ->
                    if (System.currentTimeMillis() - c.resolvedAt < STREAM_URL_TTL_MS) {
                        if (BuildConfig.DEBUG) Log.i("VerzaPlayback", "Using cached stream URL for $videoId")
                        return@Resolver dataSpec.withUri(Uri.parse(c.url))
                    }
                }

                val stream = runBlocking { InnerTube.resolveAudioStream(videoId) }
                if (stream == null) {
                    if (BuildConfig.DEBUG) {
                        Log.e("VerzaPlayback", "No stream for $videoId — ${InnerTube.lastResolveDiagnostic}")
                    }
                    showPlaybackToast(mainHandler, debugDetail = "No stream for $videoId\n${InnerTube.lastResolveDiagnostic}")
                    throw IOException("No playable audio stream for $videoId")
                }
                if (BuildConfig.DEBUG) Log.i("VerzaPlayback", "Resolved $videoId → ${stream.url.take(120)}…")
                streamUrlCache[videoId] = CachedStreamUrl(stream.url, System.currentTimeMillis())
                dataSpec.withUri(Uri.parse(stream.url))
            } catch (t: Throwable) {
                // A load the user skipped away from is cancelled by interrupting this loader thread,
                // so runBlocking throws InterruptedException / CancellationException. That isn't a
                // real playback failure — abort quietly (no toast) and let ExoPlayer move on. Without
                // this, rapidly skipping tracks surfaces a spurious "Couldn't play this track".
                if (isCancellation(t)) {
                    Thread.currentThread().interrupt() // preserve the interrupt for the loader
                    throw IOException("Resolve cancelled for $videoId", t)
                }
                if (BuildConfig.DEBUG) Log.e("VerzaPlayback", "Resolve failed for $videoId", t)
                showPlaybackToast(mainHandler, debugDetail = "Playback failed: ${t.javaClass.simpleName}: ${t.message}")
                throw if (t is IOException) t else IOException("Playback failed for $videoId", t)
            }
        }
        // Wrap the HTTP source in a DefaultDataSource so the player can also open file:// and
        // content:// URIs (downloaded files and on-device local music) — DefaultDataSource picks
        // the right sub-source by scheme and delegates http(s) to OkHttp.
        val upstreamFactory = DefaultDataSource.Factory(this, httpFactory)
        val dataSourceFactory = ResolvingDataSource.Factory(upstreamFactory, resolver)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Audio session id is needed by the visualizer in :app — track changes via Listener and
        // also publish the initial value (ExoPlayer assigns one as soon as the audio sink is set up).
        AudioSessionRegistry.set(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioSessionRegistry.set(audioSessionId)
            }
        })

        // Apply the skip-silence option pushed from the app's settings (PlayerSettings bridge).
        serviceScope.launch {
            PlayerSettings.skipSilence.collect { player.skipSilenceEnabled = it }
        }

        val activityIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE) }

        // Own the media notification explicitly rather than leaning on library defaults: a
        // guaranteed-present monochrome status-bar icon and the standard rich MediaStyle layout.
        // This makes the system reliably treat us as an active media session (lock-screen controls,
        // OnePlus hole-punch popout, always-on display), which the default path didn't always do.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.verza_ic_notification)
            }
        )

        // Load cover art for the notification / lock screen / AOD through the app's own OkHttp
        // client (with its user-agent + redirects) rather than the default HTTP loader, so the
        // remote YouTube / iTunes artwork reliably resolves into the rich now-playing card.
        artworkBitmapLoader = DataSourceBitmapLoader(
            MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()),
            OkHttpDataSource.Factory(okHttpClient),
        )

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .also { builder -> activityIntent?.let { builder.setSessionActivity(it) } }
            .setBitmapLoader(artworkBitmapLoader)
            // The "Like" heart shows on the lock screen, the notification, the OnePlus hole-punch
            // popout and the always-on display — anywhere the system surfaces the media session.
            .setCustomLayout(buildCustomLayout())
            .build()

        // Redraw the heart (filled ↔ outline) when the track changes or the liked set updates —
        // the like store lives in :app and pushes new ids in through NowPlayingBridge.
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshLikeLayout()
                embedArtwork(mediaItem)
            }
        })
        serviceScope.launch {
            NowPlayingBridge.likedIds.collect { refreshLikeLayout() }
        }
    }

    /**
     * Decodes the current track's cover and bakes it into the session metadata as raw bytes, so the
     * lock screen / always-on display / OEM popout get a real bitmap directly — some surfaces never
     * fetch a remote artworkUri on their own, which is why the card showed up bare. Fully defensive:
     * any failure is a silent no-op, and because the replacement keeps the same media id + URI it
     * only updates metadata, so playback never re-buffers or pauses.
     */
    private fun embedArtwork(item: MediaItem?) {
        val md = item?.mediaMetadata ?: return
        val uri = md.artworkUri ?: return
        if (md.artworkData != null) return                       // already embedded for this track
        val index = player.currentMediaItemIndex
        val future = runCatching { artworkBitmapLoader.loadBitmap(uri) }.getOrNull() ?: return
        future.addListener({
            runCatching {
                val raw = future.get() ?: return@runCatching
                // Downscale + JPEG so the bytes stay well under the Binder transaction limit when
                // the metadata is handed across to the system media surfaces.
                val maxDim = 640
                val scale = minOf(1f, maxDim.toFloat() / maxOf(raw.width, raw.height))
                val bmp = if (scale < 1f)
                    android.graphics.Bitmap.createScaledBitmap(
                        raw, (raw.width * scale).toInt().coerceAtLeast(1), (raw.height * scale).toInt().coerceAtLeast(1), true,
                    )
                else raw
                val stream = java.io.ByteArrayOutputStream()
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, stream)
                val bytes = stream.toByteArray()
                if (bytes.size > 400_000) return@runCatching        // safety valve
                // Make sure we're still on the same track before swapping its metadata in.
                if (player.currentMediaItemIndex != index) return@runCatching
                val current = player.getMediaItemAt(index)
                if (current.mediaMetadata.artworkUri != uri || current.mediaMetadata.artworkData != null) return@runCatching
                val newItem = current.buildUpon()
                    .setMediaMetadata(
                        current.mediaMetadata.buildUpon()
                            .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                            .build(),
                    )
                    .build()
                player.replaceMediaItem(index, newItem)
            }
        }, java.util.concurrent.Executor { mainThreadHandler.post(it) })
    }

    // ── "Like" control on the system media surfaces ─────────────────────────────

    /** The heart button — filled when the current track is liked, outline otherwise. */
    @OptIn(UnstableApi::class)
    private fun likeButton(liked: Boolean): CommandButton =
        CommandButton.Builder()
            .setDisplayName(if (liked) "Unlike" else "Like")
            .setIconResId(if (liked) R.drawable.verza_ic_heart_filled else R.drawable.verza_ic_heart)
            .setSessionCommand(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
            .setEnabled(true)
            .build()

    private fun isCurrentLiked(): Boolean {
        val id = player.currentMediaItem?.mediaId ?: return false
        return NowPlayingBridge.likedIds.value.contains(id)
    }

    private fun buildCustomLayout(): List<CommandButton> = listOf(likeButton(isCurrentLiked()))

    /** Pushes a fresh custom layout to every controller (notification included). Main thread. */
    @OptIn(UnstableApi::class)
    private fun refreshLikeLayout() {
        if (::session.isInitialized) session.setCustomLayout(buildCustomLayout())
    }

    /** Shows the full diagnostic in debug builds; a short, non-revealing message in release. */
    private fun showPlaybackToast(handler: Handler, debugDetail: String) {
        val text = if (BuildConfig.DEBUG) debugDetail else "Couldn't play this track"
        handler.post { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    }

    /**
     * True when [t] is the result of ExoPlayer cancelling this load (the user skipped while it was
     * still resolving) rather than a genuine playback failure — so we can suppress the error toast.
     */
    private fun isCancellation(t: Throwable): Boolean {
        var e: Throwable? = t
        while (e != null) {
            if (e is InterruptedException || e is InterruptedIOException || e is CancellationException) return true
            e = e.cause
        }
        return Thread.currentThread().isInterrupted
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session

    override fun onDestroy() {
        serviceScope.cancel()
        session.release()
        player.release()
        super.onDestroy()
    }

    // ── Library callbacks ──────────────────────────────────────────────────────
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        /**
         * Grant every controller (the system notification controller included) permission to send
         * our custom "toggle like" command, on top of the default media + library commands, and
         * hand it the current heart layout.
         */
        @OptIn(UnstableApi::class)
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(SessionCommand(ACTION_TOGGLE_LIKE, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setCustomLayout(buildCustomLayout())
                .build()
        }

        /**
         * The heart was tapped on the lock screen / notification / hole-punch / AOD. We hand the
         * current track (the service owns the player, so it always knows what's playing) to :app
         * via [NowPlayingBridge]; :app persists the like and republishes the liked set, which flips
         * the icon back through [refreshLikeLayout].
         */
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_TOGGLE_LIKE) {
                player.currentMediaItem?.let { item ->
                    val md = item.mediaMetadata
                    NowPlayingBridge.requestLikeToggle(
                        NowPlayingBridge.LikeRequest(
                            mediaId = item.mediaId,
                            title = md.title?.toString().orEmpty(),
                            artist = md.artist?.toString().orEmpty(),
                            artworkUri = md.artworkUri?.toString(),
                        )
                    )
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }

        /**
         * Media3 drops MediaItem.localConfiguration (the URI) when items cross the
         * controller → session IPC boundary, so the player would receive URI-less items
         * and refuse to prepare. We rebuild each item's URI from its mediaId here, before it
         * reaches ExoPlayer's ResolvingDataSource.
         *
         * Local tracks carry a self-describing `content://` / `file://` mediaId (on-device music),
         * which we keep verbatim so the file plays directly; everything else is a YouTube videoId
         * and gets the innertube:// placeholder that the resolver swaps for a real stream URL.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                val id = item.mediaId
                val uri = if (id.startsWith("content://") || id.startsWith("file://")) id
                          else "$INNERTUBE_SCHEME$id"
                item.buildUpon().setUri(uri).build()
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }
    }
}
