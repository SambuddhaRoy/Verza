package com.verza.data.downloads

import android.content.Context
import android.media.MediaMetadataRetriever
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.verza.BuildConfig
import com.verza.data.Song
import java.io.File

/**
 * A download's tags, written through tagger.py (mutagen) so the file is a proper album track in any
 * player: title, artist and album as Verza knows them, the rest (album artist, track, disc, year) from
 * MusicBrainz, and a cover from the Cover Art Archive, or else YouTube Music's.
 */
object Tags {
    /** The interpreter, started once for the tagger and Soulseek both. */
    @Synchronized
    fun python(context: Context): Python {
        if (!Python.isStarted()) Python.start(AndroidPlatform(context.applicationContext))
        return Python.getInstance()
    }

    /** Fills in what [file] is missing. Never fails a download: an untagged file still plays. */
    fun write(context: Context, file: File, song: Song, cover: () -> ByteArray?) {
        runCatching {
            val py = python(context)
            val tagger = py.getModule("tagger")
            tagger.put("USER_AGENT", "Verza/${BuildConfig.VERSION_NAME} ( https://github.com/SambuddhaRoy/Verza )")
            val hints = py.builtins.callAttr("dict")
            hints.callAttr("__setitem__", "title", song.title)
            hints.callAttr("__setitem__", "artist", song.artist)
            song.album?.let { hints.callAttr("__setitem__", "album", it) }
            tagger.callAttr("tag_file", file.path, null, hints)
            if (tagger.callAttr("_has_cover", file.path)?.toBoolean() != true) {
                cover()?.let { tagger.callAttr("_embed", file.path, py.builtins.callAttr("bytes", it), "image/jpeg") }
            }
        }
    }

    /** The album artist and album [file]'s tags name, which decide its folder. */
    fun read(file: File): Pair<String?, String?> = runCatching {
        MediaMetadataRetriever().run {
            try {
                setDataSource(file.path)
                extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.takeIf { it.isNotBlank() } to
                    extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }
            } finally { release() }
        }
    }.getOrDefault(null to null)
}
