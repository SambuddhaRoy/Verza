package com.verza

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade

class VerzaApp : Application(), SingletonImageLoader.Factory {
    lateinit var graph: Graph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = Graph(this)
    }

    /** Every picture fades in as it arrives, rather than appearing in one frame. */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context).crossfade(true).build()
}

/** The app's [Graph], from any context. */
val android.content.Context.graph: Graph get() = (applicationContext as VerzaApp).graph
