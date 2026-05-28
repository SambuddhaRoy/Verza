package com.verza.ui.navigation

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
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
fun VerzaNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val playbackViewModel: PlaybackViewModel = hiltViewModel()
    val playback by playbackViewModel.playbackState.collectAsStateWithLifecycle()
    val positionMs by playbackViewModel.positionMs.collectAsStateWithLifecycle()
    val likedIds by playbackViewModel.likedIds.collectAsStateWithLifecycle()
    val downloadedIds by playbackViewModel.downloadedIds.collectAsStateWithLifecycle()
    val downloading by playbackViewModel.downloading.collectAsStateWithLifecycle()
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
    val showMiniPlayer = hasTrack && currentRoute != Screen.NowPlaying.route && currentRoute != null

    val context = LocalContext.current

    // The track currently being added to a playlist via the sheet picker, or null when closed.
    var pendingAdd by remember { mutableStateOf<com.verza.innertube.models.MusicItem?>(null) }

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
            Column {
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
                        // Same-tab tap from a non-tab destination (Settings / NowPlaying /
                        // Collection / Artist / Lyrics …) was the case that silently no-op'd —
                        // we now always pop back to Home and then navigate to the requested tab,
                        // which gives a deterministic stack regardless of how deep the user is.
                        if (currentRoute == screen.route) return@VerzaBottomBar
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) },
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onItemClick = openItem,
                    onOpenSettings = { navController.navigate(Screen.Settings.route) },
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
                )
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
                )
            }
            composable(Screen.Lyrics.route) {
                LyricsScreen(
                    title = currentTitle,
                    artist = currentArtist,
                    durationMs = playback.durationMs,
                    positionMs = positionMs,
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
}
