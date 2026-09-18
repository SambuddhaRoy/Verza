package com.verza.data

import com.verza.data.db.PlayWithSong
import kotlin.math.pow

/**
 * What the listener actually likes, worked out from the play log.
 *
 * Recommendations used to be seeded from whatever played last. One song you tried yesterday then
 * outweighed an album you have had on repeat for a month, because "recent" was the only signal.
 * Here every listen counts. A listen is worth how much of the song was heard, so a skip after ten
 * seconds barely registers, and it fades with a sixty day half-life, so taste can drift without
 * a single new play overturning it. Twenty listens a month ago still outweigh one this morning.
 *
 * Everything in here is pure so it can be tested without a database or a network.
 */
object Taste {

    /** A listen of this long counts as a whole play. Most songs are longer, and a skip is far shorter. */
    const val FULL_LISTEN_MS = 180_000L

    /** How long until a listen counts for half as much. */
    const val HALF_LIFE_DAYS = 60.0

    data class ScoredSong(
        val id: String,
        val title: String,
        val artist: String,
        val thumbnailUrl: String?,
        val score: Double,
    )

    /** An artist and their songs, best first. [score] is the sum of theirs. */
    data class ScoredArtist(val name: String, val score: Double, val songs: List<ScoredSong>)

    /** A genre or vibe, weighted by how much the listener plays the artists in it. */
    data class Bucket<K>(val key: K, val weight: Double, val artists: List<ScoredArtist>)

    /** A MusicBrainz genre or tag with its vote count. */
    data class Tag(val name: String, val count: Int)

    fun playWeight(listenedMs: Long, ageMs: Long): Double {
        val heard = (listenedMs.coerceAtLeast(0).toDouble() / FULL_LISTEN_MS).coerceAtMost(1.0)
        val ageDays = ageMs.coerceAtLeast(0) / 86_400_000.0
        return heard * 0.5.pow(ageDays / HALF_LIFE_DAYS)
    }

    /** Every song ever played, scored, best first. */
    fun songs(plays: List<PlayWithSong>, now: Long): List<ScoredSong> =
        plays.groupBy { it.songId }
            .map { (id, listens) ->
                val latest = listens.maxBy { it.playedAt }
                ScoredSong(
                    id = id,
                    title = latest.title,
                    artist = cleanArtist(latest.artist),
                    thumbnailUrl = latest.thumbnailUrl,
                    score = listens.sumOf { playWeight(it.listenedMs, now - it.playedAt) },
                )
            }
            .sortedByDescending { it.score }

    /** Artists by the total score of their songs, best first. */
    fun artists(songs: List<ScoredSong>): List<ScoredArtist> =
        songs.filter { it.artist.isNotEmpty() }
            .groupBy { it.artist.lowercase() }
            .map { (_, theirs) ->
                val best = theirs.sortedByDescending { it.score }
                ScoredArtist(best.first().artist, best.sumOf { it.score }, best)
            }
            .sortedByDescending { it.score }

    private val artistNoise = listOf(
        // "Oasis • 9.4M views • 4:07": everything after a bullet is metadata.
        Regex("""\s*[•·]\s.*$"""),
        // The same without bullets: "Oasis 9.4M views 4:07".
        Regex("""\s+[\d.,]+\s*[KMB]?\s+(views|plays)\b.*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+\d{1,2}:\d{2}(:\d{2})?$"""),
        Regex("""\s+-\s+Topic$""", RegexOption.IGNORE_CASE),
        // Plays saved before the search parser kept the result-type label out of the artist.
        Regex("""^(Song|Video)\s+"""),
    )
    private val featuring = Regex("""\s*(,|\bfeat\.?|\bft\.?|\bfeaturing\b)\s*""", RegexOption.IGNORE_CASE)

