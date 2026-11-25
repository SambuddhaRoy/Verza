package com.verza.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.verza.graph
import com.verza.ui.home.HomePage
import com.verza.ui.kit.Glassed
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.LocalOpen
import com.verza.ui.library.LibraryPage
import com.verza.ui.look.DockState
import com.verza.ui.look.LocalLook
import com.verza.ui.look.Tab
import com.verza.ui.nowplaying.LyricsPage
import com.verza.ui.nowplaying.NowPlayingPage
import com.verza.ui.page.PageScreen
import com.verza.ui.search.SearchPage
import com.verza.ui.settings.EqualizerPage
import com.verza.ui.settings.SettingsPage
import com.verza.ui.signin.SignIn
import com.verza.ui.signin.Welcome
import com.verza.ui.theme.LocalPalette
import kotlinx.coroutines.launch

/** The dock's places, in its order. Search is last; some looks set it apart. */
private val Tabs = listOf(
    Tab("home", "Home", Icons.Rounded.Home),
    Tab("library", "Library", Icons.Rounded.LibraryMusic),
    Tab("sound", "Your Sound", Icons.Rounded.GraphicEq),
    Tab("search", "Search", Icons.Rounded.Search, search = true),
)

/** The whole app: the pages over the look's ground, and the look's dock floating over them. */
private val Still = androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero)

@Composable
fun Shell() {
    val nav = rememberNavController()
    val p = LocalPalette.current
    val look = LocalLook.current
    val context = LocalContext.current
    val player = context.graph.player
    val now by player.state.collectAsState()
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // What the dock covers at the foot, so a list's last row clears it.
    val dock = inset + look.dockHeight(now.song != null)
    val open: (String) -> Unit = { id -> nav.navigate("page/$id") }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var lyricsOpen by rememberSaveable { mutableStateOf(false) }
    var standbyOpen by rememberSaveable { mutableStateOf(false) }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    // A look can be dark while the system is light, or the other way: the status bar follows the look.
    com.verza.ui.kit.BarIcons(light = p.dark)
    val tilt = if (look is com.verza.ui.look.GlassLook) com.verza.ui.kit.rememberTilt() else Still

    CompositionLocalProvider(com.verza.ui.kit.LocalTilt provides tilt) {
    Box(Modifier.fillMaxSize().background(p.canvas)) {
        Glassed(
            ground = p.canvas,
            modifier = Modifier.fillMaxSize(),
            content = {
                look.Backdrop(Modifier.fillMaxSize())
                CompositionLocalProvider(LocalDockHeight provides dock, LocalOpen provides open) {
                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize().statusBarsPadding(),
                        enterTransition = { fadeIn(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.scaleIn(com.verza.ui.kit.bouncy(androidx.compose.animation.core.Spring.StiffnessMedium), initialScale = 0.96f) },
                        exitTransition = { fadeOut(androidx.compose.animation.core.tween(160)) },
                    ) {
                        composable("home") { HomePage(onOpen = open, onSettings = { nav.navigate("settings") }) }
                        composable("settings") { SettingsPage(onBack = { nav.popBackStack() }, onSignIn = { nav.navigate("signin") }, onEqualizer = { nav.navigate("equalizer") }) }
                        composable("equalizer") { EqualizerPage(onBack = { nav.popBackStack() }) }
                        composable("signin") { SignIn(onDone = { nav.popBackStack() }) }
                        composable("search") { SearchPage(onOpen = open) }
                        composable("library") { LibraryPage(onOpen = open) }
                        composable("sound") {
                            Column(Modifier.fillMaxSize()) {
                                com.verza.ui.kit.Masthead("Your Sound")
                                com.verza.ui.library.YourSound()
                            }
                        }
                        composable("page/{id}") { entry ->
                            PageScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }, onOpen = open)
                        }
                    }
                }
            },
            glass = {
                if (look.edgeFade) {
                    // The edges: lists soften into the status bar and under the dock, rather than being cut.
                    Box(Modifier.statusBarsPadding().fillMaxWidth().height(16.dp).background(Brush.verticalGradient(listOf(p.canvas, p.canvas.copy(alpha = 0f)))))
                    if (route != "signin") Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(dock + 16.dp).background(Brush.verticalGradient(0f to p.canvas.copy(alpha = 0f), 0.55f to p.canvas.copy(alpha = 0.6f), 1f to p.canvas.copy(alpha = 0.96f))))
                }
                // Signing in is Google's page, whole; the dock would cover its foot.
                if (route != "signin") {
                    val stack by nav.currentBackStack.collectAsState()
                    // The tab a page was opened from stays lit while it's showing.
                    val current = stack.lastOrNull { e -> Tabs.any { it.route == e.destination.route } }?.destination?.route
                    val downloads by context.graph.downloads.progress.collectAsState()
                    val state = remember(current, now, downloads) {
                        DockState(
                            tabs = Tabs,
                            current = current,
                            go = { tab ->
                                // The tab you're in takes you back to its top; another brings back where you were in it.
                                if (tab == current) nav.popBackStack(tab, inclusive = false)
                                else nav.navigate(tab) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            now = now,
                            openPlayer = { playerOpen = true },
                            downloads = if (!downloads.running) null else
                                (if (downloads.sorting) "Sorting your downloads " else "Downloading ") + "${downloads.done + 1} of ${downloads.total}" + (downloads.current?.let { " · $it" } ?: ""),
                        )
                    }
                    look.Dock(state, Modifier.align(Alignment.BottomCenter))
                }
            },
        )
        // Now Playing rises over everything from the mini player and drops back.
        // Flicks work from anywhere in the app: bringing the computer's music here needs nothing playing.
        val flick = com.verza.ui.nowplaying.FlickHandoff(playerOpen, openPlayer = { lyricsOpen = false; standbyOpen = false; playerOpen = true })
        AnimatedVisibility(playerOpen && now.song != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            NowPlayingPage(flick, onClose = { playerOpen = false }, onOpen = open, onLyrics = { lyricsOpen = true }, onStandby = { standbyOpen = true })
        }
        AnimatedVisibility(lyricsOpen && now.song != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            LyricsPage(onClose = { lyricsOpen = false })
        }
        if (standbyOpen && now.song != null) com.verza.ui.nowplaying.Standby(onExit = { standbyOpen = false })
        // The first launch of a new install: a welcome, signing in if wanted, then downloads and the
        // phone's music; an update from before those were asked about gets just those two, once. Read
        // as null until known, so an install that has seen it all never sees it flash past.
        val settings = context.graph.settings
        val onboarded by settings.onboarded.flow.collectAsState(initial = null)
        val musicSetUp by settings.musicSetUp.flow.collectAsState(initial = null)
        if ((onboarded == false || musicSetUp == false) && route != "signin") Welcome(account = onboarded == false, onSignIn = { nav.navigate("signin") })
        // What an update brought, once. A new install has nothing to compare with, so it's marked seen.
        val tourSeen by settings.tourSeen.flow.collectAsState(initial = null)
        val updated = remember { context.packageManager.getPackageInfo(context.packageName, 0).let { it.firstInstallTime != it.lastUpdateTime } }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val seen = { scope.launch { settings.tourSeen.set(com.verza.ui.signin.TOUR) }; Unit }
        if (onboarded == true && musicSetUp == true && route != "signin" && (tourSeen ?: com.verza.ui.signin.TOUR) < com.verza.ui.signin.TOUR) {
            if (updated) com.verza.ui.signin.Tour(onDone = seen)
            else LaunchedEffect(Unit) { seen() }
        }
    }
    }
}
