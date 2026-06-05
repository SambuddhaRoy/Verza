package com.verza.player

import android.app.PendingIntent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.verza.innertube.InnerTube
import com.verza.player.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import javax.inject.Inject

private const val INNERTUBE_SCHEME = "innertube://"

@AndroidEntryPoint
class MusicService : MediaLibraryService() {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var downloadLookup: DownloadLookup

    private lateinit var player: ExoPlayer
    private lateinit var session: MediaLibrarySession

    // Service-lifetime scope for observing app-pushed playback options (see PlayerSettings).
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

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

                val stream = runBlocking { InnerTube.resolveAudioStream(videoId) }
                if (stream == null) {
                    if (BuildConfig.DEBUG) {
                        Log.e("VerzaPlayback", "No stream for $videoId — ${InnerTube.lastResolveDiagnostic}")
                    }
                    showPlaybackToast(mainHandler, debugDetail = "No stream for $videoId\n${InnerTube.lastResolveDiagnostic}")
                    throw IOException("No playable audio stream for $videoId")
                }
                if (BuildConfig.DEBUG) Log.i("VerzaPlayback", "Resolved $videoId → ${stream.url.take(120)}…")
                dataSpec.withUri(Uri.parse(stream.url))
            } catch (t: Throwable) {
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

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .also { builder -> activityIntent?.let { builder.setSessionActivity(it) } }
            .build()
    }

    /** Shows the full diagnostic in debug builds; a short, non-revealing message in release. */
    private fun showPlaybackToast(handler: Handler, debugDetail: String) {
        val text = if (BuildConfig.DEBUG) debugDetail else "Couldn't play this track"
        handler.post { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
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
