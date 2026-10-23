package com.verza.ui.screens

import com.verza.ui.expressive.LocalBottomChrome
import androidx.compose.ui.unit.sp
import com.verza.ui.theme.DesignScheme
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.ButtonDefaults
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.widthIn
import com.verza.ui.expressive.animatedReadableWidth
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.verza.innertube.AudioQuality
import com.verza.data.CrashLog
import com.verza.data.StartScreen
import com.verza.ui.expressive.AccentSource
import com.verza.ui.expressive.BodyStrong
import com.verza.ui.expressive.BodyText
import com.verza.ui.expressive.ColorFlavour
import com.verza.ui.expressive.ExpressiveMotion
import com.verza.ui.expressive.HeroTitle
import com.verza.ui.expressive.LocalExpressiveColors
import com.verza.ui.expressive.MetaLabel
import com.verza.ui.expressive.PillShape
import com.verza.ui.expressive.ShapeExtraLarge
import com.verza.ui.expressive.ShapeLargeIncreased
import com.verza.ui.expressive.ShapeMedium
import com.verza.ui.expressive.expressiveColorsFrom
import com.verza.ui.theme.LocalArtworkColors

/**
 * Settings, grouped by what a setting is for rather than by when it was written.
 *
 * The old screen had fifteen sections in the order features had arrived, so "Sound" and "Now
 * Playing" both held audio controls, "Appearance" and "Colour" were separate, and the equalizer was
 * three screens from the thing it affects. The order here is: who you are, what you hear, what you
 * see, where files go, what Verza has learned about you, and finally the app itself.
 *
 * Every row is one of four shapes, so the page has a rhythm rather than fifteen bespoke layouts:
 * a switch, a link, a set of choices, or a card.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenTour: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val colors = LocalExpressiveColors.current
    val cover = LocalArtworkColors.current
    val context = LocalContext.current

    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val flavour by viewModel.colorFlavour.collectAsStateWithLifecycle()
    val design by viewModel.designScheme.collectAsStateWithLifecycle()
    val accentSource by viewModel.accentSource.collectAsStateWithLifecycle()
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()
    val startScreen by viewModel.startScreen.collectAsStateWithLifecycle()
    val resumeOnOpen by viewModel.resumeOnOpen.collectAsStateWithLifecycle()
    val skipSilence by viewModel.skipSilence.collectAsStateWithLifecycle()
    val gentleStart by viewModel.gentleStart.collectAsStateWithLifecycle()
    val crossfade by viewModel.crossfadeSeconds.collectAsStateWithLifecycle()
    val albumArtMotion by viewModel.albumArtMotion.collectAsStateWithLifecycle()
    val washBeat by viewModel.washBeat.collectAsStateWithLifecycle()
    val saveSearchHistory by viewModel.saveSearchHistory.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val spectrumOn by viewModel.glowReactive.collectAsStateWithLifecycle()
    val downloadTree by viewModel.downloadTree.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()

    var crashReport by remember { mutableStateOf(CrashLog.read(context)) }
    var confirmResetStats by remember { mutableStateOf(false) }
    var showListenBrainz by remember { mutableStateOf(false) }
    val lbUser by viewModel.listenBrainzUser.collectAsStateWithLifecycle()

    val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val audioPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.setGlowReactive(true) }
    val beatPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.setWashBeat(true) }

    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            // Without a persistable grant the permission dies with the process and the next
            // download silently falls back to app storage.
            val kept = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }.isSuccess
            if (kept) {
                viewModel.setDownloadTree(uri.toString())
                toast(context, "Downloads will be saved here")
            } else {
                toast(context, "Couldn't keep access to that folder")
            }
        }
    }

    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val exportFile = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val data = viewModel.exportLibraryJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) }
            }.isSuccess
            toast(context, if (ok) "Library exported" else "Export failed")
        }
    }

    val importFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()
                    ?.use { it.readText() } ?: error("empty file")
                viewModel.importLibraryJson(text)
            }.onSuccess { toast(context, "Imported ${it.songs} songs and ${it.playlists} playlists") }
                .onFailure { toast(context, "Couldn't read that backup") }
        }
    }

    // A settings row stretched across a thirteen inch tablet is one enormous line with a switch
    // marooned at the far end. The column stops growing and centres instead.
    Box(
        modifier = modifier.fillMaxSize().background(colors.container),
        contentAlignment = Alignment.TopCenter,
    ) {
    LazyColumn(
        modifier = Modifier
            // Width after the cap: fillMaxSize first fixes the width to the screen and the cap never bites.
            .fillMaxHeight()
            .widthIn(max = animatedReadableWidth())
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp + LocalBottomChrome.current),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                com.verza.ui.expressive.ExpressiveControl(
                    onClick = onBack,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    container = colors.surface,
                    content = colors.onSurface,
                    iconSize = 22.dp,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text("Settings", style = HeroTitle, color = colors.onContainer)
            }
        }

        // ── account ─────────────────────────────────────────────────────────
        item {
            AccountCard(
                signedIn = isSignedIn,
                onSignIn = onSignIn,
                onSignOut = viewModel::signOut,
            )
        }

        // ── what you hear ───────────────────────────────────────────────────
        group("Playback") {
            SwitchRow(
                title = "Resume on open",
                subtitle = "Pick up where you left off",
                checked = resumeOnOpen,
                onToggle = viewModel::setResumeOnOpen,
            )
            SwitchRow(
                title = "Gentle start",
                subtitle = "Ease the volume up when playback resumes",
                checked = gentleStart,
                onToggle = viewModel::setGentleStart,
            )
            SwitchRow(
                title = "Skip silence",
                subtitle = "Trim silent gaps inside a track",
                checked = skipSilence,
                onToggle = viewModel::setSkipSilence,
            )
            ChoiceRow(
                // Named for what it does. It fades out and back in rather than overlapping the two
                // tracks, and calling it crossfade would be promising the thing it is not.
                title = "Fade between tracks",
                subtitle = if (crossfade == 0) {
                    "Tracks change on a hard cut"
                } else {
                    "Each track fades out and the next fades in, over $crossfade seconds"
                },
                options = CROSSFADE_STEPS,
                selected = crossfade,
                label = { if (it == 0) "Off" else "${it}s" },
                onSelect = viewModel::setCrossfadeSeconds,
            )
            ChoiceRow(
                title = "Audio quality",
                subtitle = when (audioQuality) {
                    AudioQuality.HIGH -> "The best bitrate the track offers"
                    AudioQuality.MEDIUM -> "About 128 kbps"
                    AudioQuality.LOW -> "The smallest stream, for saving data"
                },
                options = listOf(AudioQuality.LOW, AudioQuality.MEDIUM, AudioQuality.HIGH),
                selected = audioQuality,
                label = { it.name.lowercase().replaceFirstChar(Char::titlecase) },
                onSelect = viewModel::setAudioQuality,
            )
        }

        group("Sound") {
            LinkRow(
                title = "Equalizer",
                subtitle = "Ten bands, bass boost and loudness",
                onClick = onOpenEqualizer,
            )
            SwitchRow(
                title = "Spectrum seek bar",
                subtitle = if (hasAudioPermission) {
                    "The played part of the progress bar becomes a live spectrum"
                } else {
                    "Needs the audio capture permission. It reads Verza's own playback, never the microphone."
                },
                checked = spectrumOn && hasAudioPermission,
                onToggle = { on ->
                    if (on && !hasAudioPermission) {
                        audioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.setGlowReactive(on)
                    }
                },
            )
            SwitchRow(
                title = "Feel the beat",
                subtitle = if (hasAudioPermission) {
                    "A soft vibration on the bass"
                } else {
                    "Needs the same audio capture permission as the spectrum"
                },
                checked = hapticsEnabled && hasAudioPermission,
                onToggle = { on ->
                    if (on && !hasAudioPermission) {
                        audioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.setHapticsEnabled(on)
                    }
                },
            )
        }

        // ── what you see ────────────────────────────────────────────────────
        group("Design") {
            items(DesignScheme.entries.toList()) { option ->
                DesignRow(
                    design = option,
                    selected = option == design,
                    onClick = { viewModel.setDesignScheme(option) },
                )
            }
        }

        // Glass is black and white with one red; the cover's colours have nothing to decide there.
        if (design != DesignScheme.GLASS) group("Colour") {
            item {
                Text(
                    "Verza takes its colours from the cover. This decides how far to push them.",
                    style = BodyText,
                    color = colors.onContainerMuted,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                )
            }
            items(ColorFlavour.entries.toList()) { option ->
                FlavourRow(
                    flavour = option,
                    selected = option == flavour,
                    onClick = { viewModel.setColorFlavour(option) },
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            items(AccentSource.entries.toList()) { option ->
                AccentRow(
                    source = option,
                    selected = option == accentSource,
                    preview = remember(cover, flavour, option) {
                        expressiveColorsFrom(cover, flavour, option).accent
                    },
                    onClick = { viewModel.setAccentSource(option) },
                )
            }
        }

        group("Appearance") {
            SwitchRow(
                title = if (design == DesignScheme.GLASS) "Moving background" else "Album art motion",
                subtitle = if (design == DesignScheme.GLASS) {
                    "The cover flows behind Now Playing"
                } else {
                    "The cover breathes with the bass"
                },
                checked = albumArtMotion,
                onToggle = viewModel::setAlbumArtMotion,
            )
            // The flow and the beat are separate: some want the movement without the pulse.
            if (design == DesignScheme.GLASS && albumArtMotion) SwitchRow(
                title = "Move to the beat",
                subtitle = if (hasAudioPermission) {
                    "The background swells with the bass and pulses on each kick"
                } else {
                    "Needs the same audio capture permission as the spectrum"
                },
                checked = washBeat && hasAudioPermission,
                onToggle = { on ->
                    if (on && !hasAudioPermission) {
                        beatPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                    } else {
                        viewModel.setWashBeat(on)
                    }
                },
            )
            ChoiceRow(
                title = "Start screen",
                subtitle = "Where Verza opens",
                options = StartScreen.entries.toList(),
                selected = startScreen,
                label = { it.label },
                onSelect = viewModel::setStartScreen,
            )
        }

        // ── where things go ─────────────────────────────────────────────────
        group("Downloads") {
            LinkRow(
                title = "Save music to",
                subtitle = if (downloadTree.isBlank()) {
                    "Music/Verza"
                } else {
                    viewModel.downloadFolderLabel(downloadTree)
                },
                onClick = { pickFolder.launch(null) },
            )
            if (downloadTree.isNotBlank()) {
                LinkRow(
                    title = "Use the Music folder instead",
                    subtitle = "Back to Music/Verza. Files already saved stay where they are.",
                    onClick = { viewModel.setDownloadTree(""); toast(context, "Back to Music/Verza") },
                )
            }
        }

        // ── what Verza knows ────────────────────────────────────────────────
        group("Your library") {
            LinkRow(
                title = "Your Sound",
                subtitle = "Top tracks, artists and genres",
                onClick = onOpenStats,
            )
            LinkRow(
                title = "Export",
                subtitle = "Save playlists, likes and stats to a file you own",
                onClick = { exportFile.launch("verza-library-backup.json") },
            )
            LinkRow(
                title = "Import",
                subtitle = "Merge a backup from another device",
                onClick = { importFile.launch(arrayOf("application/json", "text/plain", "*/*")) },
            )
            LinkRow(
                title = "Reset listening stats",
                subtitle = "Wipe the play history behind Your Sound",
                onClick = { confirmResetStats = true },
                destructive = true,
            )
        }

        group("Listening elsewhere") {
            LinkRow(
                title = "ListenBrainz",
                subtitle = if (lbUser.isBlank()) "Your Spotify listening on Home: Weekly Exploration, Weekly Jams and more"
                else "Connected as $lbUser",
                onClick = { showListenBrainz = true },
            )
        }

        group("Search") {
            SwitchRow(
                title = "Save search history",
                subtitle = "Remember recent searches",
                checked = saveSearchHistory,
                onToggle = viewModel::setSaveSearchHistory,
            )
            LinkRow(
                title = "Clear search history",
                subtitle = "Forget everything searched so far",
                onClick = viewModel::clearSearchHistory,
            )
        }

        // ── the app itself ──────────────────────────────────────────────────
        group("Updates") {
            item { UpdateRow(state = updateState, viewModel = viewModel) }
        }

        group("About") {
            LinkRow(
                title = "Take the tour",
                subtitle = "A quick guide to what is where",
                onClick = onOpenTour,
            )
            crashReport?.let { report ->
                LinkRow(
                    title = "Send the last crash report",
                    subtitle = "Verza closed unexpectedly. This contains the error and your Android version, nothing else.",
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Verza crash report")
                            putExtra(Intent.EXTRA_TEXT, report)
                        }
                        context.startActivity(Intent.createChooser(send, "Send crash report"))
                        CrashLog.clear(context)
                        crashReport = null
                    },
                )
            }
            item { VersionCard() }
        }
    }
    }

    if (showListenBrainz) {
        ListenBrainzDialog(
            viewModel = viewModel,
            connectedAs = lbUser,
            onDismiss = { showListenBrainz = false; viewModel.clearListenBrainzMessage() },
        )
    }

    if (confirmResetStats) {
        AlertDialog(
            onDismissRequest = { confirmResetStats = false },
            containerColor = colors.surface,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurfaceMuted,
            shape = ShapeExtraLarge,
            title = { Text("Reset listening stats?", style = BodyStrong) },
            text = {
                Text(
                    "Your play history is what builds Your Sound, your mixes and the home page. " +
                        "This cannot be undone.",
                    style = BodyText,
                )
            },
            confirmButton = {
                TextButton(shape = ButtonDefaults.textShape, onClick = { viewModel.resetListeningStats(); confirmResetStats = false }) {
                    Text("Reset", color = colors.accent, style = BodyStrong)
                }
            },
            dismissButton = {
                TextButton(shape = ButtonDefaults.textShape, onClick = { confirmResetStats = false }) {
                    Text("Cancel", color = colors.onSurfaceMuted, style = BodyText)
                }
            },
        )
    }
}

