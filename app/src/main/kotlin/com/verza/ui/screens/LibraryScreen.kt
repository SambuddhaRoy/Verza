package com.verza.ui.screens

import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.verza.ui.expressive.GlassEmpty
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.activity.compose.BackHandler
import com.verza.ui.components.pressableScale
import com.verza.ui.expressive.ShapeLarge
import com.verza.ui.expressive.ExpressiveMotion
import com.verza.ui.expressive.GlassSectionTitle
import com.verza.ui.expressive.GlassListRow
import com.verza.ui.theme.isGlass
import androidx.compose.foundation.layout.padding
import com.verza.ui.expressive.LocalBottomChrome
import com.verza.tracksLabel
import com.verza.ui.expressive.RowTitle
import com.verza.ui.expressive.RowSubtitle
import com.verza.ui.expressive.variedFace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import com.verza.ui.expressive.animatedReadableWidth
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import com.verza.ui.expressive.ExpressiveChipRow
import com.verza.ui.expressive.HeroTitle
import com.verza.ui.expressive.LocalExpressiveColors
import com.verza.ui.expressive.MetaLabel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.MusicItem
import com.verza.ui.components.TrackActionsMenu
import com.verza.ui.components.rememberSongArtwork
import com.verza.ui.expressive.BodyStrong
import com.verza.ui.expressive.BodyText
import com.verza.ui.expressive.PillShape
import com.verza.ui.expressive.ShapeLargeIncreased
import com.verza.ui.expressive.ShapeMedium
import com.verza.ui.theme.LocalVerzaExtendedColors

private enum class LibraryTab(val label: String, val icon: ImageVector) {
    RECENT("Recent", Icons.Filled.History),
    LIKED("Liked", Icons.Filled.Favorite),
    LOCAL("On device", Icons.Filled.PhoneAndroid),
    DOWNLOADED("Downloaded", Icons.Filled.Download),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Filled.PlaylistPlay),
    ARTISTS("Artists", Icons.Filled.Person),
}

