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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verza.audio.AudioVisualizer
import com.verza.audio.VisualizerSignal
import com.verza.playback.PlaybackViewModel
import com.verza.ui.navigation.Screen
import com.verza.ui.navigation.VerzaNavigation
import com.verza.ui.screens.SettingsViewModel
import com.verza.ui.theme.DefaultCoverColors
import com.verza.ui.theme.GlowBackground
import com.verza.ui.theme.GlowColorPreset
import com.verza.ui.theme.LocalCoverColors
import com.verza.ui.theme.VerzaTheme
import com.verza.ui.theme.coverColorScheme
import com.verza.ui.theme.deriveGlowTriad
import com.verza.ui.theme.extractCoverColors
import com.verza.ui.theme.resolveColor
import com.verza.ui.sleeve.LocalSleeveMode
import com.verza.ui.sleeve.grain
import com.verza.ui.sleeve.vignette
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
            val sleeveMode by settingsViewModel.sleeveMode.collectAsStateWithLifecycle()

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
            // The instant DataStore tells us the flag value, lower the splash-screen gate so
            // the OS animates out and Compose takes over with the Boot route as start dest.
            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted != null) splashReady = true
            }

            // Cover-derived palette (sampled from the current art) drives both the Adaptive theme's
            // colour scheme and every Sleeve surface. Extracted off the main thread; computed here
            // so it can feed VerzaTheme's scheme as well as LocalCoverColors below.
            val wantCover = sleeveMode || theme == VerzaTheme.ADAPTIVE || glowColor == GlowColorPreset.ALBUM_ART
            val coverColors by produceState(DefaultCoverColors, wantCover, artworkUrl) {
                value = if (wantCover && !artworkUrl.isNullOrBlank())
                    (extractCoverColors(context, artworkUrl!!) ?: DefaultCoverColors)
                else DefaultCoverColors
            }
            // Sleeve forces the cover scheme app-wide; the Adaptive theme uses it directly.
            val coverScheme = if (theme == VerzaTheme.ADAPTIVE || sleeveMode) coverColorScheme(coverColors) else null

            VerzaTheme(theme = theme, coverScheme = coverScheme, sleeve = sleeveMode) {
                val seed = glowColor.resolveColor()
                // The glow uses the cover accent whenever we're sampling it; otherwise the preset.
                val glowTriad = if (wantCover && !artworkUrl.isNullOrBlank())
                    deriveGlowTriad(coverColors.accent)
                else
                    deriveGlowTriad(seed)

                val navContent: @Composable () -> Unit = {
                    val completed = onboardingCompleted
                    if (completed != null) {
                        VerzaNavigation(
                            modifier = Modifier.fillMaxSize(),
                            startDestination = Screen.Boot.route,
                            postBootDestination = if (completed) startScreen.route else Screen.Onboarding.route,
                        )
                    }
                }

                CompositionLocalProvider(
                    LocalSleeveMode provides sleeveMode,
                    LocalCoverColors provides coverColors,
                ) {
                    // The reactive, album-coloured glow is the backdrop in every mode. In Sleeve it's
                    // forced on over a guaranteed-dark canvas so the editorial surfaces always read.
                    GlowBackground(
                        enabled = glowEnabled || sleeveMode,
                        triad = glowTriad,
                        intensity = glowIntensity,
                        signalFlow = if (shouldVisualize) visualizerSignalFlow else null,
                        forceDark = sleeveMode,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (sleeveMode) coverColors.bg else MaterialTheme.colorScheme.background)
                            // Sleeve wraps the whole app in a faint, even film grain and a soft
                            // edge vignette — the print/photographic finish from the UMBRA reference.
                            .then(if (sleeveMode) Modifier.vignette(0.30f).grain(0.05f) else Modifier),
                    ) { navContent() }
                }
            }
        }
    }
}
