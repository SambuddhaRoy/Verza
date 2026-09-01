package com.verza

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.verza.data.CrashLog
import com.verza.di.ApplicationScope
import com.verza.playback.MediaSessionLikeBridge
import com.verza.player.NowPlayingBridge
import com.verza.widget.NowPlayingWidgetUpdater
import com.verza.widget.WidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VerzaApp : Application(), SingletonImageLoader.Factory {

    // Bridges the media-notification "Like" heart to the liked-songs store; process-scoped so it
    // works with or without an Activity. Injected here so Hilt builds it on app start.
    @Inject
    lateinit var mediaSessionLikeBridge: MediaSessionLikeBridge

    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // First thing, before anything else can throw.
        CrashLog.install(this)
        mediaSessionLikeBridge.start()

        // Keep the home-screen widget in step. Done at process scope rather than from an Activity,
        // because the widget has to stay right while the app is nowhere on screen — which is most
        // of the time anyone looks at it.
        scope.launch {
            NowPlayingBridge.nowPlaying.collect { np ->
                NowPlayingWidgetUpdater.publish(
                    context = this@VerzaApp,
                    scope = scope,
                    state = WidgetState(
                        title = np?.title.orEmpty(),
                        artist = np?.artist.orEmpty(),
                        artworkUrl = np?.artworkUri,
                        isPlaying = np?.isPlaying == true,
                    ),
                )
            }
        }
    }

    // Coil 3 ships no network loader by default; register the OkHttp fetcher so remote
    // YouTube Music thumbnails load.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
}
