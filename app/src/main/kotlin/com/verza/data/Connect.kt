package com.verza.data

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.verza.playback.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Verza Connect: Verza on a computer (Verza Next, connect.js there) shows what it's playing here,
 * takes play, pause, skip and seek from here, and a queue is handed over in either direction, the
 * way Spotify Connect does it, with no server in between.
 *
 * A computer with Connect on says so with a UDP broadcast every few seconds ([listen]), or is added
 * by its address. Pairing is the six-digit code it shows; what comes back is a token, kept with the
 * computer in settings (backups are off). While the app is on screen each paired computer is
 * watched over one open request ([watch]), which is also how it hands its queue to this phone.
 *
 * Plain sockets, not the shared OkHttp client: this is http to an address on the local network,
 * which the network security config refuses everywhere else and should go on refusing.
 * Everything a computer sends is checked like a session link ([Sessions]): ids must be YouTube ids.
 */
class Connect(
    private val context: Context, private val settings: Settings, private val scope: CoroutineScope, private val player: Player,
    private val backup: com.verza.data.library.Backup,
) {

    @Serializable
    data class Computer(val id: String, val name: String, val host: String, val port: Int = PORT, val token: String = "")

    @Serializable
    data class Track(val id: String = "", val title: String = "", val artist: String = "", val art: String? = null)

    /** What a computer is playing. [at] is its clock when [positionMs] was true; [seen] is ours. */
    @Serializable
    data class Playing(
        val playing: Boolean = false, val positionMs: Long = 0, val durationMs: Long = 0,
        val track: Track? = null, val index: Int = 0, val queue: List<Track> = emptyList(),
        @kotlinx.serialization.Transient val seen: Long = android.os.SystemClock.elapsedRealtime(),
    ) {
        /** Where it is now, carried forward from when it was last told. */
        fun position(): Long =
            if (!playing) positionMs else (positionMs + android.os.SystemClock.elapsedRealtime() - seen).coerceAtMost(durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE)
    }

    @Serializable private data class Told(val name: String = "", val state: Playing? = null)
    @Serializable private data class Shelf(val name: String = "", val tracks: List<Track> = emptyList())
    @Serializable private data class Library(val playlists: List<Shelf> = emptyList(), val liked: List<Track> = emptyList())
    @Serializable private data class Beacon(val verza: Int = 0, val id: String = "", val name: String = "", val port: Int = PORT)
    @Serializable private data class Paired(val token: String = "", val id: String = "", val name: String = "", val error: String? = null)

    val paired = JsonSetting(context, "connect_computers", ListSerializer(Computer.serializer()), emptyList())

    private val _found = MutableStateFlow<List<Computer>>(emptyList())
    /** Computers heard on this network just now, paired or not. */
    val found: StateFlow<List<Computer>> = _found.asStateFlow()

    private val _playing = MutableStateFlow<Map<String, Playing?>>(emptyMap())
    /** By computer id: what each paired computer in reach is playing (null for nothing). Absent when out of reach. */
    val playing: StateFlow<Map<String, Playing?>> = _playing.asStateFlow()

    private var watching: Job? = null
    private var listening: Job? = null
    private var warming: Job? = null

    /** The app is on screen: watch every paired computer, and listen for ones on this network. */
    fun start() {
        if (watching?.isActive == true) return
        watching = scope.launch(Dispatchers.IO) {
            // One watcher per computer, started again when the list changes
            paired.flow.collectLatest { list ->
                _playing.update { now -> now.filterKeys { id -> list.any { it.id == id } } }
                coroutineScope {
                    for (c in list) launch {
                        while (isActive) {
                            // A blocked read only ends when its socket is closed, so cancelling closes it
                            val socket = Socket()
                            val closing = coroutineContext[Job]?.invokeOnCompletion { runCatching { socket.close() } }
                            runCatching { watch(c, socket) }
                            closing?.dispose()
                            _playing.update { it - c.id }
                            delay(RETRY_MS)
                        }
                    }
                }
            }
        }
        listening = scope.launch(Dispatchers.IO) { runCatching { listen() } }
        // What this phone starts, and what's next, goes ahead to each computer in reach, which loads it
        // then: a handover finds it there instead of starting from nothing.
        warming = scope.launch(Dispatchers.IO) {
            val songs = player.state.map { s -> s.queue.drop(s.index).take(2).filter { isVideoId(it.id) } }.distinctUntilChanged()
            combine(songs, _playing.map { it.keys }.distinctUntilChanged()) { a, b -> a to b }.collect { (ahead, inReach) ->
                if (ahead.isNotEmpty()) for (c in paired.get()) if (c.id in inReach) send(c, buildJsonObject {
                    put("action", "warm")
                    put("tracks", json.encodeToJsonElement(ListSerializer(SharedTrack.serializer()), ahead.map { SharedTrack(it.id, it.title, it.artist, it.art) }))
                })
            }
        }
    }

    fun stop() {
        watching?.cancel(); listening?.cancel(); warming?.cancel()
        _playing.value = emptyMap(); _found.value = emptyList()
    }

    /** Hears the computers' broadcasts. Wi-Fi drops broadcasts for a sleeping app unless asked not to. */
    private suspend fun listen() {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val lock = wifi.createMulticastLock("verza-connect").apply { setReferenceCounted(false); acquire() }
        val seen = HashMap<String, Pair<Computer, Long>>()
        try {
            DatagramSocket(null).use { socket ->
                socket.reuseAddress = true
                socket.soTimeout = 2_000
                socket.bind(InetSocketAddress(BEACON_PORT))
                val buf = ByteArray(1024)
                while (scope.isActive && listening?.isActive == true) {
                    val packet = DatagramPacket(buf, buf.size)
                    val now = System.currentTimeMillis()
                    runCatching {
                        socket.receive(packet)
                        val b = json.decodeFromString(Beacon.serializer(), String(packet.data, 0, packet.length))
                        if (b.verza == 1 && b.id.isNotEmpty()) seen[b.id] = Computer(b.id, b.name.take(60), packet.address.hostAddress.orEmpty(), b.port) to now
                    }
                    seen.values.removeAll { now - it.second > 10_000 }
                    _found.value = seen.values.map { it.first }.sortedBy { it.name }
                }
            }
        } finally { lock.release() }
    }

    /** Adds the computer at [host] with the [code] it's showing. The message is for the listener. */
    suspend fun pair(host: String, port: Int, code: String): Result<Computer> = withContext(Dispatchers.IO) {
        runCatching {
            val body = buildJsonObject {
                put("code", code.filter { it.isDigit() }); put("name", Build.MODEL ?: "Phone"); put("id", deviceId())
            }
            val answer = json.decodeFromString(Paired.serializer(), request(host, port, "POST", "/connect/pair", null, body).second)
            if (answer.token.isEmpty()) error(answer.error ?: "Couldn't add that computer")
            val computer = Computer(answer.id.ifEmpty { "$host:$port" }, answer.name.ifEmpty { host }, host, port, answer.token)
            paired.set(paired.get().filter { it.id != computer.id } + computer)
            computer
        }.recoverCatching { e ->
            throw if (e is java.io.IOException) IllegalStateException("Couldn't reach it. Is Verza Connect on, and this phone on the same Wi-Fi?") else e
        }
    }

    suspend fun forget(computer: Computer) = paired.set(paired.get().filter { it.id != computer.id })

    /** play, pause, toggle, next, prev. */
    fun tell(computer: Computer, action: String) = send(computer, buildJsonObject { put("action", action) })
    fun seek(computer: Computer, ms: Long) = send(computer, buildJsonObject { put("action", "seek"); put("positionMs", ms) })

    /** Hands this phone's queue to [computer], from where it is, and stops here. False with nothing it can play. */
    fun playThere(computer: Computer, via: String? = null): Boolean {
        val now = player.state.value
        val session = Sessions.session(now.queue, now.index, player.position, MAX_TRACKS) ?: return false
        send(computer, buildJsonObject {
            put("action", "playSession"); put("session", json.encodeToJsonElement(SharedSession.serializer(), session))
            // The computer greets a flicked queue with the other half of the animation.
            if (via != null) put("via", via)
        })
        if (now.playing) player.toggle()
        return true
    }

    /** Takes what [computer] is playing, from where it is, and stops it there. */
    fun playHere(computer: Computer, via: String? = null): Boolean {
        val there = _playing.value[computer.id] ?: return false
        if (!take(there.queue, there.index, there.position())) return false
        // "flick": the computer sends its cover off toward the listener as it stops.
        send(computer, buildJsonObject { put("action", "pause"); if (via != null) put("via", via) })
        return true
    }

    private fun take(queue: List<Track>, index: Int, positionMs: Long): Boolean {
        val songs = queue.withIndex().filter { isVideoId(it.value.id) }.take(MAX_TRACKS)
        if (songs.isEmpty()) return false
        val at = songs.indexOfLast { it.index <= index }.coerceAtLeast(0)
        player.play(songs.map { (_, t) -> Song(t.id, t.title.take(300), t.artist.take(300), art = t.art?.takeIf { it.startsWith("https://") }) }, at, if (songs[at].index == index) positionMs.coerceAtLeast(0) else 0)
        return true
    }

    private fun send(computer: Computer, body: JsonObject) {
        scope.launch(Dispatchers.IO) { runCatching { request(computer.host, computer.port, "POST", "/connect/command", computer.token, body) } }
    }

    /** One open request to [computer]: what it's playing, each time that changes, and a queue it hands over. */
    private fun watch(computer: Computer, socket: Socket) {
        socket.use {
            socket.connect(InetSocketAddress(computer.host, computer.port), 4_000)
            socket.soTimeout = 0
            socket.getOutputStream().apply {
                write("GET /connect/events HTTP/1.1\r\nHost: ${computer.host}\r\nAuthorization: Bearer ${computer.token}\r\nAccept: text/event-stream\r\n\r\n".toByteArray())
                flush()
            }
            val lines = socket.getInputStream().bufferedReader()
            val status = lines.readLine().orEmpty()
            if (" 401" in status) { scope.launch { forget(computer) }; return }   // forgotten on the computer
            if (" 200" !in status) return
            // In reach and paired: the libraries meet, at most every ten minutes.
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - (synced[computer.id] ?: -SYNC_MS) >= SYNC_MS) { synced[computer.id] = now; scope.launch(Dispatchers.IO) { runCatching { syncLibrary(computer) } } }
            var event = ""
            while (true) {
                val line = lines.readLine() ?: return
                when {
                    line.startsWith("event:") -> event = line.substringAfter(':').trim()
                    line.startsWith("data:") -> runCatching {
                        val data = line.substringAfter(':').trim()
                        when (event) {
                            "state" -> json.decodeFromString(Told.serializer(), data).state.let { s -> _playing.update { it + (computer.id to s) } }
                            "takeover" -> json.decodeFromString(SharedSession.serializer(), data).let { s ->
                                take(s.tracks.map { Track(it.id, it.title, it.artist, it.art) }, s.index, s.positionMs)
                            }
                        }
                    }
                }
            }
        }
    }

    private val synced = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /**
     * The library both ways with [computer] (Verza Next 1.16 and up): its playlists and liked songs are
     * merged in here (`Backup.import`: by name, taking nothing away), then this phone's go there and are
     * merged the same way. YouTube songs only, each way: a file means nothing on the other device. What
     * arrived is said in a toast. A computer too old to know the route answers 404, and nothing happens.
     */
    suspend fun syncLibrary(computer: Computer) {
        val (status, body) = request(computer.host, computer.port, "GET", "/connect/library", computer.token, null)
        if (status != 200) return
        val theirs = json.decodeFromString(Library.serializer(), body)
        val now = System.currentTimeMillis()
        fun song(t: Track, liked: Boolean, i: Int) = com.verza.data.library.BackupSong(
            t.id, t.title.take(300).ifBlank { "Unknown" }, t.artist.take(300), t.art?.takeIf { it.startsWith("https://") },
            liked = liked, likedAt = if (liked) now - i else null,
        )
        val liked = theirs.liked.filter { isVideoId(it.id) }.take(MAX_LIKED)
        val lists = theirs.playlists.filter { it.name.isNotBlank() }.take(MAX_LISTS)
            .map { it.name.trim().take(120) to it.tracks.filter { t -> isVideoId(t.id) }.take(MAX_LIST) }
        val got = backup.import(com.verza.data.library.LibraryBackup(
            exportedAt = now,
            songs = (liked.mapIndexed { i, t -> song(t, true, i) } + lists.flatMap { (_, ts) -> ts.map { song(it, false, 0) } }).distinctBy { it.id },
            playlists = lists.filter { it.second.isNotEmpty() }.map { (name, ts) -> com.verza.data.library.BackupPlaylist(name, now, ts.map { it.id }) },
        ))

        val mine = backup.snapshot(plays = false)
        val byId = mine.songs.filter { isVideoId(it.id) }.associateBy { it.id }
        fun out(id: String) = byId[id]?.let { Track(it.id, it.title, it.artist, it.thumbnailUrl?.takeIf { a -> a.startsWith("https://") }) }
        val sent = Library(
            playlists = mine.playlists.map { Shelf(it.name, it.trackIds.mapNotNull(::out)) }.filter { it.tracks.isNotEmpty() },
            liked = mine.songs.filter { it.liked }.sortedByDescending { it.likedAt ?: 0 }.mapNotNull { out(it.id) },
        )
        request(computer.host, computer.port, "POST", "/connect/library", computer.token, json.encodeToJsonElement(Library.serializer(), sent) as JsonObject)

        val bits = listOfNotNull(
            got.playlists.takeIf { it > 0 }?.let { if (it == 1) "1 playlist" else "$it playlists" },
            got.inPlaylists.takeIf { it > 0 && got.playlists == 0 }?.let { if (it == 1) "1 song" else "$it songs" },
        )
        if (bits.isNotEmpty()) withContext(Dispatchers.Main) {
            android.widget.Toast.makeText(context, "From ${computer.name}: ${bits.joinToString(", ")}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /** One small request; the answer's status and body. The computer always sends a length and closes. */
    private fun request(host: String, port: Int, method: String, path: String, token: String?, body: JsonObject?): Pair<Int, String> =
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 4_000)
            socket.soTimeout = 8_000
            val bytes = body?.toString()?.toByteArray() ?: ByteArray(0)
            val head = buildString {
                append("$method $path HTTP/1.1\r\nHost: $host\r\nConnection: close\r\n")
                if (token != null) append("Authorization: Bearer $token\r\n")
                if (body != null) append("Content-Type: application/json\r\nContent-Length: ${bytes.size}\r\n")
                append("\r\n")
            }
            socket.getOutputStream().apply { write(head.toByteArray()); write(bytes); flush() }
            val answer = socket.getInputStream().readBytes().decodeToString()
            val status = answer.substringBefore("\r\n").split(' ').getOrNull(1)?.toIntOrNull() ?: 0
            status to answer.substringAfter("\r\n\r\n", "")
        }

    /** This phone, the same each time, so adding it twice replaces it on the computer. */
    private suspend fun deviceId(): String {
        val saved = settings.connectId.get()
        if (saved.isNotEmpty()) return saved
        return java.util.UUID.randomUUID().toString().also { settings.connectId.set(it) }
    }

    companion object {
        const val PORT = 47126
        private const val BEACON_PORT = 47127
        private const val RETRY_MS = 5_000L
        private const val MAX_TRACKS = 500   // the whole playlist moves, not its first fifty
        private const val SYNC_MS = 10 * 60_000L
        // The computer's own limits on what it takes (connect.js): a library past them is cut, not refused.
        private const val MAX_LISTS = 300
        private const val MAX_LIST = 5000
        private const val MAX_LIKED = 10000
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    }
}
