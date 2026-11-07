package com.verza.player

import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * A lossless source the listener added: a server answering `/manifest.json`, `/search?q=` and
 * `/stream/{id}` with JSON, the format BitChord's addons use, so any of them works here.
 *
 * [url] can carry a secret (self-hosted addons put a token in it). It lives in DataStore, which is
 * excluded from backups, and only its host is ever shown or logged. [params] are the addon's declared
 * setting defaults, sent with every request as the format asks.
 */
@Serializable
data class AddonSource(val name: String, val url: String, val params: Map<String, String> = emptyMap())

@Serializable
data class AddonTrack(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    /** Seconds. An element because one row sending "3:04" must not lose the whole answer. */
    @SerialName("duration") val rawDuration: JsonElement? = null,
    val format: String = "",
    val audioQuality: String = "",
    val streamURL: String? = null,
) {
    val duration: Double? get() = (rawDuration as? JsonPrimitive)?.doubleOrNull
}

@Serializable
internal data class AddonSearch(val tracks: List<AddonTrack>? = null)

@Serializable
internal data class AddonStream(
    val url: String = "",
    val format: String = "",
    val manifest: String? = null,
    val mediaType: String? = null,
    val encrypted: JsonElement? = null,
)

@Serializable
internal data class AddonSetting(val key: String = "", val default: JsonElement? = null, val options: List<JsonElement> = emptyList())

@Serializable
internal data class AddonManifest(
    val id: String = "",
    val name: String = "",
    val resources: List<JsonElement> = emptyList(),
    val settings: List<AddonSetting> = emptyList(),
)

/** Why a link could not be added, worded for the dialog. */
class AddonProblem(message: String) : Exception(message)

/**
 * Asks the listener's sources for the song about to play.
 *
 * Plain blocking calls: the one caller during playback is ExoPlayer's loader thread. A source that
 * stops answering costs one track a few seconds and is then left alone for a minute, so a server
 * that has gone away cannot slow down every song. Anything short of a clean answer falls through to
 * YouTube.
 *
 * ponytail: progressive files only. A DASH or HLS answer counts as a miss, because the playback URI
 * is decided before the source is asked. Add it when a self-hosted addon needs it.
 */
class AddonClient(http: OkHttpClient) {

