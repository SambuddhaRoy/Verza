package com.verza.data

import android.content.Context
import com.verza.data.db.SongEntity
import com.verza.innertube.InnerTube
import com.verza.innertube.models.MusicItem
import dagger.hilt.android.qualifiers.ApplicationContext
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
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foreground-thread-safe downloader. Resolves the audio stream for a [MusicItem] via NewPipe,
 * streams the bytes to app-private storage, then upserts the [SongEntity] so the resolver in
 * MusicService can prefer the local file on subsequent plays.
 *
 * Designed to be simple — no WorkManager scheduling, no eviction. A scope-bound coroutine per
 * download, cancellable via [cancel]. State is exposed as the set of in-flight ids so the UI can
 * show progress affordances.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient,
    private val library: LibraryRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()

    private val _inProgress = MutableStateFlow<Set<String>>(emptySet())
    val inProgress: StateFlow<Set<String>> = _inProgress.asStateFlow()

    fun download(item: MusicItem) {
        if (jobs.containsKey(item.id)) return
        _inProgress.update { it + item.id }
        jobs[item.id] = scope.launch {
            try {
                val stream = InnerTube.resolveAudioStream(item.id) ?: return@launch
                val dir = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }
                // Pick an extension that roughly matches the container — many decoders sniff
                // the bytes anyway, but a sensible suffix helps file managers.
                val ext = when {
                    stream.mimeType.contains("mp4", true) || stream.mimeType.contains("aac", true) -> "m4a"
                    stream.mimeType.contains("opus", true) -> "opus"
                    stream.mimeType.contains("webm", true) -> "webm"
                    else -> "audio"
                }
                val file = File(dir, "${item.id}.$ext")
                val tmp = File(dir, "${item.id}.$ext.part")

                val request = Request.Builder().url(stream.url).get().build()
                httpClient.newCall(request).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    resp.body!!.byteStream().use { input ->
                        tmp.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                // Atomic rename so an interrupted download never leaves a half-written file in place.
                if (tmp.renameTo(file)) {
                    library.markDownloaded(item.toEntity(), file.absolutePath)
                } else {
                    tmp.delete()
                }
            } catch (_: Throwable) {
                // Best effort — failures leave the song unmarked, the user can retry.
            } finally {
                jobs.remove(item.id)
                _inProgress.update { it - item.id }
            }
        }
    }

    /** Cancels an in-flight download (if any) and removes the on-disk file + DB marker. */
    fun remove(id: String) {
        jobs.remove(id)?.cancel()
        _inProgress.update { it - id }
        scope.launch {
            val entry = library.get(id)
            entry?.downloadPath?.let { runCatching { File(it).delete() } }
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
