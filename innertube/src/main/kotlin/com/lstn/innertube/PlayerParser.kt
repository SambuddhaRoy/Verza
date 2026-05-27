package com.lstn.innertube

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
