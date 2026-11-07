package com.verza.data.downloads

import com.verza.data.Settings
import com.verza.data.Song
import com.verza.data.library.Library
import com.verza.data.youtube.Streams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.coroutines.coroutineContext

/** What's downloading: how many are queued and done, the current song, and how far it is (0..1). */
data class DownloadProgress(val total: Int = 0, val done: Int = 0, val failed: Int = 0, val current: String? = null, val fraction: Float = 0f) {
    val running: Boolean get() = done < total
}

/**
 * Saves songs for offline playback, one at a time, into Music/Verza (an album or playlist in its own
 * folder) or the folder the listener chose. A file is only kept once all of it is written; a failed or
 * cancelled one is deleted rather than left looking like a download. The player plays a downloaded
 * song from its file (Resolver), with no network.
 */
class Downloads(
    private val store: DownloadStore,
    private val streams: Streams,
    private val http: OkHttpClient,
    private val library: Library,
    private val settings: Settings,
    scope: CoroutineScope,
) {
    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()
    private val queue = Channel<Pair<Song, String>>(Channel.UNLIMITED)

    init {
        scope.launch(Dispatchers.IO) {
            for ((song, folder) in queue) {
                _progress.update { it.copy(current = song.title, fraction = 0f) }
                val ok = runCatching { save(song, folder) }.getOrDefault(false)
                _progress.update { p ->
                    val next = p.copy(done = p.done + 1, failed = p.failed + if (ok) 0 else 1, current = null)
                    if (next.done >= next.total) DownloadProgress() else next
                }
            }
        }
    }

    /** Queues [songs]; an album or playlist ([collection]) gets a folder of its own. Already saved ones are skipped. */
    fun save(songs: List<Song>, collection: String = "") {
        val folder = if (songs.size > 1) collection else ""
        val fresh = songs.filterNot { it.onPhone }
        _progress.update { it.copy(total = it.total + fresh.size) }
        fresh.forEach { queue.trySend(it to folder) }
    }

    private suspend fun save(song: Song, folder: String): Boolean {
        if (library.downloadPath(song.id) != null) return true
        val stream = streams.resolve(song.id, m4a = true) ?: return false
        val (_, mime) = DownloadNaming.containerFor(stream.mimeType)
        val target = store.create(settings.downloadFolder.get(), DownloadNaming.fileName(song.artist, song.title, song.id, stream.mimeType), mime, folder)
        try {
            http.newCall(Request.Builder().url(stream.url).build()).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body!!
                val length = body.contentLength()
                target.open().use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(64 * 1024)
                        var copied = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            out.write(buffer, 0, n)
                            copied += n
                            if (length > 0) _progress.update { it.copy(fraction = copied.toFloat() / length) }
                        }
                    }
                }
            }
            target.commit()
            library.downloaded(song, target.location)
            return true
        } catch (e: Throwable) {
            target.discard()
            throw e
        }
    }
}
