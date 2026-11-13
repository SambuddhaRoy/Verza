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
class Connect(private val context: Context, private val settings: Settings, private val scope: CoroutineScope, private val player: Player) {

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
    }

    fun stop() {
        watching?.cancel(); listening?.cancel()
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
    fun playThere(computer: Computer): Boolean {
        val now = player.state.value
        val session = Sessions.session(now.queue, now.index, player.position) ?: return false
        send(computer, buildJsonObject { put("action", "playSession"); put("session", json.encodeToJsonElement(SharedSession.serializer(), session)) })
        if (now.playing) player.toggle()
        return true
    }

    /** Takes what [computer] is playing, from where it is, and stops it there. */
    fun playHere(computer: Computer): Boolean {
        val there = _playing.value[computer.id] ?: return false
        if (!take(there.queue, there.index, there.position())) return false
        tell(computer, "pause")
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
        private const val MAX_TRACKS = 50
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    }
}
