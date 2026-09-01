package com.verza.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.verza.di.ApplicationScope
import com.verza.innertube.AudioQuality
import com.verza.innertube.InnerTube
import com.verza.ui.expressive.ColorFlavour
import com.verza.ui.expressive.CoverShapeMode
import com.verza.ui.theme.GlowColorPreset
import com.verza.ui.theme.GlowIntensity
import com.verza.ui.theme.GlowStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.distinctUntilChanged
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

    // Legacy plaintext cookie key (pre-0.4.1). Migrated to the encrypted key on first launch.
    private val cookieKey = stringPreferencesKey("account_cookie")
    // Cookie ciphertext (AES/GCM via the Android Keystore — see CookieCrypto).
    private val cookieEncKey = stringPreferencesKey("account_cookie_enc")
    private val historyKey = stringPreferencesKey("search_history")
    private val queueKey = stringPreferencesKey("saved_queue")
    // Tracks Discovery radio has already offered — excluded next time so it never repeats itself.
    private val discoveryServedKey = stringPreferencesKey("discovery_served")
    private val audioQualityKey = stringPreferencesKey("audio_quality")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val startScreenKey = stringPreferencesKey("start_screen")
    private val resumeOnOpenKey = booleanPreferencesKey("resume_on_open")
    private val skipSilenceKey = booleanPreferencesKey("skip_silence")
    private val saveSearchHistoryKey = booleanPreferencesKey("save_search_history")
    private val albumArtMotionKey = booleanPreferencesKey("album_art_motion")
    // Drives the spectrum seek bar and the music haptics. Named for the glow it used to
    // feed; the key is kept as-is so existing installs do not lose the setting.
    private val glowReactiveKey = booleanPreferencesKey("glow_reactive")
    private val hapticsKey = booleanPreferencesKey("music_haptics")
    private val gentleStartKey = booleanPreferencesKey("gentle_start")
    // SAF tree Uri for the folder downloads are written to. Blank = app-private storage, which is
    // where they used to go unconditionally: invisible to other apps and wiped on uninstall.
    private val downloadTreeKey = stringPreferencesKey("download_tree_uri")
    // Which silhouette the album art is masked with. Defaults to SHUFFLE — the shape changing per
    // track is the point of having it at all.
    private val coverShapeKey = stringPreferencesKey("cover_shape_mode")
    private val colorFlavourKey = stringPreferencesKey("color_flavour")
    // Whether the launcher icon follows the cover colour. Off by default — switching it has visible
    // side effects on the home screen, so it should be a choice rather than a surprise.
    // The version whose changelog has been shown, and the version the listener said "not now" to.
    // Both are versions rather than flags, so a later release asks again by itself.
    private val seenChangelogKey = stringPreferencesKey("seen_changelog_version")
    private val dismissedUpdateKey = stringPreferencesKey("dismissed_update_version")
    // ── Sound suite (equaliser / bass / loudness) ──────────────────────────────
    private val eqEnabledKey = booleanPreferencesKey("eq_enabled")
    private val eqBandsKey = stringPreferencesKey("eq_band_levels") // JSON List<Int> (millibels)
    private val eqPresetKey = stringPreferencesKey("eq_preset")      // EqPreset.name, or absent = Custom
    private val bassStrengthKey = intPreferencesKey("bass_strength") // 0..1000
    private val loudnessKey = booleanPreferencesKey("loudness_enabled")


    // Prefer the encrypted cookie; fall back to any not-yet-migrated legacy plaintext value.
    //
    // Keyed on the ciphertext and memoised, because the map runs on every emission of the whole
    // Preferences object and an AES-GCM unwrap is a Keystore IPC round trip. Five collectors
    // subscribe to this, and playback rewrites the saved queue into the same DataStore every ten
    // seconds — which was about thirty Keystore decryptions a minute for a value that had not
    // changed since sign-in.
    @Volatile private var cookieCache: Pair<String, String?>? = null

    val cookieFlow: Flow<String?> = store.data
        .map { prefs -> Pair(prefs[cookieEncKey], prefs[cookieKey]) }
        .distinctUntilChanged()
        .map { (encrypted, legacy) ->
            if (encrypted == null) return@map legacy
            cookieCache?.takeIf { it.first == encrypted }?.second
                ?: CookieCrypto.decrypt(encrypted).also { cookieCache = encrypted to it }
        }

    val audioQualityFlow: Flow<AudioQuality> = store.data.map { prefs ->
        prefs[audioQualityKey]?.let { runCatching { AudioQuality.valueOf(it) }.getOrNull() } ?: AudioQuality.HIGH
    }

    val searchHistoryFlow: Flow<List<String>> = store.data.map { prefs ->
        prefs[historyKey]?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
    }

    /** False on a fresh install; set to true the first time the user finishes the onboarding flow. */
    val onboardingCompletedFlow: Flow<Boolean> = store.data.map { it[onboardingCompletedKey] ?: false }

    /**
     * Whether the background glow animates with the audio FFT signal. Independent from
     * [glowEnabledFlow] — reactivity is only visible when the glow itself is enabled.
     * Permission gating (RECORD_AUDIO) is handled at the UI layer; this flag just stores
     * the user's stated preference.
     */
    // Drives the spectrum seek bar (and the beat haptics). On by default: it is part of the player's
    // look now rather than an optional effect, and it degrades to a flat bar without the permission.
    val glowReactiveFlow: Flow<Boolean> = store.data.map { it[glowReactiveKey] ?: true }

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

    val downloadTreeFlow: Flow<String> = store.data.map { it[downloadTreeKey].orEmpty() }

    val coverShapeFlow: Flow<CoverShapeMode> =
        store.data.map { CoverShapeMode.fromName(it[coverShapeKey]) }

    /** How hard to push the cover's colours. Replaced the fixed palettes. */
    val colorFlavourFlow: Flow<ColorFlavour> =
        store.data.map { ColorFlavour.fromName(it[colorFlavourKey]) }

    suspend fun seenChangelogVersion(): String = store.data.first()[seenChangelogKey].orEmpty()

    suspend fun setSeenChangelogVersion(version: String) {
        store.edit { it[seenChangelogKey] = version }
    }

    suspend fun dismissedUpdateVersion(): String = store.data.first()[dismissedUpdateKey].orEmpty()

    suspend fun setDismissedUpdateVersion(version: String) {
        store.edit { it[dismissedUpdateKey] = version }
    }

    /** Subtle vibration synced to the music's bass. Reads playback audio only (same as the glow). */
    val hapticsEnabledFlow: Flow<Boolean> = store.data.map { it[hapticsKey] ?: false }

    /** Ease the volume up over a couple of seconds when resuming playback — a soft "sunrise" start. */
    val gentleStartFlow: Flow<Boolean> = store.data.map { it[gentleStartKey] ?: false }

    // ── Sound suite ─────────────────────────────────────────────────────────────
    /** Master switch for the graphic equaliser (band sliders only apply when on). */
    val eqEnabledFlow: Flow<Boolean> = store.data.map { it[eqEnabledKey] ?: false }
    /** Per-band gains in millibels; empty until the user adjusts a band. */
    val eqBandsFlow: Flow<List<Int>> = store.data.map { prefs ->
        prefs[eqBandsKey]?.let { runCatching { json.decodeFromString<List<Int>>(it) }.getOrNull() } ?: emptyList()
    }
    /** Name of the active EQ preset, or null when the bands have been hand-tuned ("Custom"). */
    val eqPresetFlow: Flow<String?> = store.data.map { it[eqPresetKey] }
    /** Bass-boost strength 0..1000 (0 = off), independent of the equaliser switch. */
    val bassStrengthFlow: Flow<Int> = store.data.map { it[bassStrengthKey] ?: 0 }
    /** Volume leveling — lifts quiet tracks toward a steadier perceived loudness. */
    val loudnessEnabledFlow: Flow<Boolean> = store.data.map { it[loudnessKey] ?: false }

    init {
        // One-time migration: if an old plaintext cookie exists, re-store it encrypted and drop
        // the plaintext copy. Runs before/independently of the collectors below.
        scope.launch {
            val prefs = store.data.first()
            val legacy = prefs[cookieKey]
            if (!legacy.isNullOrBlank() && prefs[cookieEncKey] == null) {
                runCatching { CookieCrypto.encrypt(legacy) }.getOrNull()?.let { enc ->
                    store.edit {
                        it[cookieEncKey] = enc
                        it.remove(cookieKey)
                    }
                }
            }
        }
        // Mirror the persisted cookie + audio quality into the InnerTube client for the app lifetime.
        scope.launch { cookieFlow.collect { InnerTube.cookie = it } }
        scope.launch { audioQualityFlow.collect { InnerTube.audioQuality = it } }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        store.edit { it[audioQualityKey] = quality.name }
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

    /** [treeUri] comes from the system folder picker; blank resets to app-private storage. */
    suspend fun setDownloadTree(treeUri: String) {
        store.edit { it[downloadTreeKey] = treeUri }
    }

    suspend fun downloadTree(): String = store.data.first()[downloadTreeKey].orEmpty()

    suspend fun setCoverShape(mode: CoverShapeMode) {
        store.edit { it[coverShapeKey] = mode.name }
    }

    suspend fun setColorFlavour(flavour: ColorFlavour) {
        store.edit { it[colorFlavourKey] = flavour.name }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        store.edit { it[hapticsKey] = enabled }
    }

    suspend fun setGentleStart(enabled: Boolean) {
        store.edit { it[gentleStartKey] = enabled }
    }

    suspend fun setEqEnabled(enabled: Boolean) {
        store.edit { it[eqEnabledKey] = enabled }
    }

    suspend fun setEqBands(levelsMb: List<Int>) {
        store.edit { it[eqBandsKey] = json.encodeToString(levelsMb) }
    }

    /** Records the active preset name, or clears it (null) to mark the bands as hand-tuned. */
    suspend fun setEqPreset(name: String?) {
        store.edit { prefs ->
            if (name == null) prefs.remove(eqPresetKey) else prefs[eqPresetKey] = name
        }
    }

    suspend fun setBassStrength(strength: Int) {
        store.edit { it[bassStrengthKey] = strength.coerceIn(0, 1000) }
    }

    suspend fun setLoudnessEnabled(enabled: Boolean) {
        store.edit { it[loudnessKey] = enabled }
    }

    suspend fun setCookie(cookie: String?) {
        // Always clear any legacy plaintext value; store the new cookie encrypted.
        val encrypted = cookie?.takeIf { it.isNotBlank() }?.let { runCatching { CookieCrypto.encrypt(it) }.getOrNull() }
        store.edit { prefs ->
            prefs.remove(cookieKey)
            if (encrypted == null) prefs.remove(cookieEncKey) else prefs[cookieEncKey] = encrypted
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

    // ── Discovery radio memory ─────────────────────────────────────────────
    // YouTube's radio for a given song is deterministic, so without a record of what we've already
    // offered, a second Discovery run returns the same tracks. Keep the ids (FIFO-capped) and skip
    // them next time; that memory is what makes each run genuinely new.

    private val discoveryServedCap = 800

    suspend fun discoveryServed(): Set<String> {
        val raw = store.data.first()[discoveryServedKey] ?: return emptySet()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList()).toSet()
    }

    suspend fun addDiscoveryServed(ids: List<String>) {
        if (ids.isEmpty()) return
        store.edit { prefs ->
            val current = prefs[discoveryServedKey]
                ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() } ?: emptyList()
            val updated = (current + ids.filterNot { it in current }).takeLast(discoveryServedCap)
            prefs[discoveryServedKey] = json.encodeToString(updated)
        }
    }

    /** Forget what Discovery has offered — used when the well runs dry so it can start over. */
    suspend fun clearDiscoveryServed() {
        store.edit { it.remove(discoveryServedKey) }
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
