package com.verza.data.downloads

import android.content.Context
import com.verza.data.Settings
import com.verza.data.Song
import com.verza.data.artAt
import com.verza.data.isAlbumArt
import com.verza.data.library.Library
import com.verza.data.library.SongRow
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
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.coroutineContext

/**
 * What's downloading: how many are queued and done, the current song, and how far it is (0..1).
 * [sorting] when the work is moving earlier downloads into place rather than downloading.
 */
data class DownloadProgress(
    val total: Int = 0, val done: Int = 0, val failed: Int = 0, val current: String? = null, val fraction: Float = 0f,
    val sorting: Boolean = false,
) {
    val running: Boolean get() = done < total
}

/** How much of a file each request asks YouTube for. */
private const val CHUNK = 4L shl 20

/**
 * Saves songs for offline playback, one at a time. Each lands in a temporary file, gets its tags
 * ([Tags]), and only then moves into Music/Verza (or the folder the listener chose) under
 * Artist/Album/, so the music folder only ever holds whole, tagged songs. The player plays a
 * downloaded song from its file (Resolver), with no network.
 *
 * With Lossless chosen and a Soulseek account, a song is a FLAC from someone sharing it
 * ([Soulseek.fetchFlac]), and YouTube's stream only when nobody does or every copy stalls.
 */
