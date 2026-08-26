package com.verza.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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
    )

    /**
     * Reserve a file called [name] (extension included) in [treeUri], falling back to app-private
     * storage when no folder has been chosen or the chosen one is no longer writable — a listener
     * who picks an SD card and then ejects it should still get their download, just somewhere else.
     *
     * ponytail: no temp-file-then-rename here. SAF has no portable atomic rename, so an interrupted
     * download is cleaned up by discard() instead. Upgrade to DocumentsContract.renameDocument if a
     * provider ever leaves half-files behind on a hard kill.
     */
    fun create(treeUri: String?, name: String, mime: String): Target {
        if (!treeUri.isNullOrBlank()) {
            runCatching { createInTree(Uri.parse(treeUri), name, mime) }.getOrNull()?.let { return it }
        }
        return createPrivate(name)
    }

    private fun createInTree(tree: Uri, name: String, mime: String): Target? {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        // Providers resolve their own collisions here, appending "(1)" and so on, so two different
        // recordings of the same song never overwrite each other.
        val doc = DocumentsContract.createDocument(resolver, parent, mime, name) ?: return null
        return Target(
            location = doc.toString(),
            open = { resolver.openOutputStream(doc) ?: error("provider refused to open $doc") },
            discard = { runCatching { DocumentsContract.deleteDocument(resolver, doc) } },
        )
    }

    private fun createPrivate(name: String): Target {
        val dir = File(context.getExternalFilesDir(null), "downloads").apply { mkdirs() }
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
