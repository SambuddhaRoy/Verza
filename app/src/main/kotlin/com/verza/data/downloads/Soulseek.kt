package com.verza.data.downloads

import android.content.Context
import com.chaquo.python.PyObject
import com.verza.BuildConfig
import com.verza.data.sources.flacSeconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Lossless downloads from Soulseek, through the Nicotine+ core (pynicotine 3.3.11, unmodified) on
 * Chaquopy's Python. src/main/python/bridge.py drives it; tagger.py fills in tags and the cover.
 *
 * Started on first use, never at launch: it's a Python interpreter. It can't be restarted inside a
 * process, so it's never quit, only disconnected. The account lives in the core's own config under
 * filesDir/soulseek, which backups exclude; [PreferencesRepository] only remembers the name.
 */
object Soulseek {

    data class Status(val state: String = "offline", val user: String = "", val error: String? = null) {
        val online get() = state == "online"
    }

    private data class Transfer(val size: Long, val pos: Long, val status: String)

    private const val SEARCH_MS = 8_000L        // most answers arrive in the first few seconds
    private const val STALL_MS = 60_000L        // queued behind others, or a peer that never answers
    private const val CANDIDATES = 3
    private val GOING = setOf("Queued", "Getting status", "Transferring", "Finished")

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()
    private val results = MutableSharedFlow<Pair<String, List<SoulseekMatch.Hit>>>(extraBufferCapacity = 256)
    private val transfers = MutableStateFlow<Map<String, Transfer>>(emptyMap())
    private val downloaded = MutableSharedFlow<String>(extraBufferCapacity = 16)

    private var bridge: PyObject? = null
    private var started = false
    private val ready = CountDownLatch(1)
    private val commands = Executors.newSingleThreadExecutor()
    private lateinit var downloadRoot: File

