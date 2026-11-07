package com.verza.data.phone

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.verza.data.Album
import com.verza.data.ArtworkRepository
import com.verza.data.Song
import com.verza.data.library.Library
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/** An album or a playlist made from the music on the phone. [id] opens it on the collection page. */
data class PhoneCollection(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val isAlbum: Boolean,
    val songs: List<Song>,
) {
    val subtitle: String
        get() = if (isAlbum) listOfNotNull(artist.ifBlank { null }, year?.toString()).joinToString(" · ") else "Playlist"
}

data class PhoneLibrary(
    val songs: List<Song>,
    val albums: List<PhoneCollection>,
    val playlists: List<PhoneCollection>,
)

/** One file as MediaStore describes it, before it is sorted into albums and playlists. */
data class PhoneFile(
    val item: Song,
    /** Relative to the storage root, ending in '/': "Music/Coldplay/Parachutes (2000)/". */
    val folder: String,
    val fileName: String,
    val album: String?,
    val albumArtist: String?,
    /** MediaStore's, which is disc * 1000 + track. */
    val track: Int,
    val year: Int?,
    /** False when the file names no artist, in which case its album is just its folder's name. */
    val tagged: Boolean,
)

/**
 * Reads the user's on-device music via [MediaStore]. Each track is surfaced as a [Song] whose
 * `id` is the file's `content://` URI — that URI is self-describing, so the rest of the app (queue,
 * playlists, likes) treats local tracks exactly like remote ones, and the playback service plays
 * the content URI directly instead of resolving it through NewPipe (see `MusicService`).
 *
 * Songs Verza downloaded itself are files with no tags, so they take their names, cover and video id
 * back from the library's record of the download, and play as the song they are.
 *
 * Read-only. Requires READ_MEDIA_AUDIO (API 33+) / READ_EXTERNAL_STORAGE (≤ API 32); the caller is
 * responsible for holding the runtime permission before invoking [scan].
 */
