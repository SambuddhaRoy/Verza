package com.verza.data

import com.verza.data.db.SongEntity
import com.verza.innertube.InnerTube
import com.verza.innertube.models.MusicItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foreground-thread-safe downloader. Resolves the audio stream for a [MusicItem] via NewPipe,
 * streams the bytes to wherever [DownloadStore] says they go, then upserts the [SongEntity] so the
 * resolver in MusicService can prefer the local copy on subsequent plays.
 *
 * Two things matter about the output beyond "it downloaded". It asks the extractor for AAC-in-MP4
 * rather than taking whatever is cheapest, because a .webm of Opus is a file most other players
 * refuse; and it is named "Artist - Title", because a folder of videoIds is not a music library.
 *
 * Designed to be simple — no WorkManager scheduling, no eviction. A scope-bound coroutine per
 * download, cancellable via [remove]. State is exposed as [progress], for the progress bar, and the
 * set of in-flight ids.
 */
@Singleton
class DownloadManager @Inject constructor(
    private val httpClient: OkHttpClient,
    private val library: LibraryRepository,
    private val store: DownloadStore,
    private val prefs: PreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Concurrent because download() is called on the main thread while each download's finally
    // block removes its entry from an IO thread — two threads rehashing one LinkedHashMap, which
    // loses entries (so the download can no longer be cancelled) or corrupts a bucket outright.
    private val jobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    private val _progress = MutableStateFlow(DownloadProgress())
    val progress: StateFlow<DownloadProgress> = _progress.asStateFlow()
    val inProgress: StateFlow<Set<String>> =
        _progress.map { it.running.keys }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    /** [collection] is the playlist or album this came from; it becomes the folder on disk. */
    fun download(item: MusicItem, collection: String = "") {
        start(item, collection, counted = false)
    }

    /**
     * Starts [item] unless it is already running, which returns false. [counted] means
     * [downloadAll] has already added it to the batch's total.
     */
    private fun start(item: MusicItem, collection: String, counted: Boolean): Boolean {
        if (jobs.containsKey(item.id)) return false
        _progress.update {
            if (counted) it.copy(running = it.running + (item.id to 0f))
            else it.copy(label = item.title, total = it.total + 1, running = it.running + (item.id to 0f))
        }
        // Lazy, so the job is in [jobs] before it can run: a download that failed straight away used
        // to finish before it was registered, and the stale entry blocked that song for good.
        val job = scope.launch(start = CoroutineStart.LAZY) {
            // Held until the bytes are committed, so any failure — including cancellation — takes the
            // partial file with it rather than leaving something that later looks like a real download.
            var pending: DownloadStore.Target? = null
            try {
                val stream = InnerTube.resolveAudioStream(item.id, preferM4a = true) ?: return@launch
                val (_, mime) = DownloadNaming.containerFor(stream.mimeType)
                val name = DownloadNaming.fileName(item.artist, item.title, item.id, stream.mimeType)
                val target = store.create(prefs.downloadTree(), name, mime, collection)
                pending = target

                val request = Request.Builder().url(stream.url).get().build()
                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val body = resp.body!!
                    val length = body.contentLength()   // -1 when the server doesn't say
                    body.byteStream().use { input ->
                        target.open().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var copied = 0L
                            var shown = 0
                            while (true) {
                                ensureActive()      // so remove() stops the transfer too
                                val n = input.read(buffer)
                                if (n < 0) break
                                output.write(buffer, 0, n)
                                copied += n
                                // Whole percents only, so a song is a hundred updates, not thousands.
                                val pct = if (length > 0) (copied * 100 / length).toInt() else 0
                                if (pct != shown) {
                                    shown = pct
                                    _progress.update { p ->
                                        if (item.id in p.running) p.copy(running = p.running + (item.id to pct / 100f)) else p
                                    }
                                }
                            }
                        }
                    }
                }
                target.commit()                   // MediaStore hides the file until this clears IS_PENDING
                library.markDownloaded(item.toEntity(), target.location)
                pending = null                    // committed; leave it on disk
            } catch (_: Throwable) {
                // Best effort — failures leave the song unmarked, the user can retry.
            } finally {
                pending?.discard()
                jobs.remove(item.id)
                finished(item.id)
            }
        }
        jobs[item.id] = job
        job.start()
        return true
    }

    /** One song of the batch is over, saved or not. The last one ends the batch. */
    private fun finished(id: String?) = _progress.update {
        val next = it.copy(done = it.done + 1, running = if (id == null) it.running else it.running - id)
        if (next.done >= next.total) DownloadProgress() else next
    }

    /**
     * Download a whole playlist or album into its own folder. Sequential on purpose: a hundred
     * parallel extractions is a good way to get throttled, and nobody is waiting on track 87.
     */
    fun downloadAll(items: List<MusicItem>, collection: String) {
        if (items.isEmpty()) return
        val name = if (items.size > 1) collection else ""
        // The whole collection joins the batch now, so the bar reads "3 of 12" from the start.
        _progress.update { it.copy(label = collection, total = it.total + items.size) }
        scope.launch {
            // download() is fire-and-forget, so forEach alone started every track at once — the
            // hundred parallel extractions the comment above says this avoids. Wait for each job so
            // "sequential on purpose" is actually true.
            for (item in items) {
                // Already running on its own, so already counted once: settle this count now.
                if (!start(item, name, counted = true)) finished(null)
                jobs[item.id]?.join()
            }
        }
    }

    /** Cancels an in-flight download (if any) and removes the saved file + DB marker. */
    fun remove(id: String) {
        jobs.remove(id)?.cancel()           // its finally settles the progress
        scope.launch {
            library.get(id)?.downloadPath?.let { store.delete(it) }
            library.clearDownloadPath(id)
        }
    }
}

/**
 * Every download since the queue was last empty, as one batch: songs asked for, songs finished
 * (saved or not), and how far each running one has got, 0 to 1.
 */
data class DownloadProgress(
    val label: String = "",
    val done: Int = 0,
    val total: Int = 0,
    val running: Map<String, Float> = emptyMap(),
) {
    val active: Boolean get() = total > 0
    val fraction: Float get() = if (total == 0) 0f else ((done + running.values.sum()) / total).coerceIn(0f, 1f)
}

private fun MusicItem.toEntity() = SongEntity(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs,
)