    @Synchronized
    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        // App-specific external storage: no permission needed. Finished files are moved into the library.
        downloadRoot = File(app.getExternalFilesDir(null) ?: app.filesDir, "soulseek")
        thread(name = "soulseek", isDaemon = true) {
            val module = Tags.python(app).getModule("bridge")
            bridge = module
            ready.countDown()
            module.callAttr(
                "run", Listener(), File(app.filesDir, "soulseek").path, downloadRoot.path,
                "Verza/${BuildConfig.VERSION_NAME} ( https://github.com/SambuddhaRoy/Verza )",
            )
        }
    }

    fun login(context: Context, user: String, password: String) {
        start(context)
        _status.value = Status("connecting", user)   // so a stale error isn't read as this attempt's answer
        cmd("login", "user" to user, "password" to password)
    }

    fun logout(context: Context) {
        start(context)
        _status.value = Status()
        cmd("logout")
    }

    /**
     * Searches Soulseek for this song and downloads the best FLAC of it into a folder of its own.
     * Null when it isn't signed in, nobody shares the song, or every candidate stalled or failed.
     * The caller owns the returned file and should delete its folder once it's moved.
     */
    suspend fun fetchFlac(
        context: Context, artist: String, title: String, album: String?, durationMs: Long, onProgress: (Float) -> Unit,
    ): File? {
        start(context)
        // Kicked by a sign-in elsewhere, or a dropped network: a download is the user asking to be back
        status.value.let { if (it.error != null) { _status.value = Status("connecting", it.user); cmd("reconnect") } }
        val connected = withTimeoutOrNull(20_000) { status.first { it.online || it.error != null } }
        if (connected?.online != true) return null

        val id = UUID.randomUUID().toString()
        val hits = mutableListOf<SoulseekMatch.Hit>()
        coroutineScope {
            val listening = launch(start = CoroutineStart.UNDISPATCHED) {
                results.collect { (from, found) -> if (from == id) synchronized(hits) { hits += found } }
            }
            cmd("search", "id" to id, "text" to SoulseekMatch.query(artist, title))
            delay(SEARCH_MS)
            listening.cancel()
        }
        cmd("stop_search", "id" to id)

        val tags = JSONObject(mapOf("title" to title, "artist" to artist, "album" to album))
        for (hit in SoulseekMatch.rank(synchronized(hits) { hits.toList() }, title, durationMs).take(CANDIDATES)) {
            val file = download(hit, tags, onProgress) ?: continue
            if (isWholeSong(file, durationMs)) return file
            file.parentFile?.deleteRecursively()
        }
        return null
    }

    private suspend fun download(hit: SoulseekMatch.Hit, tags: JSONObject, onProgress: (Float) -> Unit): File? = coroutineScope {
        val folder = File(downloadRoot, UUID.randomUUID().toString())
        val key = hit.user + hit.path
        val done = async(start = CoroutineStart.UNDISPATCHED) {
            downloaded.first { File(it).parentFile?.canonicalPath == folder.canonicalPath }
        }
        cmd("download", "user" to hit.user, "path" to hit.path, "size" to hit.size, "folder" to folder.path, "tags" to tags)
        try {
            var lastPos = -1L
            var lastMove = System.currentTimeMillis()
            while (withTimeoutOrNull(1_000) { done.await() } == null) {
                val t = transfers.value[key]
                if (t != null && t.status !in GOING) break
                if (t != null && (t.pos != lastPos || t.status == "Finished")) {   // finished: waiting for the tagger
                    lastPos = t.pos
                    lastMove = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - lastMove > STALL_MS) {
                    break
                }
                if (t != null && t.size > 0) onProgress((t.pos.toFloat() / t.size).coerceIn(0f, 1f))
            }
            if (done.isCompleted) File(done.await()) else null
        } finally {
            val ok = done.isCompleted
            done.cancel()
            cmd("remove", "user" to hit.user, "path" to hit.path)   // also clears a finished one from the core's list
            if (!ok) folder.deleteRecursively()
        }
    }

    /** A real FLAC as long as the song, not a renamed MP3 or a preview (the same check lossless sources get). */
    private fun isWholeSong(file: File, durationMs: Long): Boolean {
        val head = ByteArray(42)
        val read = runCatching { file.inputStream().use { it.read(head) } }.getOrDefault(0)
        val seconds = flacSeconds(head.copyOf(maxOf(read, 0))) ?: return false
        return durationMs <= 0 || seconds < 0 || abs(seconds - durationMs / 1000.0) <= 10
    }

    private fun cmd(name: String, vararg args: Pair<String, Any?>) {
        val json = JSONObject(args.toMap()).toString()
        commands.execute {
            ready.await()
            bridge!!.callAttr("call", name, json)
        }
    }

    /** Python calls this by name (kept in proguard-rules.pro), from the core's thread and the tagger's. */
    class Listener {
        fun onEvent(name: String, json: String) {
            runCatching { handle(name, json) }
        }
    }

    private fun handle(name: String, json: String) {
        when (name) {
            "status" -> JSONObject(json).let {
                _status.value = Status(it.getString("state"), it.getString("user"), if (it.isNull("error")) null else it.getString("error"))
            }
            "results" -> {
                val o = JSONObject(json)
                val files = o.getJSONArray("files")
                results.tryEmit(o.getString("id") to List(files.length()) {
                    val f = files.getJSONObject(it)
                    SoulseekMatch.Hit(
                        o.getString("user"), f.getString("path"), f.getLong("size"), o.getBoolean("slots"),
                        o.getLong("speed"), o.getInt("queue"), f.getInt("secs"), f.getInt("rate"), f.getInt("bits"),
                    )
                })
            }
            "transfers" -> {
                val a = JSONArray(json)
                transfers.value = (0 until a.length()).associate {
                    val t = a.getJSONObject(it)
                    t.getString("key") to Transfer(t.getLong("size"), t.getLong("pos"), t.getString("status"))
                }
            }
            "downloaded" -> downloaded.tryEmit(JSONObject(json).getString("path"))
        }
    }
}
