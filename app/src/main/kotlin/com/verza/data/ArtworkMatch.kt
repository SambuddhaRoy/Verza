package com.verza.data

/** One row from an artwork search, reduced to the three fields worth judging it on. */
data class ArtworkCandidate(
    val trackName: String,
    val artistName: String,
    val artworkUrl: String,
)

/**
 * Which search result, if any, is actually the track we asked about.
 *
 * The search was taking the first result and trusting it. iTunes matches loosely, so asking for a
 * common title returned a different recording by a different artist and the app displayed it with
 * complete confidence: the right words, the wrong record.
 *
 * Both the artist and the title have to agree before a cover is used. When neither can be
 * established the answer is null and the track keeps its own thumbnail, because a plain YouTube
 * still is a smaller error than someone else's album.
 *
 * Pure so it can be tested without a network: the failure this exists to stop is a matching
 * failure, not a transport one.
 */
fun bestArtworkMatch(
    candidates: List<ArtworkCandidate>,
    title: String,
    artist: String,
): ArtworkCandidate? {
    val wantTitle = normalise(title)
    // With no artist there is nothing to tell two recordings of the same song apart, and guessing
    // is exactly the behaviour being removed.
    val wantArtist = normalise(artist)
    if (wantTitle.isBlank() || wantArtist.isBlank()) return null

    return candidates.firstOrNull { candidate ->
        matches(normalise(candidate.artistName), wantArtist) &&
            matches(normalise(candidate.trackName), wantTitle)
    }
}

/**
 * Whether two normalised names refer to the same thing.
 *
 * Containment either way rather than equality, because the two sources disagree about detail in
 * both directions: YouTube says "Oasis" where iTunes says "Oasis featuring someone", and iTunes
 * says "Wonderwall" where YouTube says "Wonderwall Remastered". Requiring the shorter to be at
 * least four characters stops "Go" matching everything with "go" in it.
 */
private fun matches(a: String, b: String): Boolean {
    if (a == b) return true
    val shorter = if (a.length <= b.length) a else b
    val longer = if (a.length <= b.length) b else a
    return shorter.length >= 4 && longer.contains(shorter)
}

/**
 * Strip a name down to the part both sources agree on.
 *
 * Bracketed suffixes go, because "(Remastered 2009)" and "(Official Video)" are the same recording
 * to a listener. Featured artists go, because which of them is credited in the title and which in
 * the artist field differs by source. "- Topic" goes, which is what YouTube calls an auto-generated
 * artist channel.
 */
private fun normalise(raw: String): String {
    var s = raw.lowercase()
    s = s.replace(Regex("\\([^)]*\\)"), " ")
    s = s.replace(Regex("\\[[^\\]]*]"), " ")
    s = s.replace(Regex("\\s-\\s*topic\\b"), " ")
    s = s.replace(Regex("\\b(feat|ft|featuring|with)\\b.*"), " ")
    s = s.replace(Regex("[^a-z0-9 ]"), " ")
    return s.replace(Regex("\\s+"), " ").trim()
}