class PhoneMusic(
    private val context: Context,
    private val saved: Library,
    private val artwork: ArtworkRepository,
) {
    /** Album-art base URI; appending an albumId yields a loadable cover (Coil reads content URIs). */
    private val albumArtBase: Uri = Uri.parse("content://media/external/audio/albumart")

    /** The last scan, which the Library and the collection page show. Null until there has been one. */
    private val _library = MutableStateFlow<PhoneLibrary?>(null)
    val library: StateFlow<PhoneLibrary?> = _library.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var filling: Job? = null
    /** Album id to the cover found for it by name; kept on disk so each album is looked up once. */
    private val coverFile get() = File(context.filesDir, "local-covers.json")
    private val covers: MutableMap<String, String> by lazy {
        runCatching { Json.decodeFromString<Map<String, String>>(coverFile.readText()) }.getOrDefault(emptyMap()).toMutableMap()
    }

    suspend fun collection(id: String): Album? {
        val lib = _library.value ?: scan()
        val c = (lib.albums + lib.playlists).firstOrNull { it.id == id } ?: return null
        val first = c.songs.firstOrNull()
        // The files' own picture, or the album's found by name, or a playlist's first song's.
        val cover = c.songs.firstNotNullOfOrNull { it.art }
            ?: if (c.isAlbum) artwork.resolveAlbum(c.title, c.artist) else first?.let { artwork.resolve(it.title, it.artist) }
        return Album(title = c.title, subtitle = c.subtitle, art = cover, songs = c.songs)
    }

    suspend fun scan(): PhoneLibrary = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DISPLAY_NAME,
            @Suppress("DEPRECATION") MediaStore.Audio.Media.DATA,
        ) + (if (Build.VERSION.SDK_INT >= 29) arrayOf(MediaStore.Audio.Media.RELATIVE_PATH) else emptyArray()) +
            // Asking for a column a phone hasn't got fails the whole query, so only where it's documented.
            (if (Build.VERSION.SDK_INT >= 30) arrayOf(ALBUM_ARTIST) else emptyArray())
        // Only real music files (excludes ringtones, notifications, podcasts flagged otherwise),
        // and skip zero-length entries the media scanner sometimes leaves behind.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val downloads = runCatching { saved.downloadRows() }.getOrDefault(emptyList())
        val downloadById = downloads.mapNotNull { s ->
            s.downloadPath?.takeIf { it.startsWith("content://media/") }?.let { Uri.parse(it).lastPathSegment?.toLongOrNull() to s }
        }.toMap()
        val downloadByName = downloads.associateBy { fileStem(it.artist, it.title, it.id).lowercase() }

        val files = ArrayList<PhoneFile>()
        val artChecked = HashMap<Long, Boolean>()
        runCatching {
            context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                fun str(col: String) = cursor.getColumnIndex(col).takeIf { it >= 0 }?.let { cursor.getString(it) }
                    ?.takeUnless { it.isBlank() || it == MediaStore.UNKNOWN_STRING }
                fun int(col: String) = cursor.getColumnIndex(col).takeIf { it >= 0 }?.let { cursor.getInt(it) } ?: 0

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val contentUri = ContentUris.withAppendedId(collection, id)
                    val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                    val fileName = str(MediaStore.Audio.Media.DISPLAY_NAME).orEmpty()
                    @Suppress("DEPRECATION")
                    val folder = str("relative_path") ?: relativeFolder(str(MediaStore.Audio.Media.DATA))
                    // A song Verza saved: its own record knows what it is.
                    val saved = downloadById[id] ?: downloadByName[fileName.substringBeforeLast('.').replace(COPY_SUFFIX, "").lowercase()]
                    val artist = saved?.artist ?: str(MediaStore.Audio.Media.ARTIST)
                    // Android reads no tags from a WAV, and plenty of files have none: "Artist - Title"
                    // is how such files are usually named, Verza's own downloads included.
                    val named = fileName.substringBeforeLast('.').split(" - ", limit = 2).takeIf { artist == null && it.size == 2 }
                    // An album id with no picture in the file or its folder gets null: its album's cover
                    // is looked up by name (fillCovers), or the song's own where it's in none.
                    val hasArt = artChecked.getOrPut(albumId) { hasAlbumArt(albumId) }
                    val item = Song(
                        id = saved?.id ?: contentUri.toString(),
                        title = saved?.title ?: named?.get(1)?.trim() ?: str(MediaStore.Audio.Media.TITLE) ?: fileName.substringBeforeLast('.'),
                        artist = artist ?: named?.get(0)?.trim() ?: "Unknown artist",
                        album = str(MediaStore.Audio.Media.ALBUM),
                        art = saved?.thumbnailUrl
                            ?: if (hasArt) ContentUris.withAppendedId(albumArtBase, albumId).toString() else null,
                        durationMs = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
                    )
                    files += PhoneFile(
                        item = item,
                        folder = folder,
                        fileName = fileName,
                        album = item.album,
                        albumArtist = if (saved == null) str(ALBUM_ARTIST) else null,
                        track = int(MediaStore.Audio.Media.TRACK),
                        year = int(MediaStore.Audio.Media.YEAR).takeIf { it > 0 },
                        tagged = artist != null,
                    )
                }
            }
        }
        val (albums, playlists) = PhoneGrouping.group(files)
        // A second copy of a song Verza saved is the same song, and a list can't show one id twice.
        val lib = withCovers(PhoneLibrary(files.map { it.item }.distinctBy { it.id }, albums, playlists))
        _library.value = lib
        fillCovers()
        lib
    }

    /**
     * Looks up, by name, the cover of each album whose files have no picture, and puts it on its songs
     * as each one lands. In the background: a library's first scan would otherwise wait on one lookup
     * per album. Songs outside an album are looked up one by one where they're shown, like any song.
     */
    private fun fillCovers() {
        if (filling?.isActive == true) return
        filling = scope.launch {
            val wanted = _library.value?.albums.orEmpty().filter { a -> a.songs.any { it.art == null } && a.id !in covers }
            for (album in wanted) {
                val url = artwork.resolveAlbum(album.title, album.artist) ?: continue
                synchronized(covers) {
                    covers[album.id] = url
                    runCatching { coverFile.writeText(Json.encodeToString(covers.toMap())) }
                }
                _library.value = _library.value?.let(::withCovers)
            }
        }
    }

    /** Each album's found cover on those of its songs that have none of their own. */
    private fun withCovers(lib: PhoneLibrary): PhoneLibrary {
        val byId = HashMap<String, String>()
        val albums = lib.albums.map { a ->
            val cover = synchronized(covers) { covers[a.id] } ?: return@map a
            a.copy(songs = a.songs.map { s -> if (s.art == null) s.copy(art = cover).also { byId[s.id] = cover } else s })
        }
        if (byId.isEmpty()) return lib
        return lib.copy(
            songs = lib.songs.map { s -> byId[s.id]?.let { s.copy(art = it) } ?: s },
            albums = albums,
            playlists = lib.playlists.map { p -> p.copy(songs = p.songs.map { s -> byId[s.id]?.let { s.copy(art = it) } ?: s }) },
        )
    }

    private fun hasAlbumArt(albumId: Long): Boolean = runCatching {
        context.contentResolver.openFileDescriptor(ContentUris.withAppendedId(albumArtBase, albumId), "r")?.use { true } ?: false
    }.getOrDefault(false)

    private companion object {
        /** MediaStore.Audio.Media.ALBUM_ARTIST, which is API 30. */
        const val ALBUM_ARTIST = "album_artist"
        /** " (1)", which a folder adds to a second file of the same name. */
        val COPY_SUFFIX = Regex("""\s\(\d+\)$""")

        /** "/storage/emulated/0/Music/x/a.flac" to "Music/x/", for phones before RELATIVE_PATH. */
        fun relativeFolder(path: String?): String {
            val dir = path?.substringBeforeLast('/', "") ?: return ""
            return dir.replace(Regex("^/storage/(emulated/\\d+|[^/]+)/?"), "").let { if (it.isEmpty()) "" else "$it/" }
        }
    }
}

