package com.verza.data

import com.verza.data.db.SongEntity
import com.verza.innertube.InnerTube
import com.verza.innertube.models.MusicItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 * download, cancellable via [remove]. State is exposed as the set of in-flight ids so the UI can
 * show progress affordances.
 */
@Singleton
class DownloadManager @Inject constructor(
    private val httpClient: OkHttpClient,
    private val library: LibraryRepository,
    private val store: DownloadStore,
    private val prefs: PreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()

    private val _inProgress = MutableStateFlow<Set<String>>(emptySet())
    val inProgress: StateFlow<Set<String>> = _inProgress.asStateFlow()

    /** [collection] is the playlist or album this came from; it becomes the folder on disk. */
    fun download(item: MusicItem, collection: String = "") {
        if (jobs.containsKey(item.id)) return
        _inProgress.update { it + item.id }
        jobs[item.id] = scope.launch {
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
                    resp.body!!.byteStream().use { input ->
                        target.open().use { output -> input.copyTo(output) }
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
                _inProgress.update { it - item.id }
            }
        }
    }

    /**
     * Download a whole playlist or album into its own folder. Sequential on purpose: a hundred
     * parallel extractions is a good way to get throttled, and nobody is waiting on track 87.
     */
    fun downloadAll(items: List<MusicItem>, collection: String) {
        val name = if (items.size > 1) collection else ""
        scope.launch { items.forEach { download(it, name) } }
    }

    /** Cancels an in-flight download (if any) and removes the saved file + DB marker. */
    fun remove(id: String) {
        jobs.remove(id)?.cancel()
        _inProgress.update { it - id }
        scope.launch {
            library.get(id)?.downloadPath?.let { store.delete(it) }
            library.clearDownloadPath(id)
        }
    }
}

private fun MusicItem.toEntity() = SongEntity(
    id = id,
    title = title,
    artist = artist,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs,
)
