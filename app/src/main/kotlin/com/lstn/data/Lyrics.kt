package com.lstn.data

import kotlinx.serialization.Serializable

/** Raw response from the LRCLIB `/api/get` endpoint. */
@Serializable
data class LyricsResult(
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)

/** One line of synced lyrics — the timestamp it should start being highlighted, and its text. */
data class LyricLine(val timeMs: Long, val text: String)

/**
 * Parses an LRC body ("[mm:ss.xx] line text") into a sorted list of [LyricLine]s.
 * Bare text lines (no timestamp) are dropped — the caller falls back to the plain lyrics for those.
 */
fun parseLrc(synced: String): List<LyricLine> {
    val regex = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?](.*)""")
    return synced.lines().mapNotNull { line ->
        val m = regex.matchEntire(line.trim()) ?: return@mapNotNull null
        val mm = m.groupValues[1].toLongOrNull() ?: return@mapNotNull null
        val ss = m.groupValues[2].toLongOrNull() ?: return@mapNotNull null
        val ms = m.groupValues[3].takeIf { it.isNotEmpty() }
            ?.padEnd(3, '0')?.take(3)?.toLongOrNull() ?: 0L
        val text = m.groupValues[4].trim()
        LyricLine(timeMs = mm * 60_000 + ss * 1000 + ms, text = text)
    }.sortedBy { it.timeMs }
}