/**
 * Connect a ListenBrainz account. Spotify feeds your listening into ListenBrainz, which is the open
 * way to get it: Spotify's own API no longer gives new apps recommendations or its playlists.
 */
@Composable
private fun ListenBrainzDialog(viewModel: SettingsViewModel, connectedAs: String, onDismiss: () -> Unit) {
    val colors = LocalExpressiveColors.current
    val state by viewModel.lbState.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf(connectedAs) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceMuted,
        shape = ShapeExtraLarge,
        title = { Text("ListenBrainz", style = BodyStrong) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Connect Spotify (or import Last.fm) at listenbrainz.org, then enter your username here. " +
                        "Your weekly playlists and last month's most played appear on Home and play through Verza.",
                    style = BodyText,
                )
                androidx.compose.material3.OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    enabled = connectedAs.isBlank() && !state.busy,
                    singleLine = true,
                    label = { Text("Username") },
                    shape = ShapeMedium,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        autoCorrectEnabled = false,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { viewModel.connectListenBrainz(input) },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.message?.let { Text(it, style = BodyText, color = colors.onSurface) }
            }
        },
        confirmButton = {
            TextButton(
                shape = ButtonDefaults.textShape,
                enabled = !state.busy,
                onClick = {
                    if (connectedAs.isBlank()) viewModel.connectListenBrainz(input)
                    else { viewModel.disconnectListenBrainz(); input = "" }
                },
            ) {
                Text(
                    if (state.busy) "Connecting" else if (connectedAs.isBlank()) "Connect" else "Disconnect",
                    color = colors.accent,
                    style = BodyStrong,
                )
            }
        },
        dismissButton = {
            TextButton(shape = ButtonDefaults.textShape, onClick = onDismiss) {
                Text("Close", color = colors.onSurfaceMuted, style = BodyText)
            }
        },
    )
}

