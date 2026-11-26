package com.verza.data.mixes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Spotify's own picks for you (Discover Weekly, Release Radar, the Daily Mixes, or any playlist), from
 * a link to it, kept fresh on Home and played from YouTube Music.
 *
 * No Spotify account or API: the Web API hasn't given new apps Spotify's own playlists since November
 * 2024, and its developer policy forbids playing its data through another service. A playlist's
 * embed page (open.spotify.com/embed/playlist/<id>, what a blog shows) carries its name and songs to
 * anyone with the link, including a listener's personal mixes, which are shared by link like any
 * playlist. It stops at 100 songs; Discover Weekly has 30 and a Daily Mix 50. The desktop's "open
 * link" reads Spotify the same way (src/main/links.js there).
 */
class SpotifyMixes(private val client: OkHttpClient) {
    data class Playlist(val name: String, val pairs: List<Pair<String, String>>)

    /** The playlist at [id], or null when Spotify says there is none. Throws when it can't be reached. */
    suspend fun fetch(id: String): Playlist? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("https://open.spotify.com/embed/playlist/$id").header("User-Agent", UA).build()
        client.newCall(req).execute().use { res ->
            if (res.code == 404) return@withContext null
            if (!res.isSuccessful) throw IOException("Spotify ${res.code}")
            parse(res.body?.string().orEmpty())
        }
    }

    companion object {
        private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36"
        private val json = Json { ignoreUnknownKeys = true }
        private val ID = Regex("""playlist(?:/|:)([A-Za-z0-9]{22})""")
        private val DATA = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)

        /** The playlist's id in a link: open.spotify.com/playlist/<id>?si=…, /intl-de/playlist/<id>, spotify:playlist:<id>. */
        fun idOf(link: String): String? = ID.find(link)?.groupValues?.get(1)

        /** An embed page's playlist: its name, and each song as (title, first artist). */
        fun parse(html: String): Playlist? {
            val data = DATA.find(html)?.groupValues?.get(1) ?: return null
            val entity = runCatching { json.parseToJsonElement(data) as JsonObject }.getOrNull()
                ?.path("props", "pageProps", "state", "data", "entity") ?: return null
            val name = entity.str("name") ?: entity.str("title") ?: return null
            val pairs = (entity["trackList"] as? JsonArray).orEmpty().mapNotNull { t ->
                val o = t as? JsonObject ?: return@mapNotNull null
                val title = o.str("title")?.trim().orEmpty()
                // "Disclosure,\u00a0Sasha Keable": the first artist is the one to search by.
                val artist = o.str("subtitle")?.split(',')?.firstOrNull()?.replace('\u00a0', ' ')?.trim().orEmpty()
                if (title.isEmpty() || artist.isEmpty()) null else title to artist
            }
            return Playlist(name, pairs)
        }

        private fun JsonObject.path(vararg keys: String): JsonObject? = keys.fold(this as JsonObject?) { o, k -> o?.get(k) as? JsonObject }
        private fun JsonObject.str(k: String) = (this[k] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
    }
}