    /**
     * The primary artist, without the view counts and durations YouTube sometimes leaves in the
     * byline. Those are why Home once said "Similar to Oasis 9.4M views 4:07", and why the same band
     * could count as several artists. Only splits on commas and "feat", never "&", so Simon &
     * Garfunkel stay one act.
     */
    fun cleanArtist(raw: String): String {
        var name = raw.trim()
        artistNoise.forEach { name = name.replace(it, "") }
        name = name.split(featuring).first().trim()
        return if (name.equals("Unknown artist", ignoreCase = true)) "" else name
    }

    // ── Genres ───────────────────────────────────────────────────────────────────────────────────

    /**
     * Genres broad enough to be someone's answer to "what do you listen to?". The rest of
     * MusicBrainz's vocabulary rolls up into these where it can. Matches the desktop app.
     */
    val TOP_LEVEL = setOf(
        "rock", "pop", "hip hop", "rap", "electronic", "dance", "r&b", "soul", "jazz", "blues",
        "country", "folk", "metal", "punk", "classical", "reggae", "latin", "funk", "disco",
        "gospel", "ambient", "house", "techno", "indie",
    )

    /** "alternative rock" is rock, "indian pop" is pop. A genre with no broader parent is its own. */
    fun parentOf(genre: String): String {
        val tag = genre.lowercase().trim()
        if (tag in TOP_LEVEL) return tag
        return TOP_LEVEL.firstOrNull { tag.endsWith(" $it") || tag.startsWith("$it ") } ?: tag
    }

    /**
     * How strongly an artist belongs to each broad genre, from 0 to 1. The artist's most-voted genre
     * is 1. Several sub-genres of one parent do not add up past 1, so an artist tagged five kinds of
     * rock is not five times as rock as one tagged once.
     */
    fun genreShares(genres: List<Tag>): Map<String, Double> {
        val strongest = genres.maxOfOrNull { it.count }?.takeIf { it > 0 } ?: return emptyMap()
        val shares = HashMap<String, Double>()
        for (g in genres) {
            if (g.count <= 0) continue
            val parent = parentOf(g.name)
            shares[parent] = maxOf(shares[parent] ?: 0.0, g.count.toDouble() / strongest)
        }
        return shares
    }

    // ── Vibes ────────────────────────────────────────────────────────────────────────────────────

    /**
     * A feel rather than a genre. There is no free source of per-song mood, so a vibe is read from
     * an artist's genres plus the mood words people tag them with on MusicBrainz. Coarse, since it
     * is per artist, but it groups an actual library into something recognisable.
     */
    enum class Vibe(val title: String, val subtitle: String, val words: List<String>) {
        CHILL(
            "Chill", "The mellow side of your rotation, and more like it",
            listOf(
                "chill", "chillout", "mellow", "relaxing", "calm", "ambient", "lo-fi", "lofi", "downtempo",
                "dream pop", "acoustic", "bossa nova", "trip hop", "easy listening", "folk",
                "singer-songwriter", "soft rock", "chamber pop", "bedroom pop",
            ),
        ),
        ENERGY(
            "High energy", "The loud, fast end of what you play",
            listOf(
                "energetic", "upbeat", "edm", "house", "techno", "drum and bass", "dubstep", "trap", "punk",
                "metal", "hard rock", "hardcore", "electro", "big beat", "grime", "drill", "party",
                "pop punk", "nu metal", "hyperpop",
            ),
        ),
        FEEL_GOOD(
            "Feel good", "Bright, bouncy and easy to love",
            listOf(
                "happy", "feel good", "funk", "disco", "motown", "summer", "reggae", "ska", "britpop",
                "indie pop", "power pop", "synth-pop", "synthpop", "k-pop", "bubblegum pop", "afrobeats",
                "dancehall", "dance-pop", "dance pop",
            ),
        ),
        MOODY(
            "In your feelings", "The heavy-hearted corner of your library",
            listOf(
                "sad", "melancholic", "melancholy", "emo", "dark", "sadcore", "slowcore", "gothic", "post-punk",
                "shoegaze", "post-rock", "grunge", "depressive", "heartbreak", "blues", "darkwave",
            ),
        ),
        AFTER_HOURS(
            "After hours", "Smooth and late, for when the lights go down",
            listOf(
                "r&b", "rnb", "contemporary r&b", "neo soul", "soul", "jazz", "smooth", "sensual", "romantic",
                "lounge", "synthwave", "quiet storm", "alternative r&b",
            ),
        ),
    }

