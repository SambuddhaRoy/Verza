package com.verza.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.verza.data.sources.Source
import com.verza.data.youtube.Quality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

// The old app's settings file, so an update keeps every setting. Keys are the old names.
private val Context.store by preferencesDataStore(name = "verza_settings")

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

/** One setting: read as a flow, read once, or written. */
open class Setting<T>(private val context: Context, private val key: Preferences.Key<T>, private val default: T) {
    val flow: Flow<T> = context.store.data.map { it[key] ?: default }
    suspend fun get(): T = flow.first()
    suspend fun set(value: T) { context.store.edit { it[key] = value } }
}

/** A setting stored as JSON in a string, the way the old app kept lists. Unreadable reads as [default]. */
class JsonSetting<T>(context: Context, name: String, private val serializer: KSerializer<T>, private val default: T) {
    private val raw = Setting(context, stringPreferencesKey(name), "")
    val flow: Flow<T> = raw.flow.map { s -> if (s.isEmpty()) default else runCatching { json.decodeFromString(serializer, s) }.getOrDefault(default) }
    suspend fun get(): T = flow.first()
    suspend fun set(value: T) = raw.set(json.encodeToString(serializer, value))
}

class Settings(private val context: Context) {
    private fun bool(name: String, default: Boolean) = Setting(context, booleanPreferencesKey(name), default)
    private fun int(name: String, default: Int) = Setting(context, intPreferencesKey(name), default)

    val onboarded = bool("onboarding_completed", false)
    val resumeOnOpen = bool("resume_on_open", true)
    val skipSilence = bool("skip_silence", false)
    /** Fade into and out of each song, in seconds; 0 is off. */
    val fadeSeconds = int("crossfade_seconds", 0)

    val quality = Setting(context, stringPreferencesKey("audio_quality"), Quality.HIGH.name)

    val sources = JsonSetting(context, "addon_sources", ListSerializer(Source.serializer()), emptyList())
    val sourcesOnMobileData = bool("addons_on_mobile_data", false)

    val saveSearchHistory = bool("save_search_history", true)
    val searchHistory = JsonSetting(context, "search_history", ListSerializer(String.serializer()), emptyList())

    val savedQueue = JsonSetting(context, "saved_queue", SavedQueue.serializer(), SavedQueue())
    /** A folder the listener chose for downloads (a tree URI), or empty for Music/Verza. */
    val downloadFolder = Setting(context, stringPreferencesKey("download_tree_uri"), "")
}

/** The queue as it was, so the app reopens where it left off. The old app's format. */
@kotlinx.serialization.Serializable
data class SavedQueue(val tracks: List<SavedTrack> = emptyList(), val index: Int = 0, val positionMs: Long = 0)

@kotlinx.serialization.Serializable
data class SavedTrack(val videoId: String, val title: String, val artist: String, val artworkUrl: String? = null, val durationMs: Long = 0) {
    fun toSong() = Song(videoId, title, artist, art = artworkUrl, durationMs = durationMs)
}

fun Song.toSaved() = SavedTrack(id, title, artist, art, durationMs)
