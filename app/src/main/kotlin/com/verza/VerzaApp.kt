package com.verza

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.verza.playback.MediaSessionLikeBridge
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class VerzaApp : Application(), SingletonImageLoader.Factory {

    // Bridges the media-notification "Like" heart to the liked-songs store; process-scoped so it
    // works with or without an Activity. Injected here so Hilt builds it on app start.
    @Inject
    lateinit var mediaSessionLikeBridge: MediaSessionLikeBridge

    override fun onCreate() {
        super.onCreate()
        mediaSessionLikeBridge.start()
    }

    // Coil 3 ships no network loader by default; register the OkHttp fetcher so remote
    // YouTube Music thumbnails load.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
}
