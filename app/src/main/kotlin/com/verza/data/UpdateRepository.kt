package com.verza.data

import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import androidx.core.content.FileProvider
import com.verza.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checking for, downloading and installing a new APK.
 *
 * Verza is not on Play, so nothing updates it — every release so far has needed the listener to
 * notice a new tag on GitHub and sideload it by hand, which is why the published release is still
 * v1.0.0 while the app is on 1.3.x. This closes that: it reads the latest release straight from the
 * GitHub API, compares versions, and hands the downloaded APK to the system installer.
 *
 * It deliberately does not install anything by itself. The download is explicit, and the install is
 * the platform's own confirmation dialog — an app that silently replaces itself is not something a
 * sideloaded music player should be doing.
 */
@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    data class Release(
        val version: String,
        val notes: String,
        val apkUrl: String,
        val sizeBytes: Long,
    )

    /** The newest release if it is newer than what is installed, else null. */
    suspend fun checkForUpdate(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            val body = client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body?.string() ?: return@withContext null
            }
            val obj = json.parseToJsonElement(body).jsonObject
            val tag = obj["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty().removePrefix("v")
            if (tag.isBlank() || !isNewer(tag, BuildConfig.VERSION_NAME)) return@withContext null

            // Prefer a real .apk asset. A release with only source archives is not installable.
            val asset = obj["assets"]?.jsonArray
                ?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk") == true }
                ?: return@withContext null

            Release(
                version = tag,
                notes = obj["body"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
                apkUrl = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: return@withContext null,
                sizeBytes = asset["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0L,
            )
        }.getOrNull()
    }

    /**
     * Download [release] into the cache and return the file, reporting 0..1 progress.
     *
     * Written to a .part first and renamed on completion, so a download interrupted by the process
     * dying cannot leave a truncated APK that the installer would then reject with a confusing
     * "package appears to be corrupt".
     */
    suspend fun download(release: Release, onProgress: (Float) -> Unit): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }   // only ever keep the one we are installing
            val target = File(dir, "Verza-${release.version}.apk")
            val part = File(dir, target.name + ".part")

            val request = Request.Builder().url(release.apkUrl).build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body ?: error("empty body")
                val total = if (release.sizeBytes > 0) release.sizeBytes else body.contentLength()
                body.byteStream().use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var done = 0L
                        while (input.read(buffer).also { read = it } >= 0) {
                            output.write(buffer, 0, read)
                            done += read
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            }
            if (!part.renameTo(target)) error("could not finalise the download")
            onProgress(1f)
            target
        }.getOrNull()
    }

    /**
     * The notes for a specific tag, used by the changelog after an update.
     *
     * Fetched rather than bundled: the notes are written when the release is cut, so shipping them
     * inside the APK would mean writing them twice and letting the two drift.
     */
    suspend fun notesFor(version: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$REPO/releases/tags/v$version")
                .header("Accept", "application/vnd.github+json")
                .build()
            val body = client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                resp.body?.string() ?: return@withContext null
            }
            json.parseToJsonElement(body).jsonObject["body"]?.jsonPrimitive?.contentOrNull?.trim()
        }.getOrNull()
    }

    /**
     * Whether Android will let Verza start an install at all.
     *
     * From Android 8 an app needs "install unknown apps" granted to it specifically. Without it the
     * installer opens and immediately refuses, which reads as the update being broken rather than as
     * a permission being missing, so it is worth asking first.
     */
    fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** Send the user to the one settings page that can grant it. */
    fun requestInstallPermission(): Boolean = runCatching {
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    /** Hand [apk] to the system installer. Returns false if the OS refuses to start it. */
    fun install(apk: File): Boolean = runCatching {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    companion object {
        private const val REPO = "SambuddhaRoy/Verza"

        /**
         * Dotted-number comparison, segment by segment.
         *
         * A string compare would call "1.10.0" older than "1.9.0", and that is exactly the version
         * where a naive check quietly stops offering updates — the failure is invisible until the
         * tenth patch release.
         */
        fun isNewer(candidate: String, current: String): Boolean {
            val a = candidate.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
            val b = current.split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}

private val kotlinx.serialization.json.JsonPrimitive.contentOrNull: String?
    get() = runCatching { content }.getOrNull()