/** READ_MEDIA_AUDIO on Android 13+, READ_EXTERNAL_STORAGE below — for scanning on-device music. */
private val audioReadPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        Manifest.permission.READ_MEDIA_AUDIO
    else
        Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun LibraryScreen(
    onPlaySongs: (List<MusicItem>, Int) -> Unit,
    onOpenItem: (HomeItem) -> Unit,
    onOpenLocalPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val recent by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val liked by viewModel.liked.collectAsStateWithLifecycle()
    val downloaded by viewModel.downloaded.collectAsStateWithLifecycle()
    val accountLiked by viewModel.accountLiked.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val localPlaylists by viewModel.localPlaylists.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val localSongs by viewModel.localSongs.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(LibraryTab.RECENT) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // On-device music needs a read permission; we track it here and (re)scan once it's granted.
    val context = LocalContext.current
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioReadPermission) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAudioPermission = granted
        if (granted) viewModel.loadLocalSongs()
    }

    val xc = LocalExpressiveColors.current
    // What each tab shows, shared by the two layouts: chips over a list, or Glass's index of
    // categories that each open as a page.
    val tabContent: @Composable (LibraryTab) -> Unit = { shown ->
            when (shown) {
                LibraryTab.RECENT ->
                    SongList(recent.map { it.toMusicItem() }, onPlaySongs, "Play something to see it here")
                LibraryTab.LIKED -> {
                    // Signed in, this was the account's list alone, so a song liked here did not
                    // show until the account list was fetched again. Your own likes go first.
                    val likedItems = (liked.map { it.toMusicItem() } + if (isSignedIn) accountLiked else emptyList())
                        .distinctBy { it.id }
                    SongList(likedItems, onPlaySongs, "Like songs to see them here")
                }
                LibraryTab.LOCAL -> LocalMusicContent(
                    hasPermission = hasAudioPermission,
                    songs = localSongs,
                    onRequestPermission = { permissionLauncher.launch(audioReadPermission) },
                    onScan = { viewModel.loadLocalSongs() },
                    onPlaySongs = onPlaySongs,
                )
                LibraryTab.DOWNLOADED ->
                    SongList(
                        downloaded.map { it.toMusicItem() },
                        onPlaySongs,
                        "Download songs to listen offline",
                    )
                LibraryTab.PLAYLISTS -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalBottomChrome.current),
                    verticalArrangement = Arrangement.spacedBy(if (isGlass()) 0.dp else 6.dp),
                ) {
                    // "+ Create playlist" is always available, regardless of sign-in state.
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ShapeMedium)
                                .clickable(onClick = { showCreatePlaylist = true; newPlaylistName = "" })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(ShapeMedium)
                                    .background(xc.accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = xc.onAccent)
                            }
                            Text(
                                "Create playlist",
                                style = BodyStrong,
                                color = xc.onSurface,
                            )
                        }
                    }
                    // Your local playlists.
                    if (localPlaylists.isNotEmpty()) {
                        items(localPlaylists) { p ->
                            LibraryRow(
                                title = p.playlist.name,
                                subtitle = tracksLabel(p.trackCount),
                                thumbnailUrl = p.coverUrl,
                                onClick = { onOpenLocalPlaylist(p.playlist.id) },
                            )
                        }
                    }
                    // Section header for saved YT playlists (only when signed in).
                    if (isSignedIn && playlists.isNotEmpty()) {
                        item {
                            Text(
                                "SAVED FROM YOUTUBE MUSIC",
                                style = MetaLabel,
                                color = xc.accent,
                                modifier = Modifier.padding(start = 6.dp, top = 20.dp, bottom = 8.dp),
                            )
                        }
                        items(playlists) { item ->
                            LibraryRow(
                                title = item.title,
                                subtitle = item.subtitle,
                                thumbnailUrl = item.thumbnailUrl,
                                onClick = { onOpenItem(item) },
                            )
                        }
                    } else if (!isSignedIn && localPlaylists.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    "Sign in to see your YouTube Music playlists here too",
                                    style = BodyText,
                                    color = xc.onContainerMuted,
                                )
                            }
                        }
                    }
                }
                LibraryTab.ARTISTS -> when {
                    !isSignedIn -> CenterHint("Sign in to see followed artists", ext.muted)
                    artists.isEmpty() -> CenterHint("No followed artists yet", ext.muted)
                    else -> CollectionList(artists, onOpenItem, circularArt = true)
                }
            }
    }
    if (isGlass()) {
        GlassLibrary(
            recent = recent.map { it.toMusicItem() },
            onPlaySongs = onPlaySongs,
            onCreatePlaylist = { showCreatePlaylist = true; newPlaylistName = "" },
            content = tabContent,
            modifier = modifier,
        )
    } else Column(
        modifier = modifier
            .fillMaxSize()
            .background(xc.container)
            // Capped and centred as a whole, so a row on a tablet is not a thumbnail followed by an
            // acre, and the header, chips and lists keep one left edge.
            .wrapContentWidth()
            .widthIn(max = animatedReadableWidth())
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        // Mono dateline over an italic display serif, matching Home's masthead.
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("YOUR COLLECTION", style = MetaLabel, color = xc.onContainerMuted)
            Spacer(Modifier.height(6.dp))
            Text("Library", style = HeroTitle.variedFace("Library"), color = xc.onContainer)
        }

        // ── Tabs ───────────────────────────────────────────────────────────
        // Filter chips rather than a tab bar: the selected one fills and squares off slightly, so
        // the active tab is identifiable by shape as well as by colour.
        ExpressiveChipRow(
            options = LibraryTab.entries.toList(),
            selected = tab,
            label = { it.label },
            icon = { it.icon },
            onSelect = { tab = it },
        )

        Box(modifier = Modifier.weight(1f)) { tabContent(tab) }
    }

    if (showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(shape = OutlinedTextFieldDefaults.shape, 
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(shape = ButtonDefaults.textShape, onClick = {
                    val name = newPlaylistName.trim()
                    if (name.isNotEmpty()) {
                        viewModel.createPlaylist(name)
                        showCreatePlaylist = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(shape = ButtonDefaults.textShape, onClick = { showCreatePlaylist = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SongList(
    songs: List<MusicItem>,
    onPlaySongs: (List<MusicItem>, Int) -> Unit,
    emptyHint: String,
) {
    val ext = LocalVerzaExtendedColors.current
    if (songs.isEmpty()) {
        CenterHint(emptyHint, ext.muted)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalBottomChrome.current),
        verticalArrangement = Arrangement.spacedBy(if (isGlass()) 0.dp else 6.dp),
    ) {
        items(items = songs, key = { it.id }) { song ->
            val art = rememberSongArtwork(song.title, song.artist, song.thumbnailUrl)
            LibraryRow(
                title = song.title,
                subtitle = song.artist,
                thumbnailUrl = art,
                onClick = { onPlaySongs(songs, songs.indexOf(song)) },
                trailing = { TrackActionsMenu(item = song) },
            )
        }
    }
}

/**
 * The "On device" tab: gates on the audio read permission, then scans MediaStore and lists local
 * tracks. Each row reuses [SongList] so play / like / add-to-playlist all work via the shared menu.
 */
@Composable
private fun LocalMusicContent(
    hasPermission: Boolean,
    songs: List<MusicItem>?,
    onRequestPermission: () -> Unit,
    onScan: () -> Unit,
    onPlaySongs: (List<MusicItem>, Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current

    if (!hasPermission) {
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = LocalBottomChrome.current).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.PhoneAndroid,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Play music stored on your device",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Grant access to your audio files to see and play them here. Verza only reads them — it never changes or deletes anything.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRequestPermission, shape = CircleShape) { Text("Grant access") }
        }
        return
    }

    // Permission held — scan once on entry, then render.
    LaunchedEffect(Unit) { onScan() }
    when {
        songs == null -> Box(modifier = Modifier.fillMaxSize().padding(bottom = LocalBottomChrome.current), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = colors.primary)
        }
        songs.isEmpty() -> CenterHint("No music found on this device", ext.muted)
        else -> SongList(songs, onPlaySongs, "")
    }
}

@Composable
private fun CollectionList(
    items: List<HomeItem>,
    onOpenItem: (HomeItem) -> Unit,
    circularArt: Boolean = false,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalBottomChrome.current),
        verticalArrangement = Arrangement.spacedBy(if (isGlass()) 0.dp else 6.dp),
    ) {
        items(items) { item ->
            LibraryRow(
                title = item.title,
                subtitle = item.subtitle.ifBlank { if (circularArt) "Artist" else "" },
                thumbnailUrl = item.thumbnailUrl,
                circularThumb = circularArt,
                onClick = { onOpenItem(item) },
            )
        }
    }
}

@Composable
private fun LibraryRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    circularThumb: Boolean = false,
    trailing: @Composable () -> Unit = {},
) {
    val colors = LocalExpressiveColors.current
    if (isGlass()) {
        GlassListRow(
            title = title,
            subtitle = subtitle,
            onClick = onClick,
            artworkUrl = thumbnailUrl ?: "",
            circularArt = circularThumb,
            trailing = trailing,
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLargeIncreased)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                // Artists round, records square. Same rule as Search, so a mixed list reads the
                // same way wherever you meet it.
                .clip(if (circularThumb) PillShape else ShapeMedium)
                .background(colors.surfaceHigh),
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            RowTitle(title, colors.onSurface)
            if (subtitle.isNotBlank()) RowSubtitle(subtitle, colors.onSurfaceMuted)
        }
        trailing()
    }
}