/**
 * Albums and playlists from the folders music sits in. A folder whose songs all name one album by one
 * artist is that album, and an album split over folders (Disc 1, Disc 2) is put back together. Any other
 * folder is a playlist by the folder's name, except the top-level ones everything lands in (Music,
 * Download) and Verza's own folder of single songs, which are just songs.
 *
 * ponytail: a folder holding two whole albums side by side is a playlist, not two albums. Split it by
 * album when someone's library is laid out that way.
 */
object PhoneGrouping {
    private val FEATURING = Regex("""\s*(,|;|&|/|\s(feat\.?|ft\.?|featuring|with|x)\s).*$""", RegexOption.IGNORE_CASE)

    /** Who an album is by: its album artist, or else the first name in the song's artist line. */
    fun albumArtist(f: PhoneFile): String =
        f.albumArtist?.trim()?.takeIf { it.isNotEmpty() } ?: f.item.artist.replace(FEATURING, "").trim()

    fun group(files: List<PhoneFile>): Pair<List<PhoneCollection>, List<PhoneCollection>> {
        val albumFiles = ArrayList<PhoneFile>()
        val playlists = ArrayList<PhoneCollection>()
        for ((folder, inFolder) in files.groupBy { it.folder }) {
            val albums = inFolder.map { it.album.orEmpty().trim().lowercase() }.toSet()
            val artists = inFolder.map { albumArtist(it).lowercase() }.toSet()
            val isAlbum = inFolder.all { it.tagged } && albums.size == 1 && albums.first().isNotEmpty() && artists.size == 1
            // Where loose songs land: nothing in it is an album or a playlist, only songs.
            val loose = folder.trimEnd('/').split('/').count { it.isNotEmpty() } < 2 ||
                folder.equals("Music/Verza/", ignoreCase = true)
            when {
                loose -> Unit
                isAlbum -> albumFiles += inFolder
                else -> playlists += PhoneCollection(
                    id = idOf("playlist", folder),
                    title = folder.trimEnd('/').substringAfterLast('/'),
                    artist = "",
                    year = null,
                    isAlbum = false,
                    songs = inFolder.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.fileName }).map { it.item },
                )
            }
        }
        val albums = albumFiles
            .groupBy { it.album!!.trim().lowercase() to albumArtist(it).lowercase() }
            .map { (key, songs) ->
                val first = songs.first()
                PhoneCollection(
                    id = idOf("album", "${key.first}|${key.second}"),
                    title = first.album!!.trim(),
                    artist = albumArtist(first),
                    year = songs.mapNotNull { it.year }.minOrNull(),
                    isAlbum = true,
                    // Disc then track, which MediaStore keeps as one number; the file name breaks ties
                    // and orders an album whose files carry no track numbers.
                    songs = songs.sortedWith(compareBy<PhoneFile> { it.track }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.fileName }).map { it.item },
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, PhoneCollection::artist).thenBy { it.year ?: 0 }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
        return albums to playlists.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }

    /** "local:" and a hash: something the route can carry, stable from one scan to the next. */
    private fun idOf(kind: String, key: String): String =
        "local:" + MessageDigest.getInstance("SHA-1").digest("$kind|$key".toByteArray())
            .take(8).joinToString("") { "%02x".format(it) }

    fun isLocalId(id: String) = id.startsWith("local:")
}

/** "Artist - Title", the name a download is saved under, as the downloader writes it. */
internal fun fileStem(artist: String, title: String, id: String): String =
    (if (artist.isNotBlank()) "$artist - ${title.ifBlank { id }}" else title.ifBlank { id })
        .replace(Regex("""[<>:"/\\|?*]"""), "").filter { it.code >= 0x20 }.replace(Regex("\\s+"), " ").trim().trimEnd('.', ' ')
