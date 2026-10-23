package com.verza.ui.screens

import com.verza.ui.theme.DesignScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.ui.expressive.AccentSource
import com.verza.ui.expressive.ColorFlavour
import com.verza.data.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import com.verza.data.DownloadStore
import com.verza.data.ListenBrainzRepository
import com.verza.data.MixKind
import com.verza.data.MixesRepository
import com.verza.data.ImportSummary
import com.verza.data.LibraryBackupRepository
import com.verza.data.PreferencesRepository
import com.verza.data.StartScreen
import com.verza.data.StatsRepository
import com.verza.innertube.AudioQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val stats: StatsRepository,
    private val backup: LibraryBackupRepository,
    private val updates: UpdateRepository,
    private val downloads: DownloadStore,
    private val listenBrainz: ListenBrainzRepository,
    private val mixes: MixesRepository,
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
    val washBeat: StateFlow<Boolean> = prefs.washBeatFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val expressiveType: StateFlow<Boolean> = prefs.expressiveTypeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val hapticsEnabled: StateFlow<Boolean> = prefs.hapticsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val gentleStart: StateFlow<Boolean> = prefs.gentleStartFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    /** Where downloads are written. Blank = app-private storage. */
    val downloadTree: StateFlow<String> = prefs.downloadTreeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val designScheme: StateFlow<DesignScheme> = prefs.designSchemeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, DesignScheme.MATERIAL)
    val colorFlavour: StateFlow<ColorFlavour> = prefs.colorFlavourFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ColorFlavour.SIGNATURE)
    val accentSource: StateFlow<AccentSource> = prefs.accentSourceFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AccentSource.COMPLEMENT)
    val crossfadeSeconds: StateFlow<Int> = prefs.crossfadeSecondsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ── ListenBrainz ───────────────────────────────────────────────────────────
    val listenBrainzUser: StateFlow<String> = prefs.listenBrainzUserFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    /** What the connect dialog says, and whether it is busy. */
    data class LbState(val busy: Boolean = false, val message: String? = null)
    private val _lbState = MutableStateFlow(LbState())
    val lbState: StateFlow<LbState> = _lbState

    fun connectListenBrainz(input: String) {
        val user = input.trim()
        if (!ListenBrainzRepository.USERNAME.matches(user)) {
            _lbState.value = LbState(message = "That is not a ListenBrainz username.")
            return
        }
        _lbState.value = LbState(busy = true, message = "Finding your ListenBrainz playlists on YouTube Music. Up to a minute the first time.")
        viewModelScope.launch {
            _lbState.value = when (val check = listenBrainz.check(user)) {
                ListenBrainzRepository.Check.NoSuchUser -> LbState(message = "ListenBrainz has no user by that name.")
                ListenBrainzRepository.Check.Unreachable ->
                    LbState(message = "Could not reach ListenBrainz. Check your connection and try again.")
                is ListenBrainzRepository.Check.Found -> if (check.listens == 0L) {
                    LbState(message = "That account has no listens yet. Connect Spotify to it at listenbrainz.org, then try again.")
                } else {
                    prefs.setListenBrainzUser(user)
                    mixes.rebuildListenBrainz().join()
                    val titles = mixes.mixes.value.filter { it.kind == MixKind.LISTENBRAINZ }.map { it.title }
                    LbState(
                        message = if (titles.isNotEmpty()) "Connected as $user. On Home now: ${titles.joinToString(", ")}."
                        else "Connected as $user. ListenBrainz makes its weekly playlists on Mondays once it has a week of your listens; they will appear on Home then.",
                    )
                }
            }
        }
    }

    fun disconnectListenBrainz() {
        _lbState.value = LbState()
        viewModelScope.launch {
            prefs.setListenBrainzUser("")
            mixes.refreshListenBrainz()
        }
    }

    fun clearListenBrainzMessage() {
        if (!_lbState.value.busy) _lbState.value = LbState()
    }

    fun setDesignScheme(design: DesignScheme) {
        viewModelScope.launch { prefs.setDesignScheme(design) }
    }

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

    fun setWashBeat(enabled: Boolean) {
        viewModelScope.launch { prefs.setWashBeat(enabled) }
    }

    fun setExpressiveType(enabled: Boolean) {
        viewModelScope.launch { prefs.setExpressiveType(enabled) }
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

    private var prefetch: Pair<String, kotlinx.coroutines.Deferred<java.io.File?>>? = null

    /**
     * Downloads [release] quietly, so the offer can appear with the file already here and Update
     * goes straight to Android's installer. Once per version: the view model outlives a rotation,
     * and a second launch of the check joins the download already running. True when it is ready.
     */
    suspend fun prefetchUpdate(release: UpdateRepository.Release): Boolean {
        if (!updates.mayDownloadQuietly()) return false
        val job = prefetch?.takeIf { it.first == release.version }?.second
            ?: viewModelScope.async { updates.download(release) {} }.also { prefetch = release.version to it }
        return job.await() != null
    }

    /** Whether Android will let us start an install without sending the user to settings first. */
    fun canInstallUpdates(): Boolean = updates.canInstall()

    /** Opens the "install unknown apps" page for Verza. */
    fun requestInstallPermission() { updates.requestInstallPermission() }

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
