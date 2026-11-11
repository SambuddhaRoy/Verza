package com.verza.data.downloads

import kotlin.math.abs

/**
 * Picks which Soulseek file to download for a YouTube Music song. Pure, so [SoulseekMatchTest] can pin it.
 *
 * bridge.py already drops anything that isn't a .flac or doesn't contain every word of the search.
 * This is the rest: the right recording, then the peer most likely to send it soon.
 */
object SoulseekMatch {

    data class Hit(
        val user: String,
        val path: String,
        val size: Long,
        val slots: Boolean,
        val speed: Long,
        val queue: Int,
        val secs: Int,
        val rate: Int,
        val bits: Int,
    )

    /** A file several seconds off is a different master or edit, not this song. */
    private const val TOLERANCE_S = 3

    // A file whose name says one of these is a different recording, unless the song's title says it too
    private val VARIANTS = setOf(
        "live", "remix", "mix", "edit", "instrumental", "karaoke", "acapella", "acoustic", "demo", "cover", "rehearsal",
    )
    private val WORD = Regex("""[\p{L}\p{N}]+""")
    private val BRACKETS = Regex("""\s*[(\[][^)\]]*[)\]]""")
    private val FEAT = Regex("""\s+(feat\.?|ft\.?|featuring)\s.*$""", RegexOption.IGNORE_CASE)
    private val SPACES = Regex("""\s+""")
    private const val FAKE_RATIO = 0.7

    /** The first credited artist and the bare title: "Get Lucky (feat. Pharrell...)" searches as "Daft Punk Get Lucky". */
    fun query(artist: String, title: String): String =
        (words(firstArtist(artist)) + words(plainTitle(title))).joinToString(" ")

    /** The files worth trying for this song, best first. Empty when nothing is the same recording. */
    fun rank(hits: List<Hit>, title: String, durationMs: Long): List<Hit> {
        // A title of symbols alone ("$$$") has no words to match by, so its symbols are what's matched
        val symbols = squash(plainTitle(title)).isEmpty()
        val key = { s: String -> if (symbols) s.lowercase().replace(SPACES, "") else squash(s) }
        val want = key(plainTitle(title))
        val titleWords = words(title).toSet()
        val seconds = durationMs / 1000
        return hits.filter { hit ->
            val name = hit.path.substringAfterLast('\\').substringBeforeLast('.')
            val nameWords = words(name)
            want.isNotEmpty() && want in key(name) &&
                nameWords.none { it in VARIANTS && it !in titleWords } &&
                // Without a length in the result a 30-second preview looks the same as the song
                (seconds <= 0 || (hit.secs > 0 && abs(hit.secs - seconds) <= TOLERANCE_S))
        }.let(::dropFakes).sortedWith(
            // ponytail: CD quality over hi-res, it's a third of the size and sounds the same on a phone
            compareBy<Hit>({ !it.slots }, { it.queue }, { quality(it) }, { -it.speed }),
        )
    }

    private fun kbps(hit: Hit) = if (hit.secs > 0) hit.size * 8.0 / hit.secs / 1000 else 0.0

    /**
     * Drops "FLACs" made from an MP3. The MP3 already lost everything above ~16kHz, so the FLAC made
     * from it is far smaller than the real thing: Introduction (instructions) came in at 247kbps where
     * every honest CD copy was 575-670. Judged against the other copies of the song in the same format,
     * so it needs a few of them; a lone file is let through.
     */
    private fun dropFakes(hits: List<Hit>): List<Hit> {
        val medians = hits.filter { kbps(it) > 0 }.groupBy { it.bits to it.rate }
            .filterValues { it.size >= 3 }
            .mapValues { (_, copies) -> copies.map(::kbps).sorted()[copies.size / 2] }
        return hits.filter { h -> medians[h.bits to h.rate]?.let { kbps(h) >= it * FAKE_RATIO } ?: true }
    }

    private fun quality(hit: Hit) = when {
        hit.bits == 0 || hit.rate == 0 -> 1
        hit.bits <= 16 && hit.rate <= 48_000 -> 0
        else -> 2
    }

    private fun firstArtist(artist: String) = artist.split(",", "&", " • ").first().replace(FEAT, "").trim()

    private fun plainTitle(title: String) = title.replace(BRACKETS, "").replace(FEAT, "").trim()

    private fun words(s: String) = WORD.findAll(s.lowercase()).map { it.value }.toList()

    private fun squash(s: String) = words(s).joinToString("")
}
