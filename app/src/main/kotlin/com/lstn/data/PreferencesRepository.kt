package com.lstn.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lstn.di.ApplicationScope
import com.lstn.innertube.AudioQuality
import com.lstn.innertube.InnerTube
import com.lstn.ui.theme.LstnTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "lstn_settings")

/** Persists user preferences (theme + signed-in account cookie) and keeps InnerTube auth in sync. */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope scope: CoroutineScope,
) {
    private val store = context.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    private val themeKey = stringPreferencesKey("theme")
    private val cookieKey = stringPreferencesKey("account_cookie")
    private val historyKey = stringPreferencesKey("search_history")
    private val queueKey = stringPreferencesKey("saved_queue")
    private val audioQualityKey = stringPreferencesKey("audio_quality")

    val themeFlow: Flow<LstnTheme> = store.data.map { prefs ->
        prefs[themeKey]?.let { runCatching { LstnTheme.valueOf(it) }.getOrNull() } ?: LstnTheme.NOIR
    }

    val cookieFlow: Flow<String?> = store.data.map { it[cookieKey] }

    val audioQualityFlow: Flow<AudioQuality> = store.data.map { prefs ->
        prefs[audioQualityKey]?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() } ?: AudioQuality.HIGH
    }

    val searchHistoryFlow: Flow<List<String>> = store.data.map { prefs ->
        prefs[historyKey]?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
    }

    init {
        // Mirror the persisted cookie + audio quality into the InnerTube client for the app lifetime.
        scope.launch { cookieFlow.collect { InnerTube.cookie = it } }
        scope.launch { audioQualityFlow.collect { InnerTube.audioQuality = it } }
    }

    suspend fun setTheme(theme: LstnTheme) {
        store.edit { it[themeKey] = theme.name }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        store.edit { it[audioQualityKey] = quality.name }
    }

    suspend fun setCookie(cookie: String?) {
        store.edit { prefs ->
            if (cookie.isNullOrBlank()) prefs.remove(cookieKey) else prefs[cookieKey] = cookie
        }
    }

    // ── Search history (most-recent-first, capped) ─────────────────────────────

    suspend fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        store.edit { prefs ->
            val current = prefs[historyKey]?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }).take(10)
            prefs[historyKey] = json.encodeToString(updated)
        }
    }

    suspend fun clearSearchHistory() {
        store.edit { it.remove(historyKey) }
    }

    // ── Playback queue persistence ─────────────────────────────────────────────

    suspend fun saveQueue(queue: SavedQueue) {
        store.edit { it[queueKey] = json.encodeToString(queue) }
    }

    suspend fun loadQueue(): SavedQueue? =
        store.data.first()[queueKey]?.let { runCatching { json.decodeFromString<SavedQueue>(it) }.getOrNull() }
}
