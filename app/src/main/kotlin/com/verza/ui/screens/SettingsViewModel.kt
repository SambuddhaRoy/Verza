package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.ImportSummary
import com.verza.data.LibraryBackupRepository
import com.verza.data.PreferencesRepository
import com.verza.data.StartScreen
import com.verza.data.StatsRepository
import com.verza.innertube.AudioQuality
import com.verza.ui.theme.GlowColorPreset
import com.verza.ui.theme.GlowIntensity
import com.verza.ui.theme.GlowStyle
import com.verza.ui.theme.VerzaTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val stats: StatsRepository,
    private val backup: LibraryBackupRepository,
) : ViewModel() {

    val theme: StateFlow<VerzaTheme> = prefs.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, VerzaTheme.DYNAMIC)

    val isSignedIn: StateFlow<Boolean> = prefs.cookieFlow
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val audioQuality: StateFlow<AudioQuality> = prefs.audioQualityFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AudioQuality.HIGH)

    val glowEnabled: StateFlow<Boolean> = prefs.glowEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val glowColor: StateFlow<GlowColorPreset> = prefs.glowColorFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GlowColorPreset.ALBUM_ART)

    val glowIntensity: StateFlow<GlowIntensity> = prefs.glowIntensityFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GlowIntensity.MEDIUM)

    val glowStyle: StateFlow<GlowStyle> = prefs.glowStyleFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GlowStyle.FLUID)

    val glowChaos: StateFlow<Float> = prefs.glowChaosFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.4f)

    /** Null while DataStore is still loading; non-null once we know whether onboarding has run. */
    val onboardingCompleted: StateFlow<Boolean?> = prefs.onboardingCompletedFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val glowReactive: StateFlow<Boolean> = prefs.glowReactiveFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Behaviour / customization ───────────────────────────────────────────────
    val startScreen: StateFlow<StartScreen> = prefs.startScreenFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, StartScreen.HOME)
    val resumeOnOpen: StateFlow<Boolean> = prefs.resumeOnOpenFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val skipSilence: StateFlow<Boolean> = prefs.skipSilenceFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val saveSearchHistory: StateFlow<Boolean> = prefs.saveSearchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val albumArtMotion: StateFlow<Boolean> = prefs.albumArtMotionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val sleeveMode: StateFlow<Boolean> = prefs.sleeveModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val hapticsEnabled: StateFlow<Boolean> = prefs.hapticsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val gentleStart: StateFlow<Boolean> = prefs.gentleStartFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setTheme(theme: VerzaTheme) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch { prefs.setAudioQuality(quality) }
    }

    fun setGlowEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setGlowEnabled(enabled) }
    }

    fun setGlowColor(preset: GlowColorPreset) {
        viewModelScope.launch { prefs.setGlowColor(preset) }
    }

    fun setGlowIntensity(intensity: GlowIntensity) {
        viewModelScope.launch { prefs.setGlowIntensity(intensity) }
    }

    fun setGlowStyle(style: GlowStyle) {
        viewModelScope.launch { prefs.setGlowStyle(style) }
    }

    fun setGlowChaos(value: Float) {
        viewModelScope.launch { prefs.setGlowChaos(value) }
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch { prefs.setOnboardingCompleted(true) }
    }

    fun setGlowReactive(reactive: Boolean) {
        viewModelScope.launch { prefs.setGlowReactive(reactive) }
    }

    fun setStartScreen(screen: StartScreen) {
        viewModelScope.launch { prefs.setStartScreen(screen) }
    }

    fun setResumeOnOpen(enabled: Boolean) {
        viewModelScope.launch { prefs.setResumeOnOpen(enabled) }
    }

    fun setSkipSilence(enabled: Boolean) {
        viewModelScope.launch { prefs.setSkipSilence(enabled) }
    }

    fun setSaveSearchHistory(enabled: Boolean) {
        viewModelScope.launch { prefs.setSaveSearchHistory(enabled) }
    }

    fun setAlbumArtMotion(enabled: Boolean) {
        viewModelScope.launch { prefs.setAlbumArtMotion(enabled) }
    }

    fun setSleeveMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setSleeveMode(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { prefs.setHapticsEnabled(enabled) }
    }

    fun setGentleStart(enabled: Boolean) {
        viewModelScope.launch { prefs.setGentleStart(enabled) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { prefs.clearSearchHistory() }
    }

    fun resetListeningStats() {
        viewModelScope.launch { stats.reset() }
    }

    // ── Library backup (export / import) ────────────────────────────────────────
    suspend fun exportLibraryJson(): String = backup.exportJson()
    suspend fun importLibraryJson(text: String): ImportSummary = backup.importJson(text)

    fun onSignedIn(cookie: String) {
        viewModelScope.launch { prefs.setCookie(cookie) }
    }

    fun signOut() {
        viewModelScope.launch { prefs.setCookie(null) }
    }
}
