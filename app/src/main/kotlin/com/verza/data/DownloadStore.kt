package com.verza.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where a downloaded track is written, and under what name.
 *
 * Downloads used to land in app-private external storage as `<videoId>.<ext>` — invisible to every
 * other app on the phone and deleted along with Verza on uninstall. A download that only Verza can
 * see is a cache, not a library. So the destination is now a folder the listener picks with the
 * system file picker, written through SAF, and the files are named for a person.
 *
 * Uses [DocumentsContract] directly rather than adding androidx.documentfile: the platform API is
 * all we need, and it hands back the content Uri that goes in the library row.
 */
@Singleton
class DownloadStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        /** Our folder inside the shared Music directory. Everything Verza saves lives under it. */
        const val ROOT_FOLDER = "Verza"
    }

    /**
     * A destination that has been reserved but not yet written.
     *
     * [location] is what goes in the library row: a `content://` Uri when the listener chose a
     * folder, an absolute path when we fell back to app-private storage. Both are things ExoPlayer
     * opens natively (see MusicService's resolver).
     */
    class Target(
        val location: String,
        val open: () -> OutputStream,
        val discard: () -> Unit,
        val commit: () -> Unit = {},
    )

    /**
     * Reserve a file called [name] (extension included), in [collection]'s own folder when it came
     * from a playlist or album so that stays one thing on disk.
     *
     * Three destinations, in order: the folder the listener chose; otherwise the shared Music
     * folder via MediaStore, which needs no permission and puts the files where every other music
     * app already looks; otherwise app-private storage. That last one is the pre-Q fallback and the
     * emergency exit for a chosen folder that has since been ejected or revoked.
     *
     * ponytail: no temp-file-then-rename on the SAF path. SAF has no portable atomic rename, so an
     * interrupted download is cleaned up by discard() instead. The MediaStore path gets this for
     * free through IS_PENDING, which hides the file until it is complete.
     */
    fun create(treeUri: String?, name: String, mime: String, collection: String = ""): Target {
        val folder = DownloadNaming.folder(collection)
        if (!treeUri.isNullOrBlank()) {
            runCatching { createInTree(Uri.parse(treeUri), name, mime, folder) }.getOrNull()?.let { return it }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching { createInMediaStore(name, mime, folder) }.getOrNull()?.let { return it }
        }
        return createPrivate(name, folder)
    }

    /**
     * Music/Verza (plus the collection folder) through MediaStore. No storage permission is needed
     * for a file this app inserts, and the result is indexed, so it shows up in every other player
     * on the phone without a rescan.
     */
    private fun createInMediaStore(name: String, mime: String, folder: String): Target? {
        val resolver = context.contentResolver
        val relative = listOfNotNull(
            Environment.DIRECTORY_MUSIC,
            ROOT_FOLDER,
            folder.ifBlank { null },
        ).joinToString("/") + "/"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, relative)
            put(MediaStore.Audio.Media.IS_PENDING, 1)   // invisible until the bytes are all there
        }
        val collectionUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val item = resolver.insert(collectionUri, values) ?: return null
        return Target(
            location = item.toString(),
            open = { resolver.openOutputStream(item) ?: error("MediaStore refused to open $item") },
            commit = {
                resolver.update(item, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            },
            discard = { runCatching { resolver.delete(item, null, null) } },
        )
    }

    private fun createInTree(tree: Uri, name: String, mime: String, folder: String): Target? {
        val resolver = context.contentResolver
        val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val parent = if (folder.isBlank()) root else (findOrCreateDir(tree, root, folder) ?: root)
        // Providers resolve their own collisions here, appending "(1)" and so on, so two different
        // recordings of the same song never overwrite each other.
        val doc = DocumentsContract.createDocument(resolver, parent, mime, name) ?: return null
        return Target(
            location = doc.toString(),
            open = { resolver.openOutputStream(doc) ?: error("provider refused to open $doc") },
            discard = { runCatching { DocumentsContract.deleteDocument(resolver, doc) } },
        )
    }

    /**
     * A subdirectory of [parent] called [name], reusing one that already exists. Without the lookup
     * every download into the same playlist would make "Name (1)", "Name (2)" and so on, because
     * createDocument resolves collisions by renaming rather than failing.
     */
    private fun findOrCreateDir(tree: Uri, parent: Uri, name: String): Uri? {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree, DocumentsContract.getDocumentId(parent),
        )
        runCatching {
            resolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    if (c.getString(1) == name && c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                        return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                    }
                }
            }
        }
        return DocumentsContract.createDocument(resolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, name)
    }

    private fun createPrivate(name: String, folder: String = ""): Target {
        val base = File(context.getExternalFilesDir(null), "downloads")
        val dir = (if (folder.isBlank()) base else File(base, folder)).apply { mkdirs() }
        val file = uniqueFile(dir, name)
        return Target(
            location = file.absolutePath,
            open = { file.outputStream() },
            discard = { runCatching { file.delete() } },
        )
    }

    /** Delete whatever [location] points at, whether it is a content Uri or a path. */
    fun delete(location: String) {
        runCatching {
            if (location.startsWith("content://")) {
                DocumentsContract.deleteDocument(context.contentResolver, Uri.parse(location))
            } else {
                File(location).delete()
            }
        }
    }

    /**
     * A readable name for a chosen folder: a tree Uri of "primary:Music/Verza" reads back as
     * "Music/Verza". Falls back to something honest rather than showing raw percent-encoding.
     */
    fun folderLabel(treeUri: String?): String {
        if (treeUri.isNullOrBlank()) return ""
        return runCatching {
            val id = DocumentsContract.getTreeDocumentId(Uri.parse(treeUri))
            id.substringAfter(':', id).ifBlank { id }
        }.getOrDefault("Chosen folder")
    }

    /** True if the chosen folder is still readable — an ejected card or a revoked grant is not. */
    fun canWrite(treeUri: String?): Boolean {
        if (treeUri.isNullOrBlank()) return false
        return runCatching {
            val tree = Uri.parse(treeUri)
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            context.contentResolver.query(parent, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
                ?.use { true } ?: false
        }.getOrDefault(false)
    }

    private fun uniqueFile(dir: File, name: String): File {
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var candidate = File(dir, name)
        var n = 2
        while (candidate.exists() && n < 1000) {
            candidate = File(dir, "$stem ($n)$ext")
            n++
        }
        return candidate
    }
}