private fun toast(context: android.content.Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}

/** Crossfade lengths worth offering. Beyond about twelve seconds it stops being a transition. */
private val CROSSFADE_STEPS = listOf(0, 2, 4, 6, 8, 12)

// ── the four row shapes ──────────────────────────────────────────────────────

/**
 * A titled group.
 *
 * Written as an extension on the list scope so a section reads as its contents rather than as a
 * pile of `item { }` wrappers, which is what made the old file hard to reorder.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.group(
    title: String,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    item { GroupHeader(title) }
    content()
    item { Spacer(Modifier.height(18.dp)) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) = item { SettingSwitch(title, subtitle, checked, onToggle) }

private fun androidx.compose.foundation.lazy.LazyListScope.LinkRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
) = item { SettingLink(title, subtitle, onClick, destructive) }

private fun <T> androidx.compose.foundation.lazy.LazyListScope.ChoiceRow(
    title: String,
    subtitle: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) = item { SettingChoice(title, subtitle, options, selected, label, onSelect) }

@Composable
private fun GroupHeader(title: String) {
    val colors = LocalExpressiveColors.current
    Text(
        text = title.uppercase(),
        style = MetaLabel,
        color = colors.accent,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 10.dp),
    )
}

/** The shared body of a tappable row: a squash on press, and a card that fills the width. */
@Composable
private fun RowSurface(
    onClick: () -> Unit,
    /** Non-null makes the row a switch to accessibility services, announcing on or off. */
    switchState: Boolean? = null,
    content: RowScopeContent,
) {
    val colors = LocalExpressiveColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = ExpressiveMotion.spatialFast(),
        label = "settingPress",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .scale(scale)
            .clip(ShapeLargeIncreased)
            .background(colors.surface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            // The pill is drawn by hand, so nothing told TalkBack this row was a switch or whether it
            // was on: it read as a plain button, with no way to know the setting's state.
            .then(
                if (switchState == null) Modifier
                else Modifier.semantics(mergeDescendants = true) {
                    role = Role.Switch
                    toggleableState = ToggleableState(switchState)
                },
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

private typealias RowScopeContent = @Composable androidx.compose.foundation.layout.RowScope.() -> Unit

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalExpressiveColors.current
    RowSurface(onClick = { onToggle(!checked) }, switchState = checked) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = BodyStrong, color = colors.onSurface)
            Text(subtitle, style = BodyText, color = colors.onSurfaceMuted)
        }
        Spacer(Modifier.width(14.dp))
        // A pill that fills rather than a Material switch, so it matches the chips and the nav bar.
        val trackWidth by animateFloatAsState(
            targetValue = if (checked) 1f else 0f,
            animationSpec = ExpressiveMotion.spatialDefault(),
            label = "switchTrack",
        )
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(PillShape)
                .background(
                    androidx.compose.ui.graphics.lerp(colors.surfaceHigh, colors.accent, trackWidth),
                ),
            // The knob rides the alignment bias from one end to the other, so the spring moves it
            // without a layout pass per frame.
            contentAlignment = androidx.compose.ui.BiasAlignment(-1f + 2f * trackWidth, 0f),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(24.dp)
                    .clip(PillShape)
                    .background(if (checked) colors.onAccent else colors.onSurfaceMuted),
            )
        }
    }
}

