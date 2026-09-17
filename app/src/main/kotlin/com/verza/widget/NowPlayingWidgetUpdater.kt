package com.verza.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.verza.ui.expressive.expressiveColorsFrom
import com.verza.ui.theme.CoverColors
import com.verza.ui.theme.DefaultCoverColors
import com.verza.ui.theme.coverColorsFrom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the home-screen widgets in step with playback.
 *
 * A widget cannot watch the player itself (a broadcast receiver has no lifetime to hold a session
 * binding across), so the app process, which is also the service's process, pushes to it instead.
 *
 * The cover and the palette taken from it are cached by URL. Redrawing on a play/pause should not
 * decode the art and run Palette again for a picture that has not changed.
 */
object NowPlayingWidgetUpdater {

    // One scope, rather than a new uncancellable CoroutineScope per refresh. Those are never
    // cancelled and each one outlives the call that made it.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var last = WidgetState()
    private var cachedUrl: String? = null
    private var cachedCover: Bitmap? = null
    private var cachedColors: CoverColors? = null
    private val lock = Mutex()

    /**
     * Record a new state and redraw if anything visible changed.
     *
     * Position is deliberately not part of [WidgetState]: no widget shows progress, so tracking it
     * would repaint on every tick for no visible difference.
     */
    fun publish(context: Context, scope: CoroutineScope, state: WidgetState) {
        if (state == last) return
        last = state
        scope.launch(Dispatchers.IO) { draw(context.applicationContext) }
    }

    /** Redraw from the last known state: for a freshly placed widget, or after a button press. */
    fun refresh(context: Context) {
        val app = context.applicationContext
        scope.launch { draw(app) }
    }

    /**
     * Always draws the newest state, inside the lock. Two draws launched a moment apart are not
     * guaranteed to run in order, and masking covers now takes long enough that an older state could
     * otherwise land on the home screen after a newer one.
     */
    private suspend fun draw(context: Context) = lock.withLock {
        val state = last
        val cover = coverFor(context, state.artworkUrl)
        val palette = if (cover != null) cachedColors else null
        val colors = expressiveColorsFrom(palette ?: DefaultCoverColors, state.flavour, state.accentSource)
        // A widget that fails to draw must not take the app process, and the music, down with it.
        runCatching { WidgetRenderer.render(context, state, cover, colors) }
            .onFailure { Log.w("VerzaWidget", "Widget render failed", it) }
    }

    private suspend fun coverFor(context: Context, url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        if (url == cachedUrl) return cachedCover

        val request = ImageRequest.Builder(context)
            .data(url)
            // RemoteViews cross a process boundary, and a hardware bitmap cannot be parcelled.
            .allowHardware(false)
            // Covers are drawn at 360px at most. A full-size download kept around would be megabytes.
            .size(512)
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
            cachedColors = coverColorsFrom(bitmap)
        }
        return bitmap
    }
}