    /** Whether [tag] says [word], as a whole word or phrase, so "house" does not match "madhouse". */
    private fun says(tag: String, word: String): Boolean =
        " ${tag.lowercase().replace('-', ' ')} ".contains(" ${word.replace('-', ' ')} ")

    /**
     * How strongly an artist has each vibe, from 0 to 1. Each matching genre or tag adds its share of
     * the artist's strongest vote, so an artist whose defining genre is shoegaze is firmly moody while
     * one stray "sad" tag barely moves them.
     */
    fun vibeShares(genres: List<Tag>, tags: List<Tag>): Map<Vibe, Double> {
        val all = genres + tags
        val strongest = all.maxOfOrNull { it.count }?.takeIf { it > 0 } ?: return emptyMap()
        val shares = HashMap<Vibe, Double>()
        for (t in all) {
            if (t.count <= 0) continue
            for (vibe in Vibe.entries) {
                if (vibe.words.any { says(t.name, it) }) {
                    shares[vibe] = ((shares[vibe] ?: 0.0) + t.count.toDouble() / strongest).coerceAtMost(1.0)
                }
            }
        }
        return shares
    }

    /**
     * Groups artists into buckets (genres or vibes), weighting each bucket by the listener's plays of
     * the artists in it rather than by how many artists it has. An artist only joins a bucket they
     * belong to at least [minShare], so a passing tag does not drag them into it.
     */
    fun <K> buckets(
        artists: List<ScoredArtist>,
        minShare: Double = 0.5,
        sharesOf: (ScoredArtist) -> Map<K, Double>,
    ): List<Bucket<K>> {
        val weights = HashMap<K, Double>()
        val members = HashMap<K, MutableList<Pair<ScoredArtist, Double>>>()
        for (artist in artists) {
            for ((key, share) in sharesOf(artist)) {
                if (share < minShare) continue
                weights[key] = (weights[key] ?: 0.0) + artist.score * share
                members.getOrPut(key) { mutableListOf() } += artist to artist.score * share
            }
        }
        return weights.entries
            .sortedByDescending { it.value }
            .map { (key, weight) ->
                Bucket(key, weight, members.getValue(key).sortedByDescending { it.second }.map { it.first })
            }
    }

    // ── Mixing ───────────────────────────────────────────────────────────────────────────────────

    /**
     * Merges several lists into one, each contributing in proportion to its weight and spread evenly
     * through the result rather than in blocks. Duplicates by [key] are skipped.
     *
     * This replaces filling a mix from the first seed's radio until it was full, which is how a
     * "from your favourites" mix ended up being one song's radio with the others never reached.
     */
    fun <T> blend(lists: List<Pair<List<T>, Double>>, limit: Int, key: (T) -> Any?): List<T> {
        val cursors = IntArray(lists.size)
        val credit = DoubleArray(lists.size)
        val seen = HashSet<Any?>()
        val out = ArrayList<T>()
        while (out.size < limit) {
            val active = lists.indices.filter { cursors[it] < lists[it].first.size && lists[it].second > 0 }
            if (active.isEmpty()) break
            // Smooth weighted round robin: every list earns its weight, the richest one pays out.
            val total = active.sumOf { lists[it].second }
            active.forEach { credit[it] += lists[it].second }
            val pick = active.maxBy { credit[it] }
            credit[pick] -= total
            val list = lists[pick].first
            while (cursors[pick] < list.size) {
                val item = list[cursors[pick]++]
                if (seen.add(key(item))) {
                    out += item
                    break
                }
            }
        }
        return out
    }

    /** "hip hop" → "Hip Hop", "r&b" → "R&B". */
    fun displayName(genre: String): String =
        genre.split(' ').joinToString(" ") { word ->
            if (word.length <= 3 && word.contains('&')) word.uppercase()
            else word.replaceFirstChar { it.uppercaseChar() }
        }
}