@Composable
private fun SettingLink(title: String, subtitle: String, onClick: () -> Unit, destructive: Boolean) {
    val colors = LocalExpressiveColors.current
    RowSurface(onClick = onClick) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = BodyStrong,
                color = if (destructive) colors.accent else colors.onSurface,
            )
            Text(subtitle, style = BodyText, color = colors.onSurfaceMuted)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = colors.onSurfaceMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun <T> SettingChoice(
    title: String,
    subtitle: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val colors = LocalExpressiveColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(ShapeLargeIncreased)
            .background(colors.surface)
            .padding(18.dp),
    ) {
        Text(title, style = BodyStrong, color = colors.onSurface)
        Text(subtitle, style = BodyText, color = colors.onSurfaceMuted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            for (option in options) {
                val isSelected = option == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(PillShape)
                        .background(if (isSelected) colors.accent else colors.surfaceHigh)
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option),
                        style = BodyText,
                        color = if (isSelected) colors.onAccent else colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ── the pieces that are not rows ─────────────────────────────────────────────

@Composable
private fun AccountCard(signedIn: Boolean, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    val colors = LocalExpressiveColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ShapeLargeIncreased)
            .background(colors.surface)
            .padding(20.dp),
    ) {
        Text(
            if (signedIn) "Signed in to YouTube Music" else "Not signed in",
            style = BodyStrong,
            color = colors.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (signedIn) {
                "Your home feed, library and recommendations are yours."
            } else {
                "Sign in to bring your feed, playlists and liked songs along. Everything else works without it."
            },
            style = BodyText,
            color = colors.onSurfaceMuted,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .height(46.dp)
                .clip(PillShape)
                .background(if (signedIn) colors.surfaceHigh else colors.accent)
                .clickable(onClick = if (signedIn) onSignOut else onSignIn)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (signedIn) "Sign out" else "Sign in",
                style = BodyStrong,
                color = if (signedIn) colors.onSurface else colors.onAccent,
            )
        }
    }
}