    private val http = http.newBuilder().callTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS).build()
    private val quietUntil = ConcurrentHashMap<String, Long>()

    /** A URL for this song from the first source that has it, or null. */
    fun find(title: String, artist: String, durationMs: Long): String? {
        for (source in PlayerSettings.addons.value) {
            if (System.currentTimeMillis() < (quietUntil[source.url] ?: 0L)) continue
            try {
                streamFrom(source, title, artist, durationMs)?.let { return it }
            } catch (e: IOException) {
                // A skipped track interrupts this thread. That says nothing about the server.
                if (Thread.currentThread().isInterrupted) throw e
                quietUntil[source.url] = System.currentTimeMillis() + QUIET_MS
            }
        }
        return null
    }

    private fun streamFrom(source: AddonSource, title: String, artist: String, durationMs: Long): String? {
        val base = source.url.toHttpUrlOrNull() ?: return null
        val query = AddonMatch.searchQuery(title, artist)
        if (query.isBlank()) return null
        val rows = get<AddonSearch>(endpoint(base, source.params, listOf("search"), "q" to query))?.tracks ?: return null
        val row = AddonMatch.pick(title, artist, durationMs, rows) ?: return null
        val url = row.streamURL?.takeIf { it.isNotBlank() } ?: streamUrl(base, source, row.id) ?: return null
        val resolved = base.resolve(url) ?: return null
        // The answer goes straight to a data source that also opens file:// and content://, so this
        // is what stops a server naming something on the phone.
        if (!isPlayableUrl(resolved, base)) return null
        return resolved.toString().takeIf { isWholeSong(it, durationMs) }
    }

    private fun streamUrl(base: HttpUrl, source: AddonSource, id: String): String? {
        if (id.isBlank()) return null
        val stream = get<AddonStream>(endpoint(base, source.params, listOf("stream", id))) ?: return null
        if (isEncrypted(stream.encrypted)) return null
        val transport = listOf(stream.manifest, stream.mediaType, stream.format).map { it.orEmpty().lowercase() }
        if (transport.any { it in MANIFEST_WORDS }) return null
        return stream.url.takeIf { it.isNotBlank() }
    }

    /**
     * One small request before the player commits to it: the start of the file, which has to be FLAC
     * and as long as the song. A source can answer with a 30-second preview, or an MP3 under a FLAC name,
     * and both used to play as if they were the song. A miss here plays the whole song from YouTube,
     * where a dead link would have cost a skip.
     *
     * ponytail: FLAC only, which is what the format's sources serve. ALAC in an MP4 would need its
     * length dug out of the moov box; add it when a source sends it.
     */
    private fun isWholeSong(url: String, durationMs: Long): Boolean = try {
        http.newCall(Request.Builder().url(url).header("Range", "bytes=0-41").build()).execute().use { resp ->
            val body = resp.body
            if (!resp.isSuccessful || body == null) {
                false
            } else {
                // A server is free to ignore the Range and send the whole file, so only this much is read.
                val source = body.source()
                source.request(FLAC_HEAD.toLong())
                val head = source.buffer.readByteArray(minOf(FLAC_HEAD.toLong(), source.buffer.size))
                val seconds = flacSeconds(head)
                seconds != null && (seconds < 0 || durationMs <= 0 || abs(seconds * 1000 - durationMs) <= FILE_SLACK_MS)
            }
        }
    } catch (e: IOException) {
        if (Thread.currentThread().isInterrupted) throw e
        false
    }

    /**
     * Checks a pasted link and works out what to store. Blocking.
     *
     * The manifest is only a description, so a server without one is still accepted when its
     * `/search` answers in the right shape.
     */
    fun inspect(input: String): AddonSource {
        val typed = input.trim()
        val parsed = typed.toHttpUrlOrNull() ?: "https://$typed".takeIf { "://" !in typed }?.toHttpUrlOrNull()
            ?: throw AddonProblem("That doesn't look like a link.")
        if (!parsed.isHttps && !isLocal(parsed)) throw AddonProblem("The link has to start with https://.")
        val base = parsed.newBuilder().apply {
            if (parsed.pathSegments.last().equals("manifest.json", ignoreCase = true)) {
                removePathSegment(parsed.pathSegments.lastIndex)
            }
        }.build()

        val manifest = try {
            get<AddonManifest>(endpoint(base, emptyMap(), listOf("manifest.json")))?.takeIf { it.id.isNotBlank() }
        } catch (_: IOException) {
            null
        }
        if (manifest != null) {
            val declared = manifest.resources.mapNotNull { (it as? JsonPrimitive)?.content ?: (it as? JsonObject)?.get("name")?.let { n -> (n as? JsonPrimitive)?.content } }
            if (declared.isNotEmpty() && declared.none { it.equals("search", ignoreCase = true) }) {
                throw AddonProblem("That source can't be searched, so Verza can't use it.")
            }
            return AddonSource(manifest.name.ifBlank { manifest.id }, base.toString(), paramsFrom(manifest))
        }
        val search = try {
            get<AddonSearch>(endpoint(base, mapOf("quality" to LOSSLESS), listOf("search"), "q" to "test"))
        } catch (_: IOException) {
            throw AddonProblem("Couldn't reach a source at that link. Check it and try again.")
        }
        if (search?.tracks == null) throw AddonProblem("That link answered, but it isn't a source.")
        return AddonSource(base.host, base.toString(), mapOf("quality" to LOSSLESS))
    }

    /**
     * The declared defaults, then `quality` set to the addon's own word for lossless, since an
     * unknown value lands on whatever its fallback is.
     */
    private fun paramsFrom(manifest: AddonManifest): Map<String, String> {
        val params = LinkedHashMap<String, String>()
        manifest.settings.forEach { setting ->
            val value = (setting.default as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }
            if (setting.key.isNotBlank() && value != null) params[setting.key] = value
        }
        val options = manifest.settings.firstOrNull { it.key == "quality" }?.options.orEmpty().mapNotNull { option ->
            ((option as? JsonObject)?.get("value") ?: option).let { (it as? JsonPrimitive)?.content }
        }
        params["quality"] = options.firstOrNull { o -> LOSSLESS_WORDS.any { it in o.lowercase() } } ?: options.firstOrNull() ?: LOSSLESS
        return params
    }

    private fun endpoint(base: HttpUrl, params: Map<String, String>, segments: List<String>, vararg extra: Pair<String, String>): HttpUrl =
        base.newBuilder().apply {
            segments.forEach(::addPathSegment)
            params.forEach { (k, v) -> setQueryParameter(k, v) }
            extra.forEach { (k, v) -> setQueryParameter(k, v) }
        }.build()

    /** The parsed answer, or null for a 404, which is how a source says it hasn't got that. */
    private inline fun <reified T> get(url: HttpUrl): T? =
        http.newCall(Request.Builder().url(url).header("Accept", "application/json").build()).execute().use { resp ->
            if (resp.code == 404) return null
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            try {
                JSON.decodeFromString<T>(resp.body!!.string())
            } catch (e: SerializationException) {
                throw IOException("Not a source's answer", e)
            }
        }

    private fun isEncrypted(element: JsonElement?): Boolean {
        val value = element as? JsonPrimitive ?: return false
        value.booleanOrNull?.let { return it }
        return value.content.isNotBlank() && value.content.lowercase() !in setOf("false", "none", "null")
    }

    private companion object {
        const val CALL_TIMEOUT_MS = 4_000L
        const val FLAC_HEAD = 42
        // Wider than a search row's 3s: a remaster's file can run a few seconds off YouTube's cut.
        const val FILE_SLACK_MS = 10_000L
        const val QUIET_MS = 60_000L
        const val LOSSLESS = "LOSSLESS"
        val LOSSLESS_WORDS = listOf("lossless", "flac", "hi-res", "hires", "hi_res")
        val MANIFEST_WORDS = setOf("hls", "m3u8", "dash", "mpd", "application/x-mpegurl", "application/vnd.apple.mpegurl", "application/dash+xml")
        val JSON = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false; coerceInputValues = true }

        fun isLocal(url: HttpUrl) = url.host == "localhost" || url.host == "127.0.0.1"

        /** https, or plain http between a source on this phone and a file on this phone. */
        fun isPlayableUrl(url: HttpUrl, source: HttpUrl) = url.isHttps || (isLocal(url) && isLocal(source))
    }
}

