package com.verza.data

private val ALBUM_ART = Regex("""^https://(?:lh\d|yt\d)\.(?:googleusercontent|ggpht)\.com/""")
private val SIZE = Regex("""=w\d+-h\d+|=s\d+""")

/**
 * A cover from YouTube Music itself, on googleusercontent or ggpht. It is the right picture for the
 * song's release and comes at any size, so it is never looked up anywhere else. A video's frame
 * (i.ytimg.com) is the one kind that is.
 */
fun isAlbumArt(url: String?): Boolean = url != null && ALBUM_ART.containsMatchIn(url)

/** The same cover at [px] square, for a YouTube Music cover; anything else as it is. */
fun artAt(url: String, px: Int): String =
    if (isAlbumArt(url) && SIZE.containsMatchIn(url)) url.replaceFirst(SIZE, "=w$px-h$px") else url
