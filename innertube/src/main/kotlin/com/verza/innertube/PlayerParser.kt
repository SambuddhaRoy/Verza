package com.verza.innertube

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

// ── Player response parsing ────────────────────────────────────────────────────
//
// The ANDROID_MUSIC client returns streaming formats under streamingData. Audio-only
// adaptive formats expose a direct `url` (no signature cipher), so we pick the highest
// bitrate audio track and hand its URL straight to ExoPlayer.

/** A resolved audio stream plus the metadata ExoPlayer cares about. */
data class StreamInfo(
    val url: String,
    val mimeType: String,
    val bitrate: Int,
    val contentLength: Long?,
)

internal fun parseAudioStream(response: JsonElement): StreamInfo? {
    val streamingData = response.child("streamingData").obj ?: return null
    val adaptive = streamingData["adaptiveFormats"] as? JsonArray ?: return null

    val best = adaptive
        .filterIsInstance<JsonObject>()
        .filter { (it["mimeType"].string ?: "").startsWith("audio/") }
        .filter { it["url"].string != null } // skip cipher-only formats we can't decode
        .maxByOrNull { it["bitrate"].asInt() }
        ?: return null

    val url = best["url"].string ?: return null
    return StreamInfo(
        url = url,
        mimeType = best["mimeType"].string ?: "audio/webm",
        bitrate = best["bitrate"].asInt(),
        contentLength = best["contentLength"].string?.toLongOrNull(),
    )
}

private fun JsonElement?.asInt(): Int =
    (this as? JsonPrimitive)?.content?.toIntOrNull() ?: 0

/** A page on YouTube Music: its name, and the browse id that opens it. */
data class PageLink(val name: String, val id: String)

/** Where a song's artists and album live on YouTube Music, for going to their pages. */
data class TrackLinks(
    val artists: List<PageLink>,
    val album: PageLink?,
)

/** The artist and album pages among a song's byline runs: "Joji • Nectar • 2020". */
private fun bylineLinks(runs: List<JsonElement>): TrackLinks {
    val artists = ArrayList<PageLink>()
    var album: PageLink? = null
    for (run in runs) {
        val page = run.child("navigationEndpoint").child("browseEndpoint")
        val id = page.child("browseId").string ?: continue
        val name = run.child("text").string ?: continue
        when (page?.findFirst("pageType").string) {
            // A video's uploader is a plain channel. It still has a page, and the artist screen opens it.
            "MUSIC_PAGE_TYPE_ARTIST", "MUSIC_PAGE_TYPE_USER_CHANNEL" -> artists += PageLink(name, id)
            "MUSIC_PAGE_TYPE_ALBUM" -> if (album == null) album = PageLink(name, id)
        }
    }
    return TrackLinks(artists, album)
}

/** The song's title and links from its line in the `next` queue. */
internal fun parseTrackLinks(response: JsonElement, videoId: String): Pair<String?, TrackLinks>? {
    val row = response.findAll("playlistPanelVideoRenderer").firstOrNull { it["videoId"].string == videoId } ?: return null
    val title = row.child("title").child("runs").array?.firstOrNull()?.child("text").string
    return title to bylineLinks(row.child("longBylineText").child("runs").array.orEmpty())
}

/**
 * The links of the song a music video is of, from a song search. A video has no album of its own.
 * Only the same title by one of the same artists counts: anything looser hands a cover or a tribute
 * its album. "Artist - Song (Official Video)" won't match, and goes without.
 */
internal fun parseSongMatch(response: JsonElement, title: String, artists: List<String>): TrackLinks? {
    val wanted = artists.map { it.trim().lowercase() }
    return response.findAll("musicResponsiveListItemRenderer").firstNotNullOfOrNull { row ->
        val columns = row["flexColumns"].array ?: return@firstNotNullOfOrNull null
        val name = columns.getOrNull(0)?.findFirst("runs")?.array?.firstOrNull()?.child("text").string
        val links = bylineLinks(columns.getOrNull(1)?.findFirst("runs")?.array.orEmpty())
        links.takeIf { name.equals(title.trim(), ignoreCase = true) && links.album != null && it.artists.any { a -> a.name.lowercase() in wanted } }
    }
}
