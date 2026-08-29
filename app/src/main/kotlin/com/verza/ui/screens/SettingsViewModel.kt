package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.ui.expressive.CoverShapeMode
import com.verza.data.IconVariant
import com.verza.data.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import com.verza.data.DownloadStore
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
    private val updates: UpdateRepository,
    private val iconVariant: IconVariant,
    private val downloads: DownloadStore,
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
    /** Where downloads are written. Blank = app-private storage. */
    val downloadTree: StateFlow<String> = prefs.downloadTreeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val coverShape: StateFlow<CoverShapeMode> = prefs.coverShapeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, CoverShapeMode.SHUFFLE)

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

    // ── app updates ─────────────────────────────────────────────────────────
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState

    sealed interface UpdateState {
        data object Idle : UpdateState
        data object Checking : UpdateState
        data object UpToDate : UpdateState
        data class Available(val release: UpdateRepository.Release) : UpdateState
        data class Downloading(val progress: Float) : UpdateState
        data class Ready(val file: java.io.File, val version: String) : UpdateState
        data class Failed(val message: String) : UpdateState
    }

    fun checkForUpdate() {
        if (_updateState.value is UpdateState.Checking) return
        _updateState.value = UpdateState.Checking
        viewModelScope.launch {
            val release = updates.checkForUpdate()
            _updateState.value =
                if (release == null) UpdateState.UpToDate else UpdateState.Available(release)
        }
    }

    fun downloadUpdate(release: UpdateRepository.Release) {
        _updateState.value = UpdateState.Downloading(0f)
        viewModelScope.launch {
            val file = updates.download(release) { p ->
                _updateState.value = UpdateState.Downloading(p)
            }
            _updateState.value = if (file == null) {
                UpdateState.Failed("Download failed")
            } else {
                UpdateState.Ready(file, release.version)
            }
        }
    }

    fun installUpdate(file: java.io.File) {
        if (!updates.install(file)) {
            _updateState.value = UpdateState.Failed("Android would not open the installer")
        }
    }

    val adaptiveIcon: StateFlow<Boolean> = prefs.adaptiveIconFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Turning it off puts the default icon back rather than leaving the last colour stuck on. */
    fun setAdaptiveIcon(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAdaptiveIcon(enabled)
            if (!enabled) iconVariant.reset()
        }
    }

    // ── notices ─────────────────────────────────────────────────────────────
    // Suspending accessors rather than flows: these are read once at launch to decide whether to
    // show a sheet, not observed for changes.
    suspend fun seenChangelogVersion(): String = prefs.seenChangelogVersion()
    suspend fun setSeenChangelogVersion(v: String) = prefs.setSeenChangelogVersion(v)
    suspend fun dismissedUpdateVersion(): String = prefs.dismissedUpdateVersion()
    fun setDismissedUpdateVersion(v: String) { viewModelScope.launch { prefs.setDismissedUpdateVersion(v) } }
    suspend fun notesFor(version: String): String? = updates.notesFor(version)

    /** A check that does not touch the Settings row state — used by the launch-time offer. */
    suspend fun checkForUpdateQuietly(): UpdateRepository.Release? = updates.checkForUpdate()

    fun setCoverShape(mode: CoverShapeMode) {
        viewModelScope.launch { prefs.setCoverShape(mode) }
    }

    fun setDownloadTree(treeUri: String) {
        viewModelScope.launch { prefs.setDownloadTree(treeUri) }
    }

    fun downloadFolderLabel(treeUri: String): String = downloads.folderLabel(treeUri)

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
