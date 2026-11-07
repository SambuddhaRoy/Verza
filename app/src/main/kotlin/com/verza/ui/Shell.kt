package com.verza.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.verza.ui.nowplaying.LyricsPage
import com.verza.ui.nowplaying.NowPlayingPage
import com.verza.ui.settings.SettingsPage
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.verza.data.artAt
import com.verza.graph
import com.verza.playback.NowPlaying
import com.verza.ui.home.HomePage
import com.verza.ui.kit.Cover
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.LocalOpen
import com.verza.ui.library.LibraryPage
import com.verza.ui.page.PageScreen
import com.verza.ui.search.SearchPage
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.delay

/** The pages the dock switches between, in its order. */
private val Tabs = listOf("home", "search", "library")

private val TabsHeight = 56.dp
private val PlayerHeight = 64.dp

/** The whole app: pages over the canvas, and the dock (the mini player and the tabs) at the foot. */
@Composable
fun Shell() {
    val nav = rememberNavController()
    val p = LocalPalette.current
    val player = LocalContext.current.graph.player
    val now by player.state.collectAsState()
    val dock = TabsHeight + if (now.song != null) PlayerHeight else 0.dp
    val open: (String) -> Unit = { id -> nav.navigate("page/$id") }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var lyricsOpen by rememberSaveable { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(p.canvas)) {
        CompositionLocalProvider(LocalDockHeight provides dock, LocalOpen provides open) {
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable("home") { HomePage(onOpen = open, onSettings = { nav.navigate("settings") }) }
                composable("settings") { SettingsPage(onBack = { nav.popBackStack() }) }
                composable("search") { SearchPage(onOpen = open) }
                composable("library") { LibraryPage(onOpen = open) }
                composable("page/{id}") { entry ->
                    PageScreen(entry.arguments?.getString("id").orEmpty(), onBack = { nav.popBackStack() }, onOpen = open)
                }
            }
        }
        Dock(nav, now, onOpenPlayer = { playerOpen = true }, Modifier.align(Alignment.BottomCenter))
        // Now Playing rises over everything from the mini player and drops back.
        AnimatedVisibility(playerOpen && now.song != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            NowPlayingPage(onClose = { playerOpen = false }, onOpen = open, onLyrics = { lyricsOpen = true })
        }
        AnimatedVisibility(lyricsOpen && now.song != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            LyricsPage(onClose = { lyricsOpen = false })
        }
    }
}

/** One pane at the foot: the playing song over a hairline that is also its progress, then the tabs. */
@Composable
private fun Dock(nav: NavHostController, now: NowPlaying, onOpenPlayer: () -> Unit, modifier: Modifier) {
    val p = LocalPalette.current
    val player = LocalContext.current.graph.player
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    Column(modifier.fillMaxWidth().background(p.canvas.copy(alpha = 0.96f)).navigationBarsPadding()) {
        now.song?.let { song ->
            Row(
                Modifier.fillMaxWidth().height(PlayerHeight).clickable(onClick = onOpenPlayer).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(song.art?.let { artAt(it, 226) }, 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(song.title, style = Type.RowTitle, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(song.artist, style = Type.RowSubtitle, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = player::toggle) {
                    Icon(if (now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (now.playing) "Pause" else "Play", tint = p.ink, modifier = Modifier.size(32.dp))
                }
            }
        }
        Progress(now)
        // While downloads run, one quiet line says how far along they are.
        val downloads by LocalContext.current.graph.downloads.progress.collectAsState()
        if (downloads.running) Text(
            "downloading ${downloads.done + 1} of ${downloads.total}" + (downloads.current?.let { " · $it" } ?: ""),
            style = Type.Small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
        )
        Row(
            Modifier.fillMaxWidth().height(TabsHeight).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for (tab in Tabs) {
                Text(
                    tab,
                    style = Type.Section.copy(fontSize = 26.sp),
                    color = if (tab == current) p.ink else p.inkMuted,
                    modifier = Modifier.clickable {
                        if (tab != current) nav.navigate(tab) {
                            popUpTo(nav.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}

/** The hairline between the player and the tabs, filled as far as the song has played. */
@Composable
private fun Progress(now: NowPlaying) {
    val p = LocalPalette.current
    val player = LocalContext.current.graph.player
    val fraction = remember { mutableFloatStateOf(0f) }
    // Polled, since position isn't pushed; read in the draw phase, so a tick is a redraw only.
    LaunchedEffect(now.song?.id, now.playing, now.durationMs) {
        while (true) {
            fraction.floatValue = if (now.durationMs > 0) (player.position.toFloat() / now.durationMs).coerceIn(0f, 1f) else 0f
            if (!now.playing) break
            delay(500)
        }
    }
    Box(
        Modifier.fillMaxWidth().height(1.dp).drawBehind {
            drawRect(p.line)
            drawLine(p.accent, Offset.Zero, Offset(size.width * fraction.floatValue, 0f), strokeWidth = size.height * 2)
        },
    )
}
