package com.verza

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verza.audio.AudioVisualizer
import com.verza.audio.VisualizerSignal
import com.verza.playback.PlaybackViewModel
import com.verza.ui.navigation.Screen
import com.verza.ui.navigation.VerzaNavigation
import com.verza.ui.screens.SettingsViewModel
import com.verza.ui.theme.GlowBackground
import com.verza.ui.theme.GlowColorPreset
import com.verza.ui.theme.GlowTriad
import com.verza.ui.theme.VerzaTheme
import com.verza.ui.theme.deriveGlowTriad
import com.verza.ui.theme.extractAlbumTriad
import com.verza.ui.theme.resolveColor
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Gate for the system splash screen: stays on screen until we know whether onboarding
    // has been completed. Plain Boolean field rather than a Compose state since the splash
    // screen's keep-on-screen lambda is invoked on the main thread outside the composition.
    private var splashReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() MUST run before super.onCreate so the OS knows to keep the
        // Theme.Verza.Starting splash visible past Activity initialisation.
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !splashReady }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Ask for notification permission so the media-playback foreground service
            // can show its notification on Android 13+.
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { /* playback works regardless; the notification just won't show if denied */ }
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val theme by settingsViewModel.theme.collectAsStateWithLifecycle()
            val glowEnabled by settingsViewModel.glowEnabled.collectAsStateWithLifecycle()
            val glowColor by settingsViewModel.glowColor.collectAsStateWithLifecycle()
            val glowIntensity by settingsViewModel.glowIntensity.collectAsStateWithLifecycle()
            val glowReactive by settingsViewModel.glowReactive.collectAsStateWithLifecycle()
            val onboardingCompleted by settingsViewModel.onboardingCompleted.collectAsStateWithLifecycle()
            val startScreen by settingsViewModel.startScreen.collectAsStateWithLifecycle()

            // ── Visualizer lifecycle ─────────────────────────────────────────────
            // PlaybackViewModel here just for audioSessionId + isPlaying — same VM is used by the
            // rest of the nav graph, and Hilt scopes it to the Activity so we share the instance.
            val playbackViewModel: PlaybackViewModel = hiltViewModel()
            val audioSessionId by playbackViewModel.audioSessionId.collectAsStateWithLifecycle()
            val playbackState by playbackViewModel.playbackState.collectAsStateWithLifecycle()
            val artworkOverride by playbackViewModel.currentArtworkOverride.collectAsStateWithLifecycle()
            val isPlaying = playbackState.isPlaying

            // Current cover URL — prefer the iTunes-resolved high-res art, fall back to the
            // media item's own artwork. Feeds the "From album art" adaptive glow.
            val artworkUrl = artworkOverride
                ?: playbackState.currentItem?.mediaMetadata?.artworkUri?.toString()

            // The visualizer is only active when all four conditions hold:
            //   1. User enabled glow reactivity in Settings
            //   2. RECORD_AUDIO permission is granted (re-checked each recomposition so a
            //      user-granted permission lights up the feature without an app restart)
            //   3. ExoPlayer has reported a non-zero audio session id
            //   4. Playback is currently active
            val context = LocalContext.current
            val hasAudioPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val shouldVisualize = glowReactive && hasAudioPermission &&
                audioSessionId != 0 && isPlaying

            // The signal flow is owned at the Activity composition scope so the GlowBackground
            // can read it. The DisposableEffect below owns the engine instance and the collector
            // coroutine, both keyed on (shouldVisualize, audioSessionId).
            val visualizerSignalFlow = remember { MutableStateFlow(VisualizerSignal()) }
            val scope = rememberCoroutineScope()
            DisposableEffect(shouldVisualize, audioSessionId) {
                val engine = if (shouldVisualize) AudioVisualizer(audioSessionId) else null
                engine?.start()
                val collectorJob = engine?.let { eng ->
                    scope.launch { eng.signal.collect { visualizerSignalFlow.value = it } }
                }
                onDispose {
                    collectorJob?.cancel()
                    engine?.stop()
                    visualizerSignalFlow.value = VisualizerSignal()
                }
            }
            val visualizerSignal by visualizerSignalFlow.collectAsStateWithLifecycle()
            // The instant DataStore tells us the flag value, lower the splash-screen gate so
            // the OS animates out and Compose takes over with the Boot route as start dest.
            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted != null) splashReady = true
            }

            VerzaTheme(theme = theme) {
                // Resolve the glow colour triad inside the theme scope so resolveColor() and the
                // album-art fallback can read the active colour scheme.
                val seed = glowColor.resolveColor()
                // Album-art extraction runs off the main thread via produceState, re-keyed on the
                // cover URL and the selected preset. Null while loading / unavailable.
                val albumTriad by produceState<GlowTriad?>(null, glowColor, artworkUrl) {
                    value = if (glowColor == GlowColorPreset.ALBUM_ART && !artworkUrl.isNullOrBlank())
                        extractAlbumTriad(context, artworkUrl!!)
                    else null
                }
                // Non-album presets (and album fallback) derive a vibrant hue-spread triad from a
                // single seed — this is also what de-monochromes the Material You glow.
                val glowTriad = albumTriad ?: deriveGlowTriad(seed)

                GlowBackground(
                    enabled = glowEnabled,
                    triad = glowTriad,
                    intensity = glowIntensity,
                    signal = if (shouldVisualize) visualizerSignal else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    // Hold off rendering the NavHost until DataStore tells us whether onboarding
                    // has been completed. The system splash is still on screen during this gap
                    // (held by setKeepOnScreenCondition), so the user sees no blank frame.
                    val completed = onboardingCompleted
                    if (completed != null) {
                        // Always start at Boot. Boot's onFinished callback decides Onboarding
                        // vs Home so the post-boot landing reflects the onboarding flag at the
                        // moment the animation ends (which catches edge cases where DataStore
                        // updates mid-animation).
                        VerzaNavigation(
                            modifier = Modifier.fillMaxSize(),
                            startDestination = Screen.Boot.route,
                            // After boot: returning users land on their chosen start screen;
                            // first-timers go through onboarding.
                            postBootDestination = if (completed) startScreen.route else Screen.Onboarding.route,
                        )
                    }
                }
            }
        }
    }
}
