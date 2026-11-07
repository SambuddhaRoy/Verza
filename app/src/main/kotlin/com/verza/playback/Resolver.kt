package com.verza.playback

import android.net.ConnectivityManager
import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.verza.Graph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/** The placeholder a queued YouTube song plays from until the resolver says where it really is. */
const val SCHEME = "verza://"

/** A queued song's URI: its id, and what a lossless source needs to search for it. */
fun placeholder(id: String, title: String, artist: String, durationMs: Long): Uri =
    Uri.parse("$SCHEME$id?t=${Uri.encode(title)}&a=${Uri.encode(artist)}&d=$durationMs")

/**
 * Where a song plays from, decided as the player opens it: a download first, then a URL looked up
 * recently, then the listener's lossless sources, then YouTube. Lookups are shared by song, so a
 * skip onto a song the look-ahead is still finding joins that lookup, and the answer is kept for
 * every later open (each seek opens the source again).
 */
class Resolver(private val graph: Graph) : ResolvingDataSource.Resolver {

    private class Found(val url: String, val at: Long, val fromSource: Boolean)

    private val found = ConcurrentHashMap<String, Found>()
    private val inflight = ConcurrentHashMap<String, Deferred<Found?>>()
    /** Songs whose source link failed in the player: YouTube plays them for the rest of the session. */
    private val sourceFailed = ConcurrentHashMap.newKeySet<String>()
    /** URLs a load failed on. YouTube's are bound to the network that asked, so a change kills them all. */
    private val deadUrls = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun resolveDataSpec(spec: DataSpec): DataSpec {
        val raw = spec.uri.toString()
        // Files on the phone and downloads named by path open as they are.
        if (!raw.startsWith(SCHEME)) return spec
        val id = raw.removePrefix(SCHEME).substringBefore('?')
        try {
            runBlocking { graph.library.downloadPath(id) }?.let { path ->
                if (path.startsWith("content://") || File(path).exists()) return spec.withUri(Uri.parse(path))
            }
            usable(id)?.let { return spec.withUri(Uri.parse(it.url)) }
            val hit = runBlocking { lookup(id, spec.uri).await() } ?: throw IOException("Nothing to play for $id")
            return spec.withUri(Uri.parse(hit.url))
        } catch (t: Throwable) {
            // A load the listener skipped away from is cancelled by interrupting this thread. That's
            // not a failure: rethrow quietly and keep the interrupt for the loader.
            if (cancelled(t)) { Thread.currentThread().interrupt(); throw InterruptedIOException("skipped") }
            throw t as? IOException ?: IOException(t)
        }
    }

    /** Looks up the songs at these URIs ahead of time; a skip onto one then finds it done. */
    fun lookAhead(uris: List<Uri>) {
        for (uri in uris) {
            val raw = uri.toString()
            if (!raw.startsWith(SCHEME)) continue
            val id = raw.removePrefix(SCHEME).substringBefore('?')
            if (usable(id) != null) continue
            scope.launch { if (graph.library.downloadPath(id) == null) runCatching { lookup(id, uri).await() } }
        }
    }

    /**
     * The player failed on this song's URL. A source's file sends the song to YouTube for good; any
     * URL is forgotten so the next open looks again. True when it was a source's, and worth retrying.
     */
    fun failed(id: String): Boolean {
        val f = found.remove(id) ?: return false
        deadUrls += f.url
        if (deadUrls.size > 256) deadUrls.clear()
        if (f.fromSource) sourceFailed += id
        return f.fromSource
    }

    /** The sources changed, so every song's answer may have too. */
    fun forgetSources() { found.clear(); sourceFailed.clear() }

    fun close() = scope.cancel()

    private fun usable(id: String): Found? = found[id]?.takeIf { f ->
        System.currentTimeMillis() - f.at < (if (f.fromSource) SOURCE_TTL else YOUTUBE_TTL) &&
            f.url !in deadUrls && (!f.fromSource || sourcesAllowed())
    }

    private fun lookup(id: String, hint: Uri): Deferred<Found?> =
        inflight.computeIfAbsent(id) {
            scope.async(start = CoroutineStart.LAZY) {
                try { fresh(id, hint)?.also { found[id] = it } } finally { inflight.remove(id) }
            }
        }.also { it.start() }

    private fun fresh(id: String, hint: Uri): Found? {
        if (id !in sourceFailed && sourcesAllowed()) {
            graph.sources.find(
                title = hint.getQueryParameter("t").orEmpty(),
                artist = hint.getQueryParameter("a").orEmpty(),
                durationMs = hint.getQueryParameter("d")?.toLongOrNull() ?: 0,
            )?.let { return Found(it, System.currentTimeMillis(), fromSource = true) }
        }
        return graph.streams.resolve(id)?.let { Found(it.url, System.currentTimeMillis(), fromSource = false) }
    }

    /** Sources are asked when there are any, and not on mobile data unless the listener allowed it. */
    private fun sourcesAllowed(): Boolean {
        if (graph.sourceList.value.isEmpty()) return false
        if (graph.sourcesOnMobileData.value) return true
        return !graph.context.getSystemService(ConnectivityManager::class.java).isActiveNetworkMetered
    }

    private fun cancelled(t: Throwable): Boolean {
        var e: Throwable? = t
        while (e != null) {
            if (e is InterruptedException || e is InterruptedIOException || e is CancellationException) return true
            e = e.cause
        }
        return Thread.currentThread().isInterrupted
    }

    private companion object {
        const val YOUTUBE_TTL = 5 * 60 * 60 * 1000L // its URLs last about six hours
        const val SOURCE_TTL = 30 * 60 * 1000L      // a source's links can be signed for much less
    }
}
