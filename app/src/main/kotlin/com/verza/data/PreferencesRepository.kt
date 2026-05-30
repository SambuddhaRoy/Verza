package com.verza.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.verza.di.ApplicationScope
import com.verza.innertube.AudioQuality
import com.verza.innertube.InnerTube
import com.verza.ui.theme.GlowColorPreset
import com.verza.ui.theme.GlowIntensity
import com.verza.ui.theme.VerzaTheme
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

private val Context.dataStore by preferencesDataStore(name = "verza_settings")

/** Which tab the app opens to after launch (post-boot / post-onboarding). */
enum class StartScreen(val route: String, val label: String) {
    HOME("home", "Home"),
    SEARCH("search", "Search"),
    LIBRARY("library", "Library"),
}

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
    private val glowEnabledKey = booleanPreferencesKey("glow_enabled")
    private val glowColorKey = stringPreferencesKey("glow_color_preset")
    private val glowIntensityKey = stringPreferencesKey("glow_intensity")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val glowReactiveKey = booleanPreferencesKey("glow_reactive")
    private val startScreenKey = stringPreferencesKey("start_screen")
    private val resumeOnOpenKey = booleanPreferencesKey("resume_on_open")
    private val skipSilenceKey = booleanPreferencesKey("skip_silence")
    private val saveSearchHistoryKey = booleanPreferencesKey("save_search_history")
    private val albumArtMotionKey = booleanPreferencesKey("album_art_motion")

    val themeFlow: Flow<VerzaTheme> = store.data.map { prefs ->
        // Default to Material You (Dynamic). On pre-Android-12 devices the theme layer falls back
        // to the Atelier dark scheme automatically, so this is a safe default everywhere.
        prefs[themeKey]?.let { runCatching { VerzaTheme.valueOf(it) }.getOrNull() } ?: VerzaTheme.DYNAMIC
    }

    val cookieFlow: Flow<String?> = store.data.map { it[cookieKey] }

    val audioQualityFlow: Flow<AudioQuality> = store.data.map { prefs ->
        prefs[audioQualityKey]?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() } ?: AudioQuality.HIGH
    }

    val searchHistoryFlow: Flow<List<String>> = store.data.map { prefs ->
        prefs[historyKey]?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
    }

    // ── Background glow (dark themes only; the UI is rendered regardless and short-circuits in light themes)
    val glowEnabledFlow: Flow<Boolean> = store.data.map { it[glowEnabledKey] ?: true }
    val glowColorFlow: Flow<GlowColorPreset> = store.data.map { prefs ->
        // Default the glow to adapt to album-cover colours.
        prefs[glowColorKey]?.let { runCatching { GlowColorPreset.valueOf(it) }.getOrNull() } ?: GlowColorPreset.ALBUM_ART
    }
    val glowIntensityFlow: Flow<GlowIntensity> = store.data.map { prefs ->
        prefs[glowIntensityKey]?.let { runCatching { GlowIntensity.valueOf(it) }.getOrNull() } ?: GlowIntensity.MEDIUM
    }

    /** False on a fresh install; set to true the first time the user finishes the onboarding flow. */
    val onboardingCompletedFlow: Flow<Boolean> = store.data.map { it[onboardingCompletedKey] ?: false }

    /**
     * Whether the background glow animates with the audio FFT signal. Independent from
     * [glowEnabledFlow] — reactivity is only visible when the glow itself is enabled.
     * Permission gating (RECORD_AUDIO) is handled at the UI layer; this flag just stores
     * the user's stated preference.
     */
    val glowReactiveFlow: Flow<Boolean> = store.data.map { it[glowReactiveKey] ?: false }

    // ── Behaviour / customization ───────────────────────────────────────────────
    val startScreenFlow: Flow<StartScreen> = store.data.map { prefs ->
        prefs[startScreenKey]?.let { runCatching { StartScreen.valueOf(it) }.getOrNull() } ?: StartScreen.HOME
    }
    /** Auto-resume the saved queue on app open (default off — most users expect a quiet launch). */
    val resumeOnOpenFlow: Flow<Boolean> = store.data.map { it[resumeOnOpenKey] ?: false }
    /** Trim silent passages during playback (ExoPlayer skip-silence). */
    val skipSilenceFlow: Flow<Boolean> = store.data.map { it[skipSilenceKey] ?: false }
    /** Whether new searches are remembered. Default on. */
    val saveSearchHistoryFlow: Flow<Boolean> = store.data.map { it[saveSearchHistoryKey] ?: true }
    /** Whether the Now Playing album art gently "breathes" while playing. Default on. */
    val albumArtMotionFlow: Flow<Boolean> = store.data.map { it[albumArtMotionKey] ?: true }

    init {
        // Mirror the persisted cookie + audio quality into the InnerTube client for the app lifetime.
        scope.launch { cookieFlow.collect { InnerTube.cookie = it } }
        scope.launch { audioQualityFlow.collect { InnerTube.audioQuality = it } }
    }

    suspend fun setTheme(theme: VerzaTheme) {
        store.edit { it[themeKey] = theme.name }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        store.edit { it[audioQualityKey] = quality.name }
    }

    suspend fun setGlowEnabled(enabled: Boolean) {
        store.edit { it[glowEnabledKey] = enabled }
    }

    suspend fun setGlowColor(preset: GlowColorPreset) {
        store.edit { it[glowColorKey] = preset.name }
    }

    suspend fun setGlowIntensity(intensity: GlowIntensity) {
        store.edit { it[glowIntensityKey] = intensity.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        store.edit { it[onboardingCompletedKey] = completed }
    }

    suspend fun setGlowReactive(reactive: Boolean) {
        store.edit { it[glowReactiveKey] = reactive }
    }

    suspend fun setStartScreen(screen: StartScreen) {
        store.edit { it[startScreenKey] = screen.name }
    }

    suspend fun setResumeOnOpen(enabled: Boolean) {
        store.edit { it[resumeOnOpenKey] = enabled }
    }

    suspend fun setSkipSilence(enabled: Boolean) {
        store.edit { it[skipSilenceKey] = enabled }
    }

    suspend fun setSaveSearchHistory(enabled: Boolean) {
        store.edit { it[saveSearchHistoryKey] = enabled }
    }

    suspend fun setAlbumArtMotion(enabled: Boolean) {
        store.edit { it[albumArtMotionKey] = enabled }
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
            // Respect the "save search history" preference — no-op when the user has turned it off.
            if (prefs[saveSearchHistoryKey] == false) return@edit
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