/**
 * One design option. Its swatch is drawn in that design's own shapes (a rounded tile beside a pill,
 * or two hard squares) so the difference is visible before it is picked, rather than only once the
 * whole app has redrawn around you.
 */
@Composable
private fun DesignRow(design: DesignScheme, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalExpressiveColors.current
    RowSurface(onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (design == DesignScheme.GLASS) {
                // Glass: the accent, and a clear pill with a lit edge.
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(percent = 50)).background(colors.accent))
                Box(
                    Modifier.size(width = 30.dp, height = 22.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(colors.onSurface.copy(alpha = 0.12f))
                        .border(1.dp, colors.onSurface.copy(alpha = 0.35f), RoundedCornerShape(percent = 50)),
                )
            } else {
            Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(colors.accent))
            Box(Modifier.size(width = 30.dp, height = 22.dp).clip(RoundedCornerShape(percent = 50)).background(colors.onSurfaceMuted))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(design.displayName, style = BodyStrong, color = colors.onSurface)
                if (design.experimental) {
                    Spacer(Modifier.width(8.dp))
                    // Still changing between releases, and said so where it is chosen.
                    Text(
                        "EXPERIMENTAL",
                        style = MetaLabel.copy(fontSize = 10.sp),
                        color = colors.onAccent,
                        modifier = Modifier.background(colors.accent).padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            Text(design.blurb, style = BodyText, color = colors.onSurfaceMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        SelectedDot(selected)
    }
}

@Composable
private fun FlavourRow(flavour: ColorFlavour, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalExpressiveColors.current
    val cover = LocalArtworkColors.current
    val preview = remember(cover, flavour) { expressiveColorsFrom(cover, flavour) }
    RowSurface(onClick = onClick) {
        Row(modifier = Modifier.clip(ShapeMedium)) {
            Box(Modifier.size(22.dp).background(preview.container))
            Box(Modifier.size(22.dp).background(preview.surface))
            Box(Modifier.size(22.dp).background(preview.accent))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(flavour.displayName, style = BodyStrong, color = colors.onSurface)
            Text(flavour.blurb, style = BodyText, color = colors.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SelectedDot(selected)
    }
}

@Composable
private fun AccentRow(source: AccentSource, selected: Boolean, preview: Color, onClick: () -> Unit) {
    val colors = LocalExpressiveColors.current
    RowSurface(onClick = onClick) {
        Box(Modifier.size(22.dp).clip(PillShape).background(preview))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(source.displayName, style = BodyStrong, color = colors.onSurface)
            Text(source.blurb, style = BodyText, color = colors.onSurfaceMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        SelectedDot(selected)
    }
}

@Composable
private fun SelectedDot(selected: Boolean) {
    val colors = LocalExpressiveColors.current
    Box(
        modifier = Modifier.size(22.dp).clip(PillShape).background(
            if (selected) colors.accent else Color.Transparent,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = "Selected", tint = colors.onAccent, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun UpdateRow(state: SettingsViewModel.UpdateState, viewModel: SettingsViewModel) {
    when (state) {
        is SettingsViewModel.UpdateState.Idle -> SettingLink(
            "Check for updates",
            "Verza is sideloaded, so it looks for new versions on GitHub itself",
            viewModel::checkForUpdate,
            false,
        )
        is SettingsViewModel.UpdateState.Checking -> SettingLink(
            "Checking", "Asking GitHub for the latest release", {}, false,
        )
        is SettingsViewModel.UpdateState.UpToDate -> SettingLink(
            "Up to date",
            "You are on ${com.verza.BuildConfig.VERSION_NAME}. Tap to check again.",
            viewModel::checkForUpdate,
            false,
        )
        is SettingsViewModel.UpdateState.Available -> SettingLink(
            "Download ${state.release.version}",
            state.release.notes.lineSequence().firstOrNull().orEmpty().ifBlank { "A new version is ready" },
            { viewModel.downloadUpdate(state.release) },
            false,
        )
        is SettingsViewModel.UpdateState.Downloading -> SettingLink(
            "Downloading ${(state.progress * 100).toInt()}%",
            "Keep Verza open until this finishes",
            {},
            false,
        )
        is SettingsViewModel.UpdateState.Ready -> SettingLink(
            "Install ${state.version}",
            "Android will ask you to confirm",
            { viewModel.installUpdate(state.file) },
            false,
        )
        is SettingsViewModel.UpdateState.Failed -> SettingLink(
            "Update failed",
            "${state.message}. Tap to try again.",
            viewModel::checkForUpdate,
            false,
        )
    }
}

@Composable
private fun VersionCard() {
    val colors = LocalExpressiveColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(ShapeLargeIncreased)
            .background(colors.surface)
            .padding(20.dp),
    ) {
        Text("Verza", style = HeroTitle, color = colors.onSurface)
        Spacer(Modifier.height(4.dp))
        Text(
            "Version ${com.verza.BuildConfig.VERSION_NAME}",
            style = MetaLabel,
            color = colors.onSurfaceMuted,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "An unofficial YouTube Music client. No account of its own, no telemetry, no ads.",
            style = BodyText,
            color = colors.onSurfaceMuted,
        )
    }
}
