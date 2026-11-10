package com.verza.data

import kotlin.math.abs

/**
 * Whether two songs are the same recording, from their names and lengths: a file on the phone and a
 * YouTube song, or two YouTube ids for one song (the album track and its video). Used to play a copy
 * the phone already has, and to find a phone file's song on YouTube for its radio.
 */
object SameSong {
    // "(feat. X)", "[with X]", "- Remastered 2011": what one copy names and another leaves out.
    private val EXTRA = Regex("""\s*[(\[](feat\.?|ft\.?|featuring|with|from|remaster(ed)?|\d{4} remaster(ed)?|official|audio|lyrics?)\b[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)
    private val REMASTER = Regex("""\s+-\s+(\d{4}\s+)?remaster(ed)?.*$""", RegexOption.IGNORE_CASE)
    private val FEATURING = Regex("""\s*(,|;|&|\s/\s|\s(feat\.?|ft\.?|featuring|with|x)\s).*$""", RegexOption.IGNORE_CASE)
    private val SPACE = Regex("""[^\p{L}\p{N}]+""")
    private val APOSTROPHE = Regex("""['’]""")

    fun title(t: String): String = t.replace(EXTRA, "").replace(REMASTER, "").replace(APOSTROPHE, "").lowercase().replace(SPACE, " ").trim()

    /** The first artist named, so "Coldplay & Rihanna" and "Coldplay" match. */
    fun artist(a: String): String = mainArtist(a).replace(APOSTROPHE, "").lowercase().replace(SPACE, " ").trim()

    fun mainArtist(a: String): String = a.replace(FEATURING, "").trim().ifEmpty { a.trim() }

    fun key(title: String, artist: String) = title(title) + "\u0000" + artist(artist)

    /** Same names, and when both lengths are known, within 3s (a live take or an edit isn't). */
    fun same(a: Song, b: Song): Boolean =
        key(a.title, a.artist) == key(b.title, b.artist) &&
            (a.durationMs <= 0 || b.durationMs <= 0 || abs(a.durationMs - b.durationMs) <= 3_000)
}
