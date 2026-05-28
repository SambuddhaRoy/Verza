package com.verza.player

import android.app.PendingIntent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.verza.innertube.InnerTube
import dagger.hilt.android.AndroidEntryPoint
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

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Base HTTP source for the actual googlevideo stream bytes.
        val httpFactory = OkHttpDataSource.Factory(okHttpClient)

        // Intercept our placeholder innertube://<videoId> URIs and swap in a freshly
        // resolved stream URL right before ExoPlayer opens the connection. Resolution
        // runs on ExoPlayer's loader thread (never the main thread), so blocking is fine.
        val resolver = ResolvingDataSource.Resolver { dataSpec ->
            val raw = dataSpec.uri.toString()
            if (!raw.startsWith(INNERTUBE_SCHEME)) return@Resolver dataSpec
            val videoId = raw.removePrefix(INNERTUBE_SCHEME)

            // Prefer a downloaded copy if one exists — no network, instant start, works offline.
            val cached = runBlocking { downloadLookup.pathFor(videoId) }
            if (!cached.isNullOrBlank()) {
                val file = File(cached)
                if (file.exists()) return@Resolver dataSpec.withUri(Uri.fromFile(file))
            }

            val stream = runBlocking { InnerTube.resolveAudioStream(videoId) }
                ?: throw IOException("No playable audio stream for videoId=$videoId")
            dataSpec.withUri(Uri.parse(stream.url))
        }
        val dataSourceFactory = ResolvingDataSource.Factory(httpFactory, resolver)

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

        val activityIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE) }

        session = MediaLibrarySession.Builder(this, player, LibrarySessionCallback())
            .also { builder -> activityIntent?.let { builder.setSessionActivity(it) } }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession = session

    override fun onDestroy() {
        session.release()
        player.release()
        super.onDestroy()
    }

    // ── Library callbacks ──────────────────────────────────────────────────────
    private inner class LibrarySessionCallback : MediaLibrarySession.Callback {

        /**
         * Media3 drops MediaItem.localConfiguration (the URI) when items cross the
         * controller → session IPC boundary, so the player would receive URI-less items
         * and refuse to prepare. We rebuild each item's innertube:// URI from its mediaId
         * (the videoId) here, before it reaches ExoPlayer's ResolvingDataSource.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.map { item ->
                item.buildUpon()
                    .setUri("$INNERTUBE_SCHEME${item.mediaId}")
                    .build()
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }
    }
}
