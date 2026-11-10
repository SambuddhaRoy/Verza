package com.verza.data.downloads

import android.content.Context
import com.verza.data.Settings
import com.verza.data.Song
import com.verza.data.library.Library
import com.verza.data.youtube.Streams
import com.verza.data.youtube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
 *
 * Signed in to Soulseek, a song is a FLAC from someone sharing it ([Soulseek.fetchFlac]), and YouTube's
 * stream only when nobody does or every copy stalls.
 */
/** How much of a file each request asks YouTube for. */
private const val CHUNK = 4L shl 20

class Downloads(
    private val context: Context,
    private val store: DownloadStore,
    private val streams: Streams,
    private val youtube: YouTube,
    private val http: OkHttpClient,
    private val library: Library,
    private val settings: Settings,
    scope: CoroutineScope,
) {
    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()
    private val queue = Channel<Pair<Song, String>>(Channel.UNLIMITED)

    init {
        // A Soulseek download can sit in a queue for a minute, and Android cuts a cached app's network
        // (Built on the first download, so the first value seen can already be "running".)
        scope.launch {
            var on = false
            progress.map { it.running }.distinctUntilChanged().collect { running ->
                if (running != on) DownloadService.set(context, running)
                on = running
            }
        }
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
        if (settings.losslessDownloads.get() && settings.soulseekUser.get().isNotBlank() && saveFlac(song, folder)) return true
        val stream = streams.resolve(song.id, m4a = true) ?: return false
        val (_, mime) = DownloadNaming.containerFor(stream.mimeType)
        val target = store.create(settings.downloadFolder.get(), DownloadNaming.fileName(song.artist, song.title, song.id, stream.mimeType), mime, folder)
        try {
            // In pieces: YouTube sends one long request at about the song's own speed, so a whole file
            // in one took as long as listening to it. Pieces of a few MB come at full speed.
            target.open().use { out ->
                val buffer = ByteArray(64 * 1024)
                var copied = 0L
                var length = -1L
                while (length < 0 || copied < length) {
                    val range = "bytes=$copied-${copied + CHUNK - 1}"
                    val whole = http.newCall(Request.Builder().url(stream.url).header("Range", range).build()).execute().use { resp ->
                        if (!resp.isSuccessful) error("HTTP ${resp.code}")
                        // 206 says how long the file is; a 200 is the whole of it, ranges ignored
                        length = resp.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                            ?: if (resp.code == 200) resp.body!!.contentLength() else error("no Content-Range")
                        val before = copied
                        resp.body!!.byteStream().use { input ->
                            while (true) {
                                coroutineContext.ensureActive()
                                val n = input.read(buffer)
                                if (n < 0) break
                                out.write(buffer, 0, n)
                                copied += n
                                if (length > 0) _progress.update { it.copy(fraction = copied.toFloat() / length) }
                            }
                        }
                        if (copied == before) error("empty piece at $before of $length")
                        resp.code == 200
                    }
                    if (whole) break
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

    /** The song as a FLAC from Soulseek, tagged and moved into place; false to fall back to YouTube. */
    private suspend fun saveFlac(given: Song, folder: String): Boolean {
        var song = given
        val flac = try {
            song = youtube.complete(given)
            // Not knowing how long the song is, any file would pass for it, a 30-second preview included
            if (song.durationMs <= 0) return false
            Soulseek.fetchFlac(context, song.artist, song.title, song.album, song.durationMs) { f ->
                _progress.update { it.copy(fraction = f) }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null                              // Soulseek trouble is no reason not to download at all
        } ?: return false
        try {
            val target = store.create(settings.downloadFolder.get(), DownloadNaming.fileName(song.artist, song.title, song.id, "audio/flac"), "audio/flac", folder)
            try {
                target.open().use { out -> flac.inputStream().use { it.copyTo(out) } }
                target.commit()
                library.downloaded(song, target.location)
                return true
            } catch (e: Throwable) {
                target.discard()
                throw e
            }
        } finally {
            flac.parentFile?.deleteRecursively()
        }
    }
}
