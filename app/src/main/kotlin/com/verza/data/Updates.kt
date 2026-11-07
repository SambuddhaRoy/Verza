package com.verza.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.ConnectivityManager
import android.os.Build
import com.verza.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** A newer release on GitHub, and its APK once downloaded. */
data class Release(val version: String, val notes: String, val apkUrl: String, val size: Long, val apk: File? = null)

/**
 * Verza updates itself from its GitHub releases, being sideloaded. The APK is downloaded before the
 * update is offered, so taking it starts the install at once.
 *
 * The install goes through a PackageInstaller session rather than handing the file to the system's
 * installer. From Android 12 an app updating itself can ask for no confirmation (USER_ACTION_NOT_REQUIRED);
 * where Android still wants one it says so (STATUS_PENDING_USER_ACTION) and [InstallResult] shows it.
 */
class Updates(private val context: Context, private val http: OkHttpClient) {
    private val json = Json { ignoreUnknownKeys = true }
    private val _ready = MutableStateFlow<Release?>(null)
    /** A newer release whose APK is downloaded and can be installed now. */
    val ready: StateFlow<Release?> = _ready.asStateFlow()

    /** Checks, and downloads a newer release unless on a metered network. */
    suspend fun check(): Release? = withContext(Dispatchers.IO) {
        val release = runCatching { latest() }.getOrNull() ?: return@withContext null
        val metered = context.getSystemService(ConnectivityManager::class.java).isActiveNetworkMetered
        if (metered) return@withContext release
        runCatching { download(release) }.getOrNull()?.let { r -> _ready.value = r; r } ?: release
    }

    private fun latest(): Release? {
        val body = http.newCall(Request.Builder().url("https://api.github.com/repos/$REPO/releases/latest").header("Accept", "application/vnd.github+json").build())
            .execute().use { if (it.isSuccessful) it.body!!.string() else return null }
        val o = json.parseToJsonElement(body).jsonObject
        val version = o["tag_name"]?.jsonPrimitive?.contentOrNull.orEmpty().removePrefix("v")
        if (!isNewer(version, BuildConfig.VERSION_NAME)) return null
        // A release with only source archives isn't installable.
        val apk = o["assets"]?.jsonArray?.map { it.jsonObject }?.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull.orEmpty().endsWith(".apk") } ?: return null
        return Release(version, o["body"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(), apk["browser_download_url"]!!.jsonPrimitive.content, apk["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 0)
    }

    /** Into the cache, through a .part file, so a download cut short can't pass for an APK. */
    private fun download(r: Release): Release {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(dir, "Verza-${r.version}.apk")
        if (apk.exists() && (r.size <= 0 || apk.length() == r.size)) return r.copy(apk = apk)
        dir.listFiles()?.forEach { it.delete() }
        val part = File(dir, apk.name + ".part")
        http.newCall(Request.Builder().url(r.apkUrl).build()).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            part.outputStream().use { out -> resp.body!!.byteStream().use { it.copyTo(out) } }
        }
        if (!part.renameTo(apk)) error("couldn't finish the download")
        return r.copy(apk = apk)
    }

    /** Whether Android lets Verza install apps; when not, Settings has to allow it first. */
    fun allowed(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun install(r: Release) {
        val apk = r.apk ?: return
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("verza.apk", 0, apk.length()).use { out -> apk.inputStream().use { it.copyTo(out) }; session.fsync(out) }
            val done = PendingIntent.getBroadcast(
                context, id, Intent(context, InstallResult::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0),
            )
            session.commit(done.intentSender)
        }
    }

    companion object {
        const val REPO = "SambuddhaRoy/Verza"

        /** Dotted numbers, segment by segment: a string compare calls 1.10.0 older than 1.9.0. */
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

/** Where an install session reports back. When Android wants the listener to confirm, it shows its prompt. */
class InstallResult : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            (intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}