/**
 * Decides whether a source's row is the recording that was asked for.
 *
 * Deliberately strict. Failing to match costs lossless on one song; matching wrongly plays a live
 * take or a remix under the title the listener picked, with nothing to tell them. So any word in a
 * title's brackets that isn't known to be harmless counts as a different recording, rather than
 * keeping a list of the words that mean different ("Live", "Remix", "Taylor's Version"...).
 */
object AddonMatch {

    private const val DURATION_SLACK_MS = 3_000L
    private val BRACKETS = Regex("""[(\[{（【]([^)\]}）】]*)[)\]}）】]""")
    private val NOT_A_LETTER = Regex("""[^\p{L}\p{N}]+""")
    private val ACCENTS = Regex("""\p{Mn}+""")
    private val FEATURING = Regex("""\s(feat\.?|ft\.?|featuring)\s.*$""", RegexOption.IGNORE_CASE)
    private val ARTIST_SPLIT = Regex("""\s*(,|;|/|&|\+|\bx\b|\bfeat\.?|\bft\.?|\bfeaturing\b|\bwith\b)\s*""", RegexOption.IGNORE_CASE)
    private val CHANNEL_SUFFIX = Regex("""(\s-\sTopic|VEVO)$""", RegexOption.IGNORE_CASE)
    private val LOSSLESS_HINT = Regex("""flac|alac|lossless|hi[-_ ]?res""", RegexOption.IGNORE_CASE)
    /** A bracket opening with one of these is about the release, whatever follows it. */
    private val IGNORED_OPENERS = setOf("feat", "ft", "featuring", "with", "from", "prod", "produced")
    private val HARMLESS = setOf(
        "official", "video", "audio", "lyric", "lyrics", "music", "visualizer", "visualiser", "hd", "hq", "4k",
        "explicit", "clean", "remaster", "remastered", "album", "version", "single", "stereo", "mono", "original",
        "deluxe", "edition", "bonus", "track", "mv", "digital",
    )

    /** What to ask a source for: the plain title and the main artist. */
    fun searchQuery(title: String, artist: String): String {
        val core = split(title, artist).first
        val main = artist.replace(CHANNEL_SUFFIX, "").split(ARTIST_SPLIT).firstOrNull().orEmpty().trim()
        return listOf(core, main).filter { it.isNotBlank() }.joinToString(" ")
    }

    /** The row for this song, lossless ones first, then the closest in length. Null if none is. */
    fun pick(title: String, artist: String, durationMs: Long, rows: List<AddonTrack>): AddonTrack? =
        rows.filter { matches(title, artist, durationMs, it) }
            .minWithOrNull(
                compareBy<AddonTrack> { if (LOSSLESS_HINT.containsMatchIn("${it.format} ${it.audioQuality}")) 0 else 1 }
                    .thenBy { row -> row.duration?.let { abs(it * 1000 - durationMs) } ?: Double.MAX_VALUE },
            )

    fun matches(title: String, artist: String, durationMs: Long, row: AddonTrack): Boolean {
        val (core, extras) = split(title, artist)
        val (rowCore, rowExtras) = split(row.title, row.artist)
        if (core.isBlank() || norm(core) != norm(rowCore)) return false
        if (versionWords(extras) != versionWords(rowExtras)) return false
        if (!sameArtist(artist, row.artist)) return false
        val rowMs = row.duration?.takeIf { it > 0 }?.let { (it * 1000).toLong() }
        return durationMs <= 0 || rowMs == null || abs(rowMs - durationMs) <= DURATION_SLACK_MS
    }

