package com.verza.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import com.verza.ui.kit.GlassButton
import com.verza.ui.kit.Glassed
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.LocalOpen
import com.verza.ui.kit.glass
import com.verza.ui.kit.takesTouches
import com.verza.ui.library.LibraryPage
import com.verza.ui.nowplaying.LyricsPage
import com.verza.ui.nowplaying.NowPlayingPage
import com.verza.ui.page.PageScreen
import com.verza.ui.search.SearchPage
import com.verza.ui.settings.EqualizerPage
import com.verza.ui.settings.SettingsPage
import com.verza.ui.signin.SignIn
import com.verza.ui.signin.Welcome
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.launch

private class Tab(val label: String, val icon: ImageVector)

/** The tab bar's places, in its order. Search is the round button at its end, as iOS puts it. */
private val Tabs = linkedMapOf(
    "home" to Tab("Home", Icons.Rounded.Home),
    "library" to Tab("Library", Icons.Rounded.LibraryMusic),
    "sound" to Tab("Your Sound", Icons.Rounded.GraphicEq),
)

private val BarHeight = 62.dp
private val PlayerHeight = 56.dp
private val Gap = 8.dp

/** The whole app: the pages, and floating over them in glass the mini player and the tab bar. */
@Composable
fun Shell() {
    val nav = rememberNavController()
    val p = LocalPalette.current
    val player = LocalContext.current.graph.player
    val now by player.state.collectAsState()
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // What the glass covers at the foot, and a little more so a list's last row clears it.
    val dock = inset + Gap + BarHeight + (if (now.song != null) Gap + PlayerHeight else 0.dp) + 12.dp
    val open: (String) -> Unit = { id -> nav.navigate("page/$id") }
    var playerOpen by rememberSaveable { mutableStateOf(false) }
    var lyricsOpen by rememberSaveable { mutableStateOf(false) }
    var standbyOpen by rememberSaveable { mutableStateOf(false) }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    Box(Modifier.fillMaxSize().background(p.canvas)) {
        Glassed(
            ground = p.canvas,
            modifier = Modifier.fillMaxSize(),
            content = {
                CompositionLocalProvider(LocalDockHeight provides dock, LocalOpen provides open) {
                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        modifier = Modifier.fillMaxSize().statusBarsPadding(),
                        enterTransition = { fadeIn() },
                        exitTransition = { fadeOut() },
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
                // The edges: lists soften into the status bar and under the dock, rather than being cut.
                Box(Modifier.statusBarsPadding().fillMaxWidth().height(16.dp).background(Brush.verticalGradient(listOf(p.canvas, p.canvas.copy(alpha = 0f)))))
                // Signing in is Google's page, whole; the dock would cover its foot.
                if (route != "signin") {
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(dock + 16.dp).background(Brush.verticalGradient(0f to p.canvas.copy(alpha = 0f), 0.55f to p.canvas.copy(alpha = 0.6f), 1f to p.canvas.copy(alpha = 0.96f))))
                    Dock(nav, now, onOpenPlayer = { playerOpen = true }, Modifier.align(Alignment.BottomCenter))
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
        val settings = LocalContext.current.graph.settings
        val onboarded by settings.onboarded.flow.collectAsState(initial = null)
        val musicSetUp by settings.musicSetUp.flow.collectAsState(initial = null)
        if ((onboarded == false || musicSetUp == false) && route != "signin") Welcome(account = onboarded == false, onSignIn = { nav.navigate("signin") })
        // What an update brought, once. A new install has nothing to compare with, so it's marked seen.
        val context = LocalContext.current
        val tourSeen by settings.tourSeen.flow.collectAsState(initial = null)
        val paired by context.graph.connect.paired.flow.collectAsState(initial = emptyList())
        val updated = remember { context.packageManager.getPackageInfo(context.packageName, 0).let { it.firstInstallTime != it.lastUpdateTime } }
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val seen = { scope.launch { settings.tourSeen.set(com.verza.ui.signin.TOUR) }; Unit }
        if (onboarded == true && musicSetUp == true && route != "signin" && (tourSeen ?: com.verza.ui.signin.TOUR) < com.verza.ui.signin.TOUR) {
            if (updated) com.verza.ui.signin.Tour(paired.isNotEmpty(), onDone = seen, onSetUp = { seen(); nav.navigate("settings") })
            else LaunchedEffect(Unit) { seen() }
        }
    }
}

/** What floats at the foot: how far downloads have got, the playing song, then the tabs and search. */
@Composable
private fun Dock(nav: NavHostController, now: NowPlaying, onOpenPlayer: () -> Unit, modifier: Modifier) {
    val p = LocalPalette.current
    // The tab a page was opened from stays lit while it's showing.
    val stack by nav.currentBackStack.collectAsState()
    val current = stack.lastOrNull { it.destination.route in Tabs.keys || it.destination.route == "search" }?.destination?.route
    fun go(tab: String) {
        // The tab you're in takes you back to its top; another brings back where you were in it.
        if (tab == current) nav.popBackStack(tab, inclusive = false)
        else nav.navigate(tab) {
            popUpTo(nav.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Column(modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = Gap), verticalArrangement = Arrangement.spacedBy(Gap)) {
        val downloads by LocalContext.current.graph.downloads.progress.collectAsState()
        if (downloads.running) Text(
            (if (downloads.sorting) "Sorting your downloads " else "Downloading ") + "${downloads.done + 1} of ${downloads.total}" + (downloads.current?.let { " · $it" } ?: ""),
            style = Type.Small, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        )
        now.song?.let { MiniPlayer(now, onOpenPlayer) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TabBar(current, ::go, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            // On the search page its circle is stained like a chosen tab.
            GlassButton(Icons.Rounded.Search, "Search", size = BarHeight, stain = if (current == "search") p.accent else null, modifier = Modifier.takesTouches()) { go("search") }
        }
    }
}

/**
 * The tabs on a capsule of glass. The chosen one sits in a drop of glass stained with the accent, and
 * moving to another, the drop's ends chase it at different speeds: it stretches toward the new tab and
 * settles round there, the way a drop of water runs.
 */
@Composable
private fun TabBar(current: String?, go: (String) -> Unit, modifier: Modifier) {
    val p = LocalPalette.current
    val routes = Tabs.keys.toList()
    val index = routes.indexOf(current)
    val left = remember { Animatable(index.coerceAtLeast(0).toFloat()) }
    val right = remember { Animatable(index.coerceAtLeast(0) + 1f) }
    val shown = remember { Animatable(if (index >= 0) 1f else 0f) }
    LaunchedEffect(index) {
        if (index < 0) { shown.animateTo(0f, tween(180)); return@LaunchedEffect }
        if (shown.value == 0f) { left.snapTo(index.toFloat()); right.snapTo(index + 1f) }
        val ahead = spring<Float>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        val behind = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
        val forward = index >= left.value
        launch { shown.animateTo(1f, tween(180)) }
        launch { left.animateTo(index.toFloat(), if (forward) behind else ahead) }
        launch { right.animateTo(index + 1f, if (forward) ahead else behind) }
    }
    Row(
        modifier.height(BarHeight).takesTouches().glass(CircleShape, BarHeight / 2).padding(4.dp)
            .drawBehind {
                if (shown.value <= 0f) return@drawBehind
                val w = size.width / routes.size
                val start = Offset(left.value * w, 0f)
                val drop = Size((right.value - left.value) * w, size.height)
                val round = CornerRadius(size.height / 2)
                drawRoundRect(Brush.verticalGradient(
                    0f to p.accent.copy(alpha = (if (p.dark) 0.5f else 0.3f) * shown.value),
                    1f to p.accent.copy(alpha = (if (p.dark) 0.3f else 0.16f) * shown.value),
                ), start, drop, round)
                drawRoundRect(p.accent.copy(alpha = 0.45f * shown.value), start, drop, round, style = Stroke(1.dp.toPx()))
            },
    ) {
        for ((route, tab) in Tabs) {
            val on = route == current
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(CircleShape)
                    .clickable { go(route) }
                    .semantics(mergeDescendants = true) { role = Role.Tab; selected = on },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(tab.icon, null, tint = p.ink, modifier = Modifier.size(25.dp))
                Text(tab.label, style = Type.Tab, color = p.ink, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

/**
 * The playing song in a capsule of glass over the tabs; a tap raises Now Playing. The glass holds the
 * cover's colours, and its rim fills with the accent as the song plays, from the top left round.
 */
@Composable
private fun MiniPlayer(now: NowPlaying, onOpen: () -> Unit) {
    val song = now.song ?: return
    val p = LocalPalette.current
    val context = LocalContext.current
    val player = context.graph.player
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    // A cover twelve pixels wide, stretched across the capsule, is its colours and nothing else.
    val colours = coil3.compose.rememberAsyncImagePainter(
        coil3.request.ImageRequest.Builder(context).data(song.art?.let { artAt(it, 60) }).size(12).precision(coil3.size.Precision.EXACT).build(),
    )
    val played = remember { mutableFloatStateOf(0f) }
    // Polled, since position isn't pushed; read in the draw phase, so a tick is a redraw only.
    LaunchedEffect(song.id, now.playing, now.durationMs) {
        while (true) {
            played.floatValue = if (now.durationMs > 0) (player.position.toFloat() / now.durationMs).coerceIn(0f, 1f) else 0f
            if (!now.playing) break
            delay(500)
        }
    }
    Row(
        Modifier.fillMaxWidth().height(PlayerHeight).takesTouches()
            .glass(CircleShape, PlayerHeight / 2, pressed, inside = { with(colours) { draw(size, alpha = if (p.dark) 0.45f else 0.38f) } })
            .drawWithCache {
                val inset = 1.dp.toPx()
                val h = size.height
                val rim = Path().apply {
                    moveTo(h / 2, inset)
                    lineTo(size.width - h / 2, inset)
                    arcTo(Rect(size.width - h + inset, inset, size.width - inset, h - inset), -90f, 180f, false)
                    lineTo(h / 2, h - inset)
                    arcTo(Rect(inset, inset, h - inset, h - inset), 90f, 180f, false)
                }
                val measure = PathMeasure().apply { setPath(rim, false) }
                val part = Path()
                val line = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
                onDrawWithContent {
                    drawContent()
                    part.reset()
                    if (measure.getSegment(0f, measure.length * played.floatValue, part, true)) drawPath(part, p.accent, style = line)
                }
            }
            .clickable(source, null, onClick = onOpen)
            .semantics { contentDescription = "Now playing: ${song.title}" }
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(song.art?.let { artAt(it, 226) }, 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(com.verza.data.shownTitle(song.title, song.artist), style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, style = Type.Small, color = p.ink.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = player::toggle) {
            Icon(if (now.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (now.playing) "Pause" else "Play", tint = p.ink, modifier = Modifier.size(30.dp))
        }
        IconButton(onClick = player::next) { Icon(Icons.Rounded.SkipNext, "Next", tint = p.ink, modifier = Modifier.size(30.dp)) }
    }
}
