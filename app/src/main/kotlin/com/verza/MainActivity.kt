package com.verza

import android.Manifest
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.draw.drawBehind
import com.verza.ui.expressive.ExpressiveMotion
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.verza.ui.expressive.ColorFlavour
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.verza.ui.expressive.LocalExpressiveColors
import androidx.compose.runtime.saveable.rememberSaveable
import javax.inject.Inject
import com.verza.ui.expressive.expressiveColorScheme
import com.verza.ui.expressive.expressiveColorsFrom
import com.verza.audio.HapticPlayer
import com.verza.audio.VisualizerSignal
import com.verza.data.SessionInbox
import com.verza.data.SharedSongInbox
import com.verza.playback.PlaybackViewModel
import com.verza.ui.navigation.Screen
import com.verza.ui.navigation.VerzaNavigation
import com.verza.ui.screens.SettingsViewModel
import com.verza.ui.theme.DefaultCoverColors
import com.verza.ui.theme.LocalArtworkColors
import com.verza.ui.theme.LocalAudioSignal
import com.verza.ui.theme.LocalCoverColors
import com.verza.ui.theme.VerzaTheme
import com.verza.ui.theme.coverColorsFromExpressive
import com.verza.ui.theme.extractCoverColors
import com.verza.ui.sleeve.LocalSleeveMode
import com.verza.ui.sleeve.grain
import com.verza.ui.sleeve.vignette
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Swaps the launcher icon alias when "Icon follows the music" is on. */
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
        // A verza://session/... link or a shared YouTube song may have launched us cold — hand
        // either to the playback owner.
        //
        // Only on a genuinely new start. getIntent() still returns the launching intent after a
        // rotation or a split-screen change, so re-running this replayed the shared song from the
        // top mid-listen and put the "Listen along?" dialog back on screen every time.
        if (savedInstanceState == null) {
            handleSessionIntent(intent)
            handleSharedYouTube(intent)
        }
        // Both bars fully transparent, and — the part that actually matters — contrast
        // enforcement off. Left on, Android paints its own translucent scrim behind the gesture
        // pill, and a solid white or black bar behind three-button navigation, which is why the
        // bottom of the screen stayed uncoloured no matter what the app drew.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
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
            val glowReactive by settingsViewModel.glowReactive.collectAsStateWithLifecycle()
            val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsStateWithLifecycle()
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
            // The visualizer feeds the spectrum seek bar and the music haptics, so either being on
            // (plus permission + an active session) is enough to run it.
            val shouldVisualize = (glowReactive || hapticsEnabled) && hasAudioPermission &&
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

            // ── Music-synced haptics ─────────────────────────────────────────────
            // Reuses the same visualizer signal: a rising-edge detector on the bass band fires a
            // short vibration tick on each kick (debounced). Active only when "Feel the beat" is
            // on (and the visualizer is therefore running).
            val hapticPlayer = remember { HapticPlayer(context) }
            DisposableEffect(Unit) { onDispose { hapticPlayer.stop() } }
            LaunchedEffect(hapticsEnabled, shouldVisualize) {
                if (!(hapticsEnabled && shouldVisualize)) return@LaunchedEffect
                var prevBass = 0f
                var lastPulse = 0L
                visualizerSignalFlow.collect { signal ->
                    val now = android.os.SystemClock.elapsedRealtime()
                    val bass = signal.bass
                    if (bass > 0.45f && prevBass <= 0.45f && now - lastPulse > 90L) {
                        hapticPlayer.pulse(bass)
                        lastPulse = now
                    }
                    prevBass = bass
                }
            }
            // The instant DataStore tells us the flag value, lower the splash-screen gate so
            // the OS animates out and Compose takes over with the Boot route as start dest.
            LaunchedEffect(onboardingCompleted) {
                if (onboardingCompleted != null) splashReady = true
            }

            // Cover-art palette (sampled from the current art) — feeds the Now-Playing poster, the
            // Adaptive theme's scheme, and the album-art glow. Extracted off the main thread.
            // We need it whenever Sleeve is on (the poster), the Adaptive theme is picked, or the
            // glow is set to follow album colours.
            // The cover drives every colour in the app now, so it is always extracted. This used to be
            // gated on Sleeve, the Adaptive theme or an album-art glow setting — all of which are
            // gone, and when the gate came out false the app sat on the default palette entirely.
            val artworkColors by produceState(DefaultCoverColors, artworkUrl) {
                if (artworkUrl.isNullOrBlank()) {
                    value = DefaultCoverColors
                    return@produceState
                }
                // Settle before decoding. Skipping through ten tracks otherwise runs ten bitmap
                // decodes and ten Palette passes for nine covers nobody sees, and that work lands on
                // exactly the frames the skip animation needs.
                kotlinx.coroutines.delay(220)
                value = extractCoverColors(context, artworkUrl!!) ?: DefaultCoverColors
            }

            // Pure functions of the cover, so they are computed when the cover changes rather than on
            // every playback emission. Both walk contrast searches; rebuilding them per recomposition
            // was doing that work dozens of times a second for an unchanged colour.
            val flavour by settingsViewModel.colorFlavour.collectAsStateWithLifecycle()
            val expressive = remember(artworkColors, flavour) {
                expressiveColorsFrom(artworkColors, flavour)
            }
            val materialScheme = remember(expressive) { expressiveColorScheme(expressive) }

            // System bar glyphs follow the canvas, not the system's dark-mode flag. Pastel puts a
            // light container behind them, and white-on-white status icons are invisible.
            val view = LocalView.current
            val lightBars = expressive.container.luminance() > 0.45f
            LaunchedEffect(lightBars, view) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = lightBars
                controller.isAppearanceLightNavigationBars = lightBars
            }

            // The Material scheme is the expressive palette. Anything still reading
            // MaterialTheme.colorScheme therefore agrees with the canvas it is drawn on, instead of
            // colouring text from a theme chosen independently of the background.
            VerzaTheme(scheme = materialScheme) {
                // Screens not yet rewritten read LocalCoverColors; mapping it onto the expressive
                // palette converts them without each one having to be touched.
                val chromeCover = remember(expressive) {
                    coverColorsFromExpressive(
                        container = expressive.container,
                        onContainer = expressive.onContainer,
                        onContainerMuted = expressive.onContainerMuted,
                        accent = expressive.accent,
                        line = expressive.line,
                    )
                }

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

                // Cross-fade the canvas as the cover changes. An effects spring, not a spatial one:
                // colour must not overshoot or it reads as a flash between tracks.
                //
                // Kept as a State and read in the draw lambda below rather than unwrapped here. Read
                // here, the fade would invalidate this scope — and everything the app draws with it —
                // on every frame of every track change.
                val canvas = animateColorAsState(
                    targetValue = expressive.container,
                    animationSpec = ExpressiveMotion.effectsSlow(),
                    label = "appCanvas",
                )

                CompositionLocalProvider(
                    LocalCoverColors provides chromeCover,
                    LocalArtworkColors provides artworkColors,
                    LocalExpressiveColors provides expressive,
                    // Any composable can ride the music: the same gated signal the player reads.
                    LocalAudioSignal provides (if (shouldVisualize) visualizerSignalFlow else null),
                ) {
                    // One surface, edge to edge, behind everything including the system bars.
                    Box(modifier = Modifier.fillMaxSize().drawBehind { drawRect(canvas.value) }) {
                        navContent()
                    }
                }
            }
        }
    }

    /** Re-delivered while we're already running (singleTop) — e.g. a session link tapped in a chat. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSessionIntent(intent)
        handleSharedYouTube(intent)
    }

    /** Posts an incoming verza://session/... link to the inbox; the playback owner loads it. */
    private fun handleSessionIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (intent.action == Intent.ACTION_VIEW && data.scheme == "verza" && data.host == "session") {
            SessionInbox.post(data.toString())
        }
    }

    /**
     * Handles a YouTube song shared into Verza — either a text/plain Share (ACTION_SEND, the usual
     * "Share → Verza" from the YouTube app) or a tapped youtu.be link (ACTION_VIEW). Pulls the video
     * id out and posts it; the playback owner plays it. A short toast covers the case where the
     * shared text carries no recognisable YouTube link (Verza shows up for any text/plain share).
     */
    private fun handleSharedYouTube(intent: Intent?) {
        if (intent == null) return
        val source = when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString?.takeIf { it.startsWith("http") }
            else -> null
        } ?: return
        val videoId = SharedSongInbox.extractVideoId(source)
        if (videoId != null) {
            SharedSongInbox.post(videoId)
        } else if (intent.action == Intent.ACTION_SEND) {
            android.widget.Toast.makeText(this, "No YouTube link found to play", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
