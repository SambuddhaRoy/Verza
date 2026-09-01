package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.ui.expressive.AccentSource
import com.verza.ui.expressive.ColorFlavour
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
    private val downloads: DownloadStore,
) : ViewModel() {

    val isSignedIn: StateFlow<Boolean> = prefs.cookieFlow
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val audioQuality: StateFlow<AudioQuality> = prefs.audioQualityFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AudioQuality.HIGH)

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
    val hapticsEnabled: StateFlow<Boolean> = prefs.hapticsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val gentleStart: StateFlow<Boolean> = prefs.gentleStartFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    /** Where downloads are written. Blank = app-private storage. */
    val downloadTree: StateFlow<String> = prefs.downloadTreeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val colorFlavour: StateFlow<ColorFlavour> = prefs.colorFlavourFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ColorFlavour.SIGNATURE)
    val accentSource: StateFlow<AccentSource> = prefs.accentSourceFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentSource.COMPLEMENT)
    val crossfadeSeconds: StateFlow<Int> = prefs.crossfadeSecondsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setColorFlavour(flavour: ColorFlavour) {
        viewModelScope.launch { prefs.setColorFlavour(flavour) }
    }

    fun setAccentSource(source: AccentSource) {
        viewModelScope.launch { prefs.setAccentSource(source) }
    }

    fun setCrossfadeSeconds(seconds: Int) {
        viewModelScope.launch { prefs.setCrossfadeSeconds(seconds) }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch { prefs.setAudioQuality(quality) }
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
