package com.verza.widget

import android.content.Context
import android.graphics.Bitmap
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the home-screen widget in step with playback.
 *
 * The widget cannot watch the player itself — a broadcast receiver has no lifetime to hold a session
 * binding across — so the app process, which is also the service's process, pushes to it instead.
 *
 * Artwork is cached by URL. A widget update that re-decoded the cover every time the position
 * changed would be a bitmap decode several times a second for a picture that had not changed.
 */
object NowPlayingWidgetUpdater {

    // One scope, rather than a new uncancellable CoroutineScope per refresh — those are never
    // cancelled and each one outlives the call that made it.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var last = WidgetState()
    private var cachedUrl: String? = null
    private var cachedCover: Bitmap? = null
    private val lock = Mutex()

    /**
     * Record a new state and redraw if anything visible changed.
     *
     * Position is deliberately not part of [WidgetState]: the widget shows no progress, so tracking
     * it would repaint on every tick for no visible difference.
     */
    fun publish(context: Context, scope: CoroutineScope, state: WidgetState) {
        if (state == last) return
        last = state
        scope.launch(Dispatchers.IO) { draw(context.applicationContext, state) }
    }

    /** Redraw from the last known state — for a freshly placed widget, or after a button press. */
    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch { draw(app, last) }
    }

    private suspend fun draw(context: Context, state: WidgetState) {
        val cover = lock.withLock { coverFor(context, state.artworkUrl) }
        NowPlayingWidget.render(context, state, cover)
    }

    private suspend fun coverFor(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        if (url == cachedUrl) return cachedCover

        val request = ImageRequest.Builder(context)
            .data(url)
            // RemoteViews cross a process boundary, and a hardware bitmap cannot be parcelled.
            .allowHardware(false)
            .build()
        val bitmap = runCatching {
            // SingletonImageLoader, not a new ImageLoader: Coil 3 ships no network fetcher by
            // default, and VerzaApp registers OkHttp on the singleton. A fresh loader here would
            // fail on every remote cover and succeed only on local files.
            SingletonImageLoader.get(context).execute(request).image?.toBitmap()
        }.getOrNull()

        if (bitmap != null) {
            cachedUrl = url
            cachedCover = bitmap
        }
        return bitmap
    }
}
