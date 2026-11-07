package com.verza.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/**
 * Pages kept on disk, so a page draws at once from its last copy and updates when the network
 * answers. In the cache directory: Android may clear it, and that only costs a slower first look.
 */
class Saved(context: Context) {
    private val dir = File(context.cacheDir, "pages").apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true }

    private fun file(key: String) = File(dir, MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).joinToString("") { "%02x".format(it) })

    /**
     * The saved copy first, if there is one, then the fresh one, which is saved for next time. A failed
     * fetch with a copy already shown ends quietly; with nothing shown it throws, for the page to say so.
     */
    fun <T> load(key: String, serializer: KSerializer<T>, fetch: suspend () -> T): Flow<T> = flow {
        val f = file(key)
        val saved = withContext(Dispatchers.IO) { runCatching { json.decodeFromString(serializer, f.readText()) }.getOrNull() }
        if (saved != null) emit(saved)
        val fresh = try { fetch() } catch (e: Exception) { if (saved == null) throw e else return@flow }
        if (fresh != saved) emit(fresh)
        withContext(Dispatchers.IO) { runCatching { f.writeText(json.encodeToString(serializer, fresh)) } }
    }
}