class Downloads(
    private val context: Context,
    private val store: DownloadStore,
    private val streams: Streams,
    private val youtube: YouTube,
    private val http: OkHttpClient,
    private val library: Library,
    private val settings: Settings,
    private val scope: CoroutineScope,
) {
    private class Job(val title: String, val sorting: Boolean, val run: suspend () -> Boolean)

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()
    private val queue = Channel<Job>(Channel.UNLIMITED)

    init {
        // What a killed process was part way through; nothing is running yet
        File(context.cacheDir, "downloading").deleteRecursively()
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
            for (job in queue) {
                _progress.update { it.copy(current = job.title, fraction = 0f, sorting = job.sorting) }
                val ok = runCatching { job.run() }.getOrDefault(false)
                _progress.update { p ->
                    val next = p.copy(done = p.done + 1, failed = p.failed + if (ok) 0 else 1, current = null)
                    if (next.done >= next.total) DownloadProgress() else next
                }
            }
        }
    }

    /** Queues [songs]. Already saved ones, and files already on the phone, are skipped. */
    fun save(songs: List<Song>) {
        val fresh = songs.filterNot { it.onPhone }
        _progress.update { it.copy(total = it.total + fresh.size) }
        fresh.forEach { s -> queue.trySend(Job(s.title, sorting = false) { save(s) }) }
    }

    /**
     * Moves the downloads saved before Artist/Album folders into them, tagging each on the way:
     * they were "Artist - Title" files with no tags, loose in Music/Verza or in a folder per playlist.
     * Once; a song that can't be done yet (offline) is tried again at the next launch.
     *
     * ponytail: a launch killed mid-way moves the songs it had done once more, into the same folder, and
     * the folder names the second copy "Title (1)". Keep a list of the ones done if that ever matters.
     */
    fun sortEarlier() {
        scope.launch(Dispatchers.IO) {
            if (settings.downloadsSorted.get()) return@launch
            val rows = library.downloadRows().filter { it.downloadPath != null }
            if (rows.isEmpty()) { settings.downloadsSorted.set(true); return@launch }
            val left = AtomicInteger(rows.size)
            val failed = AtomicInteger(0)
            _progress.update { it.copy(total = it.total + rows.size) }
            for (row in rows) queue.send(Job(row.title, sorting = true) {
                val ok = runCatching { refile(row) }.getOrDefault(false)
                if (!ok) failed.incrementAndGet()
                if (left.decrementAndGet() == 0 && failed.get() == 0) settings.downloadsSorted.set(true)
                ok
            })
        }
    }

    /**
     * Deletes [song]'s file and forgets it, so it streams again. False when the file is there and can't
     * be deleted: one an earlier install saved, which Android lets only that install delete.
     */
    suspend fun remove(song: Song): Boolean {
        val path = library.downloadPath(song.id) ?: return true
        val gone = store.delete(path) || store.nameOf(path) == null
        if (gone) library.forgetDownload(song.id)
        return gone
    }

    private suspend fun save(given: Song): Boolean {
        if (library.downloadPath(given.id) != null) return true
        // A search row is often just "Coldplay": the album decides the folder and finds the tags, and a
        // lossless download needs the length to tell the song from a preview or another edit
        val song = runCatching { youtube.complete(given) }.getOrDefault(given)
        val file = flac(song) ?: regular(song) ?: return false
        place(song, file)
        return true
    }

    /** A FLAC from Soulseek when that's what the listener chose; null to download YouTube's instead. */
    private suspend fun flac(song: Song): File? {
        if (!settings.losslessDownloads.get() || settings.soulseekUser.get().isBlank()) return null
        // Not knowing how long the song is, any file would pass for it, a 30-second preview included
        if (song.durationMs <= 0) return null
        return try {
            Soulseek.fetchFlac(context, song.artist, song.title, song.album, song.durationMs) { f -> _progress.update { it.copy(fraction = f) } }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            null                              // Soulseek trouble is no reason not to download at all
        }
    }

    /** YouTube's stream, whole, in a temporary file. */
    private suspend fun regular(song: Song): File? {
        val stream = streams.resolve(song.id, m4a = true) ?: return null
        val file = temp(DownloadNaming.fileName(song.artist, song.title, song.id, stream.mimeType))
        try {
            // In pieces: YouTube sends one long request at about the song's own speed, so a whole file
            // in one took as long as listening to it. Pieces of a few MB come at full speed.
            file.outputStream().use { out ->
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
            return file
        } catch (e: Throwable) {
            file.parentFile?.deleteRecursively()
            throw e
        }
    }

    /**
     * Tags [file] and moves it into its album's folder, where the library finds it from now on. The
     * temporary file goes either way. Where it was put.
     */
    private suspend fun place(song: Song, file: File): String {
        try {
            Tags.write(context, file, song) { cover(song) }
            val (albumArtist, album) = Tags.read(file)
            val ext = file.extension.lowercase()
            val target = store.create(
                settings.downloadFolder.get(),
                DownloadNaming.fileName(song.artist, song.title, song.id, ext),
                DownloadNaming.containerFor(ext).second,
                DownloadNaming.folders(albumArtist ?: song.artist, album ?: song.album),
            )
            try {
                target.open().use { out -> file.inputStream().use { it.copyTo(out) } }
                target.commit()
            } catch (e: Throwable) {
                target.discard()
                throw e
            }
            library.downloaded(song, target.location)
            return target.location
        } finally {
            file.parentFile?.deleteRecursively()
        }
    }

    /** An earlier download, copied out, tagged and put in its folder; the old file goes once the new one is in. */
    private suspend fun refile(row: SongRow): Boolean {
        val old = row.downloadPath ?: return true
        if (library.downloadPath(row.id) != old) return true          // downloaded again since
        val name = store.nameOf(old) ?: return true                    // deleted outside the app
        val song = youtube.complete(row.toSong())                      // offline: try again next launch
        val file = temp(DownloadNaming.fileName(song.artist, song.title, song.id, name.substringAfterLast('.', "m4a")))
        try {
            store.read(old).use { input -> file.outputStream().use { input.copyTo(it) } }
        } catch (e: Throwable) {
            file.parentFile?.deleteRecursively()
            throw e
        }
        val placed = place(song, file)
        // A file an earlier install saved can't be deleted by this one: keep it where it is, not twice
        if (!store.delete(old)) { store.delete(placed); library.downloaded(song, old) }
        return true
    }

    /** YouTube Music's cover for the song, for a file MusicBrainz found none for. A video's frame isn't one. */
    private fun cover(song: Song): ByteArray? = song.art?.takeIf(::isAlbumArt)?.let { url ->
        runCatching { http.newCall(Request.Builder().url(artAt(url, 1200)).build()).execute().use { r -> if (r.isSuccessful) r.body?.bytes() else null } }.getOrNull()
    }

    /** A file called [name] in a folder of its own, under the app's cache. */
    private fun temp(name: String): File =
        File(File(context.cacheDir, "downloading"), UUID.randomUUID().toString()).apply { mkdirs() }.let { File(it, name) }
}
