package com.verza.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlaylistPlay
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
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaShape
import com.verza.ui.verso.VersoMasthead
import com.verza.ui.verso.pebbleShape

private enum class LibraryTab(val label: String, val icon: ImageVector) {
    RECENT("Recent", Icons.Filled.History),
    LIKED("Liked", Icons.Filled.Favorite),
    LOCAL("On device", Icons.Filled.PhoneAndroid),
    DOWNLOADED("Downloaded", Icons.Filled.Download),
    PLAYLISTS("Playlists", Icons.Filled.PlaylistPlay),
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Verso masthead ─────────────────────────────────────────────────
        VersoMasthead(title = "library", eyebrow = "your collection")

        // ── Tabs ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LibraryTab.entries.forEach { entry ->
                LibraryPill(entry = entry, selected = tab == entry) { tab = entry }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                LibraryTab.RECENT ->
                    SongList(recent.map { it.toMusicItem() }, onPlaySongs, "Play something to see it here")
                LibraryTab.LIKED -> {
                    val likedItems =
                        if (isSignedIn && accountLiked.isNotEmpty()) accountLiked
                        else liked.map { it.toMusicItem() }
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
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    // "+ Create playlist" is always available, regardless of sign-in state.
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = { showCreatePlaylist = true; newPlaylistName = "" })
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(VerzaShape)
                                    .background(colors.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = colors.primary)
                            }
                            Text(
                                "Create playlist",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                            )
                        }
                    }
                    // Your local playlists.
                    if (localPlaylists.isNotEmpty()) {
                        items(localPlaylists) { p ->
                            LibraryRow(
                                title = p.playlist.name,
                                subtitle = "${p.trackCount} tracks",
                                thumbnailUrl = p.coverUrl,
                                onClick = { onOpenLocalPlaylist(p.playlist.id) },
                            )
                        }
                    }
                    // Section header for saved YT playlists (only when signed in).
                    if (isSignedIn && playlists.isNotEmpty()) {
                        item {
                            Text(
                                "Saved from YouTube Music",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.muted,
                                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
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
                                    "Sign in to also see your YT Music playlists",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ext.muted,
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
    }

    if (showCreatePlaylist) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("New playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text("Playlist name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newPlaylistName.trim()
                    if (name.isNotEmpty()) {
                        viewModel.createPlaylist(name)
                        showCreatePlaylist = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LibraryPill(entry: LibraryTab, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bg = if (selected) colors.primary else colors.primaryContainer.copy(alpha = 0.5f)
    val fg = if (selected) colors.onPrimary else colors.primary
    Row(
        modifier = Modifier
            .clip(remember(entry) { pebbleShape(entry.label, base = 15.dp, swing = 8.dp) })
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(entry.icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(entry.label.lowercase(), style = MaterialTheme.typography.labelLarge, color = fg)
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
        contentPadding = PaddingValues(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
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
            modifier = Modifier.fillMaxSize().padding(32.dp),
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
        songs == null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        contentPadding = PaddingValues(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
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
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(if (circularThumb) CircleShape else VerzaShape)
                .background(colors.surfaceVariant),
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}

@Composable
private fun CenterHint(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
