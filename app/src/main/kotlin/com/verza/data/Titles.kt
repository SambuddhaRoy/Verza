package com.verza.data

/**
 * A title as a song's, for showing: YouTube's uploads carry "(Official Video)", "[Lyrics]", the artist
 * in front ("Artist - Song") and quotes around the name. The song keeps its own title, which matching,
 * lyrics and downloads use. The desktop's nice() in renderer.js; keep the two the same.
 */
private val JUNK = Regex("""\s*[(\[](?:\s*(?:official|music|lyrics?|audio|video|visuali[sz]er|hd|hq|4k|mv|full|clip)\b[\s.]*)+[)\]]""", RegexOption.IGNORE_CASE)
private val TRAILING_VIDEO = Regex("""\s+(?:official\s+)?(?:music\s+|lyrics?\s+)?video\s*$""", RegexOption.IGNORE_CASE)
private val BAR = Regex("""\s+[|｜]\s+.*$""")
private val QUOTED = Regex("""^\s*['"‘“]([^'"’”]+)['"’”]""")
private val ARTIST_SPLIT = Regex(""",|\s&\s|\s(?:feat|ft)\.?\s""", RegexOption.IGNORE_CASE)

fun shownTitle(title: String, artist: String?): String {
    var s = title.replace(JUNK, "").replace(TRAILING_VIDEO, "").replace(BAR, "")
    val a = artist.orEmpty().split(ARTIST_SPLIT).first().trim()
    if (a.isNotEmpty() && s.lowercase().startsWith(a.lowercase() + " - ")) s = s.substring(a.length + 3)
    s = s.replace(QUOTED, "$1").trim()
    return s.ifEmpty { title }
}
