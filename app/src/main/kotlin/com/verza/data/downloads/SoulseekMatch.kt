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

    /** The first credited artist and the bare title: "Get Lucky (feat. Pharrell...)" searches as "Daft Punk Get Lucky". */
    fun query(artist: String, title: String): String =
        (words(firstArtist(artist)) + words(plainTitle(title))).joinToString(" ")

    /** The files worth trying for this song, best first. Empty when nothing is the same recording. */
    fun rank(hits: List<Hit>, title: String, durationMs: Long): List<Hit> {
        val want = squash(plainTitle(title))
        val titleWords = words(title).toSet()
        val seconds = durationMs / 1000
        return hits.filter { hit ->
            val name = hit.path.substringAfterLast('\\').substringBeforeLast('.')
            val nameWords = words(name)
            want.isNotEmpty() && want in squash(name) &&
                nameWords.none { it in VARIANTS && it !in titleWords } &&
                // Without a length in the result a 30-second preview looks the same as the song
                (seconds <= 0 || (hit.secs > 0 && abs(hit.secs - seconds) <= TOLERANCE_S))
        }.sortedWith(
            // ponytail: CD quality over hi-res, it's a third of the size and sounds the same on a phone
            compareBy<Hit>({ !it.slots }, { it.queue }, { quality(it) }, { -it.speed }),
        )
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