    /** The title proper, and what's in its brackets or after its " - ". */
    private fun split(title: String, artist: String): Pair<String, List<String>> {
        val extras = BRACKETS.findAll(title).map { it.groupValues[1] }.toMutableList()
        var core = BRACKETS.replace(title, " ").replace(FEATURING, "").trim()
        val dash = core.indexOf(" - ")
        if (dash > 0) {
            val before = core.substring(0, dash)
            val after = core.substring(dash + 3)
            // Videos are often titled "Artist - Song". Otherwise it's "Song - Live at Wembley".
            if (sameArtist(before, artist)) core = after else { core = before; extras += after }
        }
        return (core.ifBlank { title }).trim() to extras
    }

    private fun versionWords(extras: List<String>): Set<String> =
        extras.flatMap { extra ->
            val words = norm(extra).split(' ').filter { it.isNotBlank() }
            if (words.firstOrNull() in IGNORED_OPENERS) emptyList() else words
        }.filterNot { it in HARMLESS || it.all(Char::isDigit) }.toSet()

    private fun sameArtist(a: String, b: String): Boolean {
        val left = a.replace(CHANNEL_SUFFIX, "")
        val right = b.replace(CHANNEL_SUFFIX, "")
        if (norm(left).isBlank() || norm(right).isBlank()) return false
        if (norm(left) == norm(right)) return true
        return names(left).intersect(names(right)).isNotEmpty()
    }

    private fun names(artist: String): Set<String> =
        artist.split(ARTIST_SPLIT).map(::norm).filter { it.isNotBlank() }.toSet()

    private fun norm(text: String): String =
        ACCENTS.replace(Normalizer.normalize(text, Normalizer.Form.NFKD), "")
            .replace("&", " and ").replace("+", " and ")
            .lowercase().replace(NOT_A_LETTER, " ").trim()
}

/**
 * How long a FLAC file is, from its first 42 bytes; null if they aren't the start of one, and -1 if it
 * doesn't say. STREAMINFO is always the first block: sample rate, then the total number of samples.
 */
fun flacSeconds(head: ByteArray): Double? {
    if (head.size < 4 || String(head, 0, 4, Charsets.ISO_8859_1) != "fLaC") return null
    if (head.size < 26 || (head[4].toInt() and 0x7f) != 0) return -1.0
    fun b(i: Int) = head[i].toInt() and 0xff
    val rate = (b(18) shl 12) or (b(19) shl 4) or (b(20) shr 4)
    val samples = ((b(21) and 0xf).toLong() shl 32) or (b(22).toLong() shl 24) or (b(23).toLong() shl 16) or
        (b(24).toLong() shl 8) or b(25).toLong()
    return if (rate == 0 || samples == 0L) -1.0 else samples.toDouble() / rate
}

/**
 * The badge for a track, from the format the player actually opened rather than a source's label, or
 * null for anything lossy. A lossy file re-wrapped as FLAC still gets it: that can't be seen from here.
 * Uncompressed audio (a WAV or AIFF on the phone) arrives as raw PCM, and is named by its container.
 */
@androidx.annotation.OptIn(UnstableApi::class)
fun losslessLabel(mimeType: String?, pcmEncoding: Int, sampleRateHz: Int, containerMimeType: String? = null): String? {
    val codec = when (mimeType) {
        MimeTypes.AUDIO_FLAC -> "FLAC"
        MimeTypes.AUDIO_ALAC -> "ALAC"
        MimeTypes.AUDIO_RAW -> when (containerMimeType) {
            MimeTypes.AUDIO_WAV, "audio/x-wav", "audio/wave" -> "WAV"
            "audio/aiff", "audio/x-aiff" -> "AIFF"
            else -> "PCM"
        }
        else -> return null
    }
    val bits = when (pcmEncoding) {
        C.ENCODING_PCM_16BIT -> "16-bit"
        C.ENCODING_PCM_24BIT -> "24-bit"
        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> "32-bit"
        else -> null
    }
    val rate = sampleRateHz.takeIf { it > 0 }?.let {
        val tenths = it / 100
        if (tenths % 10 == 0) "${tenths / 10} kHz" else "${tenths / 10}.${tenths % 10} kHz"
    }
    val detail = listOfNotNull(bits, rate).joinToString(" / ")
    return if (detail.isEmpty()) codec else "$codec · $detail"
}