/**
 * Filenames for downloaded tracks. Pure, so the self-check can pin the edge cases.
 *
 * These names have to survive more than Android's own filesystem: a chosen folder can be a FAT32 SD
 * card or a network share, and both inherit Windows' rules. So the Windows restrictions are the ones
 * worth enforcing everywhere — they are the strictest, and getting them wrong shows up as a download
 * that fails with an error blaming the extractor.
 */
object DownloadNaming {

    // Windows and FAT32 forbid these outright. Space and hyphen are NOT in here: they are the two
    // characters that "Artist - Title" is actually made of.
    private val ILLEGAL = Regex("""[<>:"/\\|?*]""")
    private val RESERVED = Regex("^(con|prn|aux|nul|com[1-9]|lpt[1-9])$", RegexOption.IGNORE_CASE)
    private const val MAX = 120

    /**
     * A folder name for a playlist or album, or "" when there is nothing usable. Empty means the
     * track goes straight into the root folder, which is what a single downloaded song should do.
     */
    fun folder(collection: String?): String {
        val raw = collection.orEmpty().trim()
        if (raw.isEmpty()) return ""
        // stem() never returns empty — it falls back to the id — so pass a blank id and check for it,
        // or a playlist called "???" would become a folder called "track".
        val name = stem("", raw, "")
        return if (name == "track") "" else name
    }

    /** "Artist - Title", cleaned up, never empty. Extension not included. */
    fun stem(artist: String?, title: String?, fallbackId: String): String {
        val a = artist.orEmpty().trim()
        val t = title.orEmpty().trim().ifEmpty { fallbackId }
        var name = (if (a.isNotEmpty()) "$a - $t" else t)
            .replace(ILLEGAL, "")
            .filter { it.code >= 0x20 }              // control characters are illegal too
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', ' ')                 // Windows drops these silently, so we would lose the file
        if (RESERVED.matches(name)) name = "_$name"
        if (name.length > MAX) name = name.take(MAX).trim().trimEnd('.', ' ')
        return name.ifEmpty { fallbackId.ifEmpty { "track" } }
    }

    /**
     * The extension and mime for a stream. Anything that isn't AAC-in-MP4 keeps its own container:
     * we have no converter on Android, and mislabelling Opus as .m4a produces a file that looks
     * playable and isn't.
     */
    fun containerFor(mimeType: String): Pair<String, String> = when {
        mimeType.contains("mp4", true) || mimeType.contains("aac", true) -> "m4a" to "audio/mp4"
        mimeType.contains("opus", true) -> "opus" to "audio/opus"
        mimeType.contains("webm", true) -> "webm" to "audio/webm"
        mimeType.contains("mpeg", true) -> "mp3" to "audio/mpeg"
        else -> "m4a" to "audio/mp4"
    }

    fun fileName(artist: String?, title: String?, fallbackId: String, mimeType: String): String {
        val (ext, _) = containerFor(mimeType)
        return "${stem(artist, title, fallbackId)}.$ext"
    }
}
