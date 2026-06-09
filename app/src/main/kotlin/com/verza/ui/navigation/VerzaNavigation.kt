package com.verza.ui.navigation

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.verza.innertube.SearchFilter
import com.verza.innertube.models.HomeItem
import com.verza.playback.PlaybackViewModel
import com.verza.ui.components.LocalTrackActions
import com.verza.ui.components.VerzaBottomBar
import com.verza.ui.components.MiniPlayer
import com.verza.ui.components.TrackActions
import com.verza.ui.screens.*

@Composable
fun VerzaNavigation(
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    postBootDestination: String = Screen.Home.route,
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // Boot animation, Onboarding, and the WebView Login screen are all pre-app surfaces.
    // The chrome (mini-player, bottom nav) should not appear on any of them.
    val isChromeHidden = currentRoute == Screen.Boot.route ||
        currentRoute == Screen.Onboarding.route ||
        currentRoute == Screen.Login.route

    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val playback by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val positionMs by playbackViewModel.positionMs.collectAsStateWithLifecycle()
    val likedIds by playbackViewModel.likedIds.collectAsStateWithLifecycle()
    val downloadedIds by playbackViewModel.downloadedIds.collectAsStateWithLifecycle()
    val downloading by playbackViewModel.downloading.collectAsStateWithLifecycle()
    val sleepTimerEndAt by playbackViewModel.sleepTimerEndAt.collectAsStateWithLifecycle()
    val focusSession by playbackViewModel.focusSession.collectAsStateWithLifecycle()
    val focusComplete by playbackViewModel.focusComplete.collectAsStateWithLifecycle()
    val pendingSharedSession by playbackViewModel.pendingSharedSession.collectAsStateWithLifecycle()

    // Activity-scoped settings VM (same instance MainActivity uses) for UI prefs the player VM
    // doesn't own — e.g. the Now Playing album-art motion toggle.
    val settingsVm: SettingsViewModel = hiltViewModel()
    val albumArtMotion by settingsVm.albumArtMotion.collectAsStateWithLifecycle()
    val sleeveMode by settingsVm.sleeveMode.collectAsStateWithLifecycle()
    // Sleeve mode wants an immersive, edge-to-edge Now Playing, so the bottom chrome hides there.
    val immersiveNowPlaying = sleeveMode && currentRoute == Screen.NowPlaying.route
    val artworkOverride by playbackViewModel.currentArtworkOverride.collectAsStateWithLifecycle()

    val current = playback.currentItem
    val currentTitle = current?.mediaMetadata?.title?.toString() ?: "Nothing playing"
    val currentArtist = current?.mediaMetadata?.artist?.toString() ?: ""
    // Prefer the higher-resolution iTunes cover when we've resolved one; otherwise use the YT thumb.
    val currentArtworkUrl = artworkOverride ?: current?.mediaMetadata?.artworkUri?.toString()
    val currentLiked = current?.mediaId?.let { it in likedIds } ?: false
    val currentDownloaded = current?.mediaId?.let { it in downloadedIds } ?: false
    val currentDownloading = current?.mediaId?.let { it in downloading } ?: false

    val hasTrack = current != null
    val showMiniPlayer = hasTrack && currentRoute != Screen.NowPlaying.route && currentRoute != null && !isChromeHidden

    val context = LocalContext.current

    // The track currently being added to a playlist via the sheet picker, or null when closed.
    var pendingAdd by remember { mutableStateOf<com.verza.innertube.models.MusicItem?>(null) }
    // The Home card whose long-press context menu is open, or null when closed.
    var homeMenuItem by remember { mutableStateOf<HomeItem?>(null) }
    // Whether the full-screen ambient ("lean-back") display is showing.
    var ambientActive by remember { mutableStateOf(false) }

    // Per-track action menu wiring — provided once to every row composable via CompositionLocal.
    val trackActions = remember(playbackViewModel, navController, context) {
        TrackActions(
            onPlayNext = playbackViewModel::playNext,
            onAddToQueue = playbackViewModel::enqueue,
            onToggleLike = playbackViewModel::toggleLike,
            onDownload = playbackViewModel::download,
            onShare = { item ->
                val url = "https://music.youtube.com/watch?v=${item.id}"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, item.title)
                    putExtra(Intent.EXTRA_TEXT, "${item.title} — ${item.artist}\n$url")
                }
                context.startActivity(Intent.createChooser(intent, "Share song"))
            },
            onGoToArtist = { item ->
                PendingSearch.query = item.artist
                PendingSearch.filter = SearchFilter.ARTISTS
                navController.navigate(Screen.Search.route) { launchSingleTop = true }
            },
            onAddToPlaylist = { item -> pendingAdd = item },
        )
    }

    // Songs play immediately; artists (UC…) open the artist page; albums/playlists open detail.
    val openItem: (HomeItem) -> Unit = { item ->
        val browseId = item.browseId
        when {
            item.isSong -> {
                playbackViewModel.playHomeItem(item)
                navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
            }
            browseId != null && browseId.startsWith("UC") ->
                navController.navigate(Screen.Artist.create(browseId))
            browseId != null ->
                navController.navigate(Screen.Collection.create(browseId))
            item.playlistId != null ->
                navController.navigate(Screen.Collection.create("VL${item.playlistId}"))
            else -> {
                playbackViewModel.playHomeItem(item)
                navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        bottomBar = {
            // Hide the entire bottom chrome (mini-player + nav) on Onboarding/Login so the
            // pre-app screens get the full canvas. AnimatedVisibility on each piece keeps the
            // re-appear smooth on exit from onboarding.
            if (!isChromeHidden && !immersiveNowPlaying) Column {
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
                ) {
                    MiniPlayer(
                        title = currentTitle,
                        artist = currentArtist,
                        isPlaying = playback.isPlaying,
                        artworkColor = Color(0xFF2980B9),
                        artworkUrl = currentArtworkUrl,
                        onExpand = {
                            navController.navigate(Screen.NowPlaying.route) {
                                launchSingleTop = true
                            }
                        },
                        onTogglePlay = { playbackViewModel.togglePlay() },
                    )
                }
                VerzaBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        if (currentRoute == screen.route) return@VerzaBottomBar
                        // Two-phase nav for bottom-bar taps:
                        // 1. If the target tab is already in the back stack (the common case for
                        //    Home — it's the start destination, always present), pop straight back
                        //    to it. This is the only path that reliably "goes home" from a deep
                        //    destination like Settings or Now Playing; the previous navigate(…)
                        //    + popUpTo + restoreState combo silently no-op'd in that case.
                        // 2. Otherwise (e.g. tapping Search for the first time), navigate fresh,
                        //    anchored under Home so the stack stays flat across tab switches.
                        val popped = navController.popBackStack(screen.route, inclusive = false)
                        if (popped) return@VerzaBottomBar
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
      CompositionLocalProvider(LocalTrackActions provides trackActions) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            // Two motion idioms, chosen per-navigation (see helpers at the bottom of the file):
            //  • Switching between the bottom-bar tabs uses Material **fade-through** — the
            //    outgoing screen dissolves to reveal the live glow, then the new one fades and
            //    eases up. No directional slide, so lateral moves feel calm and seamless.
            //  • Pushing into / out of a detail screen uses a **shared-axis** slide + fade with
            //    emphasized easing, giving hierarchy a gentle sense of direction.
            enterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                when {
                    from in PREAPP_ROUTES || to in PREAPP_ROUTES -> fadeIn(tween(320))
                    from in TAB_ROUTES && to in TAB_ROUTES -> fadeThroughIn()
                    else -> sharedAxisIn(forward = true)
                }
            },
            exitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                when {
                    from in PREAPP_ROUTES || to in PREAPP_ROUTES -> fadeOut(tween(220))
                    from in TAB_ROUTES && to in TAB_ROUTES -> fadeThroughOut()
                    else -> sharedAxisOut(forward = true)
                }
            },
            popEnterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                when {
                    from in PREAPP_ROUTES || to in PREAPP_ROUTES -> fadeIn(tween(320))
                    from in TAB_ROUTES && to in TAB_ROUTES -> fadeThroughIn()
                    else -> sharedAxisIn(forward = false)
                }
            },
            popExitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                when {
                    from in PREAPP_ROUTES || to in PREAPP_ROUTES -> fadeOut(tween(220))
                    from in TAB_ROUTES && to in TAB_ROUTES -> fadeThroughOut()
                    else -> sharedAxisOut(forward = false)
                }
            },
        ) {
            composable(Screen.Boot.route) {
                BootScreen(
                    onFinished = {
                        navController.navigate(postBootDestination) {
                            // Strip Boot from the back stack — once the animation has played,
                            // a back-press from Home/Onboarding should exit the app rather than
                            // re-trigger the boot reveal.
                            popUpTo(Screen.Boot.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onSignIn = { navController.navigate(Screen.Login.route) },
                    onFinished = { takeTour ->
                        val dest = if (takeTour) Screen.Tour.create(fromOnboarding = true) else Screen.Home.route
                        navController.navigate(dest) {
                            // Strip onboarding from the back stack so a back-press from Home exits
                            // the app rather than returning the user to onboarding.
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = Screen.Tour.route,
                arguments = listOf(navArgument(Screen.Tour.ARG) {
                    type = NavType.BoolType
                    defaultValue = false
                }),
            ) { entry ->
                val fromOnboarding = entry.arguments?.getBoolean(Screen.Tour.ARG) ?: false
                FeatureTourScreen(
                    onFinish = {
                        if (fromOnboarding) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Tour.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onItemClick = openItem,
                    onItemLongPress = { homeMenuItem = it },
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
                    onOpenMix = { mixId -> navController.navigate(Screen.Mix.create(mixId)) },
                )
            }
            composable(
                route = Screen.Mix.route,
                arguments = listOf(navArgument(Screen.Mix.ARG) { type = NavType.StringType }),
            ) {
                MixScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = openItem,
                    onPlayAll = { items ->
                        playbackViewModel.playSongItems(items)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(onItemClick = openItem)
            }
            composable(
                route = Screen.Collection.route,
                arguments = listOf(navArgument(Screen.Collection.ARG) { type = NavType.StringType }),
            ) {
                CollectionScreen(
                    onBack = { navController.popBackStack() },
                    onPlayTracks = { tracks, index ->
                        playbackViewModel.playSongs(tracks, index)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    onShuffle = { tracks ->
                        playbackViewModel.playShuffled(tracks)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    onAddToQueue = { tracks -> playbackViewModel.enqueueAll(tracks) },
                )
            }
            composable(
                route = Screen.Artist.route,
                arguments = listOf(navArgument(Screen.Artist.ARG) { type = NavType.StringType }),
            ) {
                ArtistScreen(
                    onBack = { navController.popBackStack() },
                    onItemClick = openItem,
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onSignIn = { navController.navigate(Screen.Login.route) },
                    onOpenStats = { navController.navigate(Screen.Stats.route) },
                    onOpenEqualizer = { navController.navigate(Screen.Equalizer.route) },
                    onOpenTour = { navController.navigate(Screen.Tour.create(fromOnboarding = false)) },
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Equalizer.route) {
                EqualizerScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Login.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                LoginScreen(
                    onSignedIn = { cookie ->
                        settingsViewModel.onSignedIn(cookie)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Library.route) {
                LibraryScreen(
                    onPlaySongs = { songs, index ->
                        playbackViewModel.playSongs(songs, index)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    onOpenItem = openItem,
                    onOpenLocalPlaylist = { id ->
                        navController.navigate(Screen.LocalPlaylist.create(id))
                    },
                )
            }
            composable(
                route = Screen.LocalPlaylist.route,
                arguments = listOf(navArgument(Screen.LocalPlaylist.ARG) { type = NavType.StringType }),
            ) {
                LocalPlaylistScreen(
                    onBack = { navController.popBackStack() },
                    onPlayTracks = { tracks, index ->
                        playbackViewModel.playSongs(tracks, index)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    onShuffle = { tracks ->
                        playbackViewModel.playShuffled(tracks)
                        navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                    },
                    onAddToQueue = { tracks -> playbackViewModel.enqueueAll(tracks) },
                )
            }
            composable(Screen.NowPlaying.route) {
                NowPlayingScreen(
                    onBack = { navController.popBackStack() },
                    videoId = current?.mediaId,
                    title = currentTitle,
                    artist = currentArtist,
                    artworkUrl = currentArtworkUrl,
                    isPlaying = playback.isPlaying,
                    isLiked = currentLiked,
                    isDownloaded = currentDownloaded,
                    isDownloading = currentDownloading,
                    positionMs = positionMs,
                    durationMs = playback.durationMs,
                    shuffleEnabled = playback.shuffleEnabled,
                    repeatMode = playback.repeatMode,
                    queue = playback.queue,
                    currentIndex = playback.currentIndex,
                    onTogglePlay = { playbackViewModel.togglePlay() },
                    onToggleLike = { playbackViewModel.toggleLikeCurrent() },
                    onStartRadio = { current?.mediaId?.let { playbackViewModel.startRadio(it) } },
                    onOpenLyrics = { navController.navigate(Screen.Lyrics.route) { launchSingleTop = true } },
                    onDownload = { playbackViewModel.downloadCurrent() },
                    onRemoveDownload = { playbackViewModel.removeDownloadCurrent() },
                    onNext = { playbackViewModel.seekToNext() },
                    onPrevious = { playbackViewModel.seekToPrevious() },
                    onSeek = { playbackViewModel.seekTo(it) },
                    onToggleShuffle = { playbackViewModel.toggleShuffle() },
                    onCycleRepeat = { playbackViewModel.cycleRepeatMode() },
                    onPlayQueueItem = { playbackViewModel.playQueueItemAt(it) },
                    onRemoveQueueItem = { playbackViewModel.removeQueueItemAt(it) },
                    onAddToPlaylist = {
                        current?.let { item ->
                            pendingAdd = com.verza.innertube.models.MusicItem(
                                id = item.mediaId,
                                title = currentTitle,
                                artist = currentArtist,
                                thumbnailUrl = currentArtworkUrl,
                                durationMs = playback.durationMs,
                            )
                        }
                    },
                    onEnterAmbient = { ambientActive = true },
                    sleepTimerEndAt = sleepTimerEndAt,
                    onSetSleepTimer = { playbackViewModel.setSleepTimer(it) },
                    onWindDown = { playbackViewModel.setSleepTimerWindDown(it) },
                    onSleepTimerEndOfTrack = { playbackViewModel.setSleepTimerEndOfTrack() },
                    focusActive = focusSession != null,
                    focusEndAt = focusSession?.endAt,
                    onStartFocus = { playbackViewModel.startFocusSession(it) },
                    onEndFocus = { playbackViewModel.endFocusSession() },
                    focusCompleteMinutes = focusComplete,
                    onConsumeFocusComplete = { playbackViewModel.consumeFocusComplete() },
                    onBuildSessionLink = { playbackViewModel.buildSessionShareLink() },
                    albumArtMotion = albumArtMotion,
                    sleeveMode = sleeveMode,
                )
            }
            composable(Screen.Lyrics.route) {
                LyricsScreen(
                    title = currentTitle,
                    artist = currentArtist,
                    durationMs = playback.durationMs,
                    positionMs = positionMs,
                    artworkUrl = currentArtworkUrl,
                    onBack = { navController.popBackStack() },
                )
            }
        }
      }
    }

    // Modal "Add to playlist" sheet — sits above the whole nav graph so any track action that
    // toggles `pendingAdd` opens it, regardless of which screen the action came from.
    pendingAdd?.let { item ->
        AddToPlaylistSheet(item = item, onDismiss = { pendingAdd = null })
    }

    // Full-screen ambient "lean-back" display — sits above the whole nav graph; exits on tap/back.
    if (ambientActive && hasTrack) {
        AmbientDisplay(
            title = currentTitle,
            artist = currentArtist,
            artworkUrl = currentArtworkUrl,
            isPlaying = playback.isPlaying,
            positionMs = positionMs,
            durationMs = playback.durationMs,
            onTogglePlay = { playbackViewModel.togglePlay() },
            onExit = { ambientActive = false },
        )
    }
    // Auto-exit ambient if playback stops entirely (nothing left to display).
    LaunchedEffect(hasTrack) { if (!hasTrack) ambientActive = false }

    // Long-press context menu for a Home feed card.
    homeMenuItem?.let { item ->
        HomeItemSheet(
            item = item,
            isLiked = item.videoId?.let { it in likedIds } ?: false,
            onPlay = {
                playbackViewModel.playHomeItem(item)
                navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
            },
            onPlayNext = { playbackViewModel.playNext(item.toMusicItem()) },
            onAddToQueue = { playbackViewModel.enqueueHomeItem(item) },
            onStartRadio = {
                item.videoId?.let { id ->
                    playbackViewModel.startRadio(id)
                    navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                }
            },
            onToggleLike = { playbackViewModel.toggleLike(item.toMusicItem()) },
            onAddToPlaylist = { pendingAdd = item.toMusicItem() },
            onGoToArtist = { trackActions.onGoToArtist(item.toMusicItem()) },
            onOpen = { openItem(item) },
            onDismiss = { homeMenuItem = null },
        )
    }

    // A "listen along" link was opened — confirm before replacing the current queue (the deep link
    // is exported, so the session is never loaded without the user's explicit go-ahead).
    pendingSharedSession?.let { session ->
        val count = session.tracks.size
        val lead = session.tracks.firstOrNull()
        AlertDialog(
            onDismissRequest = { playbackViewModel.dismissSharedSession() },
            title = { Text("Listen along?") },
            text = {
                Text(
                    buildString {
                        append("Someone shared a set of $count track")
                        if (count != 1) append("s")
                        lead?.takeIf { it.title.isNotBlank() }?.let { append(", starting with “${it.title}”") }
                        append(". Load it and start playing? This replaces your current queue.")
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    playbackViewModel.acceptSharedSession()
                    navController.navigate(Screen.NowPlaying.route) { launchSingleTop = true }
                }) { Text("Listen") }
            },
            dismissButton = {
                TextButton(onClick = { playbackViewModel.dismissSharedSession() }) { Text("Not now") }
            },
        )
    }
}

// ── Navigation motion ────────────────────────────────────────────────────────────
// Tuned to feel like the transitions in design-forward apps: nothing ever just "cuts",
// lateral moves dissolve through the ambient glow, and hierarchy slides with a confident,
// emphasized decelerate rather than a linear shove.

/** Material 3 "emphasized" easing — a strong, natural decelerate with no overshoot. */
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/** The four bottom-bar tabs — switching between any of these uses fade-through. */
private val TAB_ROUTES = setOf(
    Screen.Home.route, Screen.Search.route, Screen.Library.route, Screen.NowPlaying.route,
)

/** Pre-app surfaces (boot / onboarding / login) — kept to a plain, quiet crossfade. */
private val PREAPP_ROUTES = setOf(
    Screen.Boot.route, Screen.Onboarding.route, Screen.Login.route,
)

/** Fade-through enter: a beat after the old screen clears, the new one fades + eases up to scale. */
private fun fadeThroughIn(): EnterTransition =
    fadeIn(tween(280, delayMillis = 90, easing = Emphasized)) +
        scaleIn(initialScale = 0.94f, animationSpec = tween(280, delayMillis = 90, easing = Emphasized))

/** Fade-through exit: the outgoing screen quickly dissolves + eases down, revealing the glow. */
private fun fadeThroughOut(): ExitTransition =
    fadeOut(tween(90, easing = LinearOutSlowInEasing)) +
        scaleOut(targetScale = 0.96f, animationSpec = tween(90))

/** Shared-axis (X) enter — a small directional slide + fade for push (forward) / pop (back). */
private fun sharedAxisIn(forward: Boolean): EnterTransition {
    val dir = if (forward) 1 else -1
    return slideInHorizontally(tween(340, easing = Emphasized)) { full -> dir * full / 10 } +
        fadeIn(tween(220, delayMillis = 40, easing = Emphasized))
}

/** Shared-axis (X) exit — the counterpart slide + fade for the departing screen. */
private fun sharedAxisOut(forward: Boolean): ExitTransition {
    val dir = if (forward) 1 else -1
    return slideOutHorizontally(tween(340, easing = Emphasized)) { full -> -dir * full / 10 } +
        fadeOut(tween(150, easing = LinearOutSlowInEasing))
}