@Composable
private fun CenterHint(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.fillMaxSize().padding(bottom = LocalBottomChrome.current), contentAlignment = Alignment.Center) {
        if (isGlass()) GlassEmpty(text) else Text(text, style = BodyText, color = color)
    }
}

/**
 * Library in the Glass design: an index rather than a row of chips. Each kind of thing in the
 * library is a line with an icon and a chevron, opening as a page of its own with a way back, and
 * under the index the songs played most recently, as covers.
 */
@Composable
private fun GlassLibrary(
    recent: List<MusicItem>,
    onPlaySongs: (List<MusicItem>, Int) -> Unit,
    onCreatePlaylist: () -> Unit,
    content: @Composable (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val xc = LocalExpressiveColors.current
    var open by rememberSaveable { mutableStateOf<LibraryTab?>(null) }
    com.verza.ui.expressive.onTabReselected { open = null }
    BackHandler(enabled = open != null) { open = null }
    AnimatedContent(
        targetState = open,
        // The app's own push, with each page opaque: two see-through pages cross-fading showed both.
        transitionSpec = { com.verza.ui.expressive.pushTransform(forward = targetState != null) },
        modifier = modifier.fillMaxSize().background(xc.container),
        label = "libraryPage",
    ) { page ->
        Box(Modifier.fillMaxSize().background(xc.container)) {
            if (page == null) LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentWidth()
                    .widthIn(max = animatedReadableWidth())
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 16.dp + LocalBottomChrome.current),
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                        GlassMasthead("Your collection", "Library", Modifier.weight(1f))
                        GlassHeaderButton(Icons.Filled.Add, "New playlist", onCreatePlaylist)
                    }
                }
                items(LibraryTab.entries.toList().chunked(2)) { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                        for (tab in pair) GlassTile(tab.label, tab.icon, Modifier.weight(1f)) { open = tab }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (recent.isNotEmpty()) {
                    item {
                        Text(
                            "Recently played",
                            style = GlassSectionTitle,
                            color = xc.onContainer,
                            modifier = Modifier.padding(top = 30.dp, bottom = 12.dp),
                        )
                    }
                    val shown = recent.take(12)
                    items(shown.chunked(2)) { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(bottom = 18.dp)) {
                            for (song in pair) {
                                val art = rememberSongArtwork(song.title, song.artist, song.thumbnailUrl)
                                Column(Modifier.weight(1f).pressableScale { onPlaySongs(shown, shown.indexOf(song)) }) {
                                    AsyncImage(
                                        model = art,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(ShapeLarge).background(xc.surfaceHigh),
                                    )
                                    Spacer(Modifier.height(7.dp))
                                    Text(song.title, style = BodyText.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = xc.onContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, style = BodyText.copy(fontSize = 13.sp), color = xc.onContainerMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            } else Column(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentWidth()
                    .widthIn(max = animatedReadableWidth())
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp, top = 2.dp)
                        .clip(ShapeMedium)
                        .clickable { open = null }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = xc.accent, modifier = Modifier.size(28.dp))
                    Text("Library", style = BodyText.copy(fontSize = 17.sp), color = xc.accent)
                }
                Text(
                    page.label,
                    style = HeroTitle,
                    color = xc.onContainer,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                )
                Box(Modifier.weight(1f)) { content(page) }
            }
        }
    }
}

/**
 * One part of the library, as a tile: its icon and name on a grey glass-like surface, with the
 * accent glowing from one corner, the same light the dock's selected tab sits in.
 */
@Composable
private fun GlassTile(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val xc = LocalExpressiveColors.current
    Column(
        modifier = modifier
            .height(88.dp)
            .pressableScale(onClick = onClick)
            .clip(ShapeLargeIncreased)
            .background(xc.surface)
            .drawBehind {
                val r = size.width * 0.55f
                drawCircle(
                    androidx.compose.ui.graphics.Brush.radialGradient(
                        0f to xc.accent.copy(alpha = 0.11f),
                        1f to androidx.compose.ui.graphics.Color.Transparent,
                        center = Offset(size.width, 0f),
                        radius = r,
                    ),
                    radius = r,
                    center = Offset(size.width, 0f),
                )
            }
            .semantics(mergeDescendants = true) { role = androidx.compose.ui.semantics.Role.Button }
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(icon, contentDescription = null, tint = xc.accent, modifier = Modifier.size(24.dp))
        Text(label, style = BodyStrong.copy(fontSize = 16.sp), color = xc.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
