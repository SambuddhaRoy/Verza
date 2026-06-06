package com.verza.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.verza.data.StartScreen
import com.verza.innertube.AudioQuality
import com.verza.ui.components.EditorialSectionHeader
import com.verza.ui.components.pressableScale
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.DynamicColorSupported
import com.verza.ui.theme.GlowColorPreset
import com.verza.ui.theme.GlowIntensity
import com.verza.ui.theme.GlowStyle
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaTheme
import com.verza.ui.theme.resolveColor
import com.verza.ui.theme.toColorScheme

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
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val currentTheme by viewModel.theme.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()
    val glowEnabled by viewModel.glowEnabled.collectAsStateWithLifecycle()
    val glowColor by viewModel.glowColor.collectAsStateWithLifecycle()
    val glowIntensity by viewModel.glowIntensity.collectAsStateWithLifecycle()
    val glowStyle by viewModel.glowStyle.collectAsStateWithLifecycle()
    val glowReactive by viewModel.glowReactive.collectAsStateWithLifecycle()
    val startScreen by viewModel.startScreen.collectAsStateWithLifecycle()
    val resumeOnOpen by viewModel.resumeOnOpen.collectAsStateWithLifecycle()
    val skipSilence by viewModel.skipSilence.collectAsStateWithLifecycle()
    val albumArtMotion by viewModel.albumArtMotion.collectAsStateWithLifecycle()
    val saveSearchHistory by viewModel.saveSearchHistory.collectAsStateWithLifecycle()
    val sleeveMode by viewModel.sleeveMode.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val gentleStart by viewModel.gentleStart.collectAsStateWithLifecycle()
    var showResetStatsDialog by remember { mutableStateOf(false) }

    // ── Library backup (export / import) ────────────────────────────────────────
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val data = viewModel.exportLibraryJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) }
            }.isSuccess
            Toast.makeText(context, if (ok) "Library exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("empty file")
                viewModel.importLibraryJson(text)
            }.onSuccess { r ->
                Toast.makeText(context, "Imported ${r.songs} songs · ${r.playlists} playlists", Toast.LENGTH_LONG).show()
            }.onFailure {
                Toast.makeText(context, "Couldn't read that backup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(start = 12.dp, end = 20.dp, top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onBackground)
                    }
                    Spacer(Modifier.weight(1f))
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .width(40.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.primary),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Preferences",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onBackground,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        // ── Account ──────────────────────────────────────────────────────────
        item { SectionHeader("Account") }
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = if (isSignedIn) "Signed in to YouTube Music" else "Not signed in",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    Text(
                        text = if (isSignedIn)
                            "Your home feed, recommendations and library are personalised."
                        else
                            "Sign in to get your personal YT Music feed and library.",
                        style = CaptionItalic,
                        color = ext.muted,
                    )
                    if (isSignedIn) {
                        OutlinedButton(
                            onClick = viewModel::signOut,
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.Start),
                        ) { Text("Sign out") }
                    } else {
                        Button(
                            onClick = onSignIn,
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.Start),
                        ) { Text("Sign in") }
                    }
                }
            }
        }

        // ── Insights ─────────────────────────────────────────────────────────
        item { SectionHeader("Insights") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressableScale(onClick = onOpenStats)
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Your Sound", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                        Text("Listening stats — top tracks, artists, streaks", style = CaptionItalic, color = ext.muted)
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ext.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = ext.borderGlass)
            }
        }

        // ── General ──────────────────────────────────────────────────────────
        item { SectionHeader("General") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Start screen", style = CaptionItalic, color = ext.muted)
                SegmentedChoice(
                    options = StartScreen.entries,
                    selected = startScreen,
                    label = { it.label },
                    onSelect = viewModel::setStartScreen,
                )
            }
        }

        // ── Playback ─────────────────────────────────────────────────────────
        item { SectionHeader("Playback") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToggleRow(
                    title = "Resume on open",
                    subtitle = "Pick up where you left off when the app reopens",
                    checked = resumeOnOpen,
                    onToggle = viewModel::setResumeOnOpen,
                )
                ToggleRow(
                    title = "Skip silence",
                    subtitle = "Trim silent gaps within tracks",
                    checked = skipSilence,
                    onToggle = viewModel::setSkipSilence,
                )
                ToggleRow(
                    title = "Gentle start",
                    subtitle = "Ease the volume up when you resume — a soft sunrise",
                    checked = gentleStart,
                    onToggle = viewModel::setGentleStart,
                    divider = false,
                )
            }
        }

        // ── Audio quality ──────────────────────────────────────────────────────
        item { SectionHeader("Audio quality") }
        item {
            // Flat list with continuous hairlines — no inter-row gap so dividers form one rule.
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                AudioQualityRow(AudioQuality.HIGH, "High", "Best available bitrate", audioQuality == AudioQuality.HIGH) { viewModel.setAudioQuality(AudioQuality.HIGH) }
                AudioQualityRow(AudioQuality.MEDIUM, "Medium", "About 128 kbps", audioQuality == AudioQuality.MEDIUM) { viewModel.setAudioQuality(AudioQuality.MEDIUM) }
                AudioQualityRow(AudioQuality.LOW, "Low", "Data saver", audioQuality == AudioQuality.LOW) { viewModel.setAudioQuality(AudioQuality.LOW) }
            }
        }

        // ── Sound ───────────────────────────────────────────────────────────────
        item { SectionHeader("Sound") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressableScale(onClick = onOpenEqualizer)
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Equalizer", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                        Text("Tune the bands, bass, and volume leveling", style = CaptionItalic, color = ext.muted)
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ext.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = ext.borderGlass)
            }
        }

        // ── Appearance ─────────────────────────────────────────────────────────
        item { SectionHeader("Appearance") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToggleRow(
                    title = "Sleeve mode",
                    subtitle = "Editorial look — full-bleed poster player, translucent surfaces, " +
                        "all over the live cover-coloured glow",
                    checked = sleeveMode,
                    onToggle = viewModel::setSleeveMode,
                )
                ToggleRow(
                    title = "Album art motion",
                    subtitle = "Gently animate the cover while playing",
                    checked = albumArtMotion,
                    onToggle = viewModel::setAlbumArtMotion,
                    divider = false,
                )
            }
        }

        // ── Theme ──────────────────────────────────────────────────────────────
        item { SectionHeader("Theme") }
        items(VerzaTheme.entries.filter { it != VerzaTheme.DYNAMIC || DynamicColorSupported }) { theme ->
            ThemeRow(theme = theme, selected = theme == currentTheme) { viewModel.setTheme(theme) }
        }

        // ── Background glow ────────────────────────────────────────────────────
        // The glow now renders on light schemes too (as soft colour washes), so its controls are
        // available whenever it's enabled, regardless of theme.
        item { SectionHeader("Background glow") }
        item {
            GlowToggleRow(
                enabled = glowEnabled,
                onToggle = viewModel::setGlowEnabled,
                availableInTheme = true,
            )
        }
        if (glowEnabled) {
            item {
                GlowPatternRow(
                    selected = glowStyle,
                    onSelect = viewModel::setGlowStyle,
                )
            }
            item {
                GlowColorRow(
                    selected = glowColor,
                    onSelect = viewModel::setGlowColor,
                )
            }
            item {
                GlowIntensityRow(
                    selected = glowIntensity,
                    onSelect = viewModel::setGlowIntensity,
                )
            }
            item {
                GlowReactivityRow(
                    enabled = glowReactive,
                    onToggle = viewModel::setGlowReactive,
                )
            }
            item {
                MusicHapticsRow(
                    enabled = hapticsEnabled,
                    onToggle = viewModel::setHapticsEnabled,
                )
            }
        }

        // ── Search ───────────────────────────────────────────────────────────
        item { SectionHeader("Search") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToggleRow(
                    title = "Save search history",
                    subtitle = "Remember recent searches for quick access",
                    checked = saveSearchHistory,
                    onToggle = viewModel::setSaveSearchHistory,
                )
                ActionRow(
                    title = "Clear search history",
                    subtitle = "Remove all remembered searches",
                    onClick = viewModel::clearSearchHistory,
                    divider = false,
                )
            }
        }

        // ── Data ─────────────────────────────────────────────────────────────
        item { SectionHeader("Data") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                ActionRow(
                    title = "Export library",
                    subtitle = "Save your playlists, likes & stats to a file you own",
                    onClick = { exportLauncher.launch("verza-library-backup.json") },
                )
                ActionRow(
                    title = "Import library",
                    subtitle = "Merge a Verza backup from another device",
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                )
                ActionRow(
                    title = "Reset listening stats",
                    subtitle = "Wipe the play history behind Your Sound",
                    tint = colors.error,
                    onClick = { showResetStatsDialog = true },
                    divider = false,
                )
            }
        }

        // ── Help ─────────────────────────────────────────────────────────────
        item { SectionHeader("Help") }
        item {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                ActionRow(
                    title = "Take the tour",
                    subtitle = "A quick guide to every feature and where to find it",
                    onClick = onOpenTour,
                    divider = false,
                )
            }
        }

        // ── Credits ────────────────────────────────────────────────────────────
        item { Spacer(Modifier.height(8.dp)) }
        item { SectionHeader("About") }
        item {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("Verza", style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
                    Text(
                        "A YouTube Music client",
                        style = CaptionItalic,
                        color = ext.muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Designed and built by",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.muted,
                    )
                    Text(
                        "Sambuddha Roy",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.primary,
                    )
                    Spacer(Modifier.height(12.dp))
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    Text(
                        "Privacy policy",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                runCatching {
                                    ctx.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://github.com/SambuddhaRoy/Verza/blob/main/PRIVACY.md"),
                                        ),
                                    )
                                }
                            }
                            .padding(8.dp),
                    )
                }
            }
        }
    }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            title = { Text("Reset listening stats?") },
            text = { Text("This permanently clears the play history behind Your Sound. Your liked songs and playlists are untouched.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetListeningStats()
                    showResetStatsDialog = false
                }) { Text("Reset", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    EditorialSectionHeader(title = title)
}

/** Title + subtitle row with a trailing Switch, hairline rule below (unless [divider] is false). */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    divider: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                Text(subtitle, style = CaptionItalic, color = ext.muted)
            }
            Switch(checked = checked, onCheckedChange = onToggle)
        }
        if (divider) HorizontalDivider(thickness = 0.5.dp, color = ext.borderGlass)
    }
}

/** Tappable title + subtitle row (for one-shot actions like clear/reset). */
@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color? = null,
    divider: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pressableScale(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = tint ?: colors.onBackground)
            Text(subtitle, style = CaptionItalic, color = ext.muted)
        }
        if (divider) HorizontalDivider(thickness = 0.5.dp, color = ext.borderGlass)
    }
}

/** Horizontal segmented selector — a tracked row of pill options with one active. */
@Composable
private fun <T> SegmentedChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Surface(
                onClick = { onSelect(option) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) colors.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) colors.primary else ext.borderGlass,
                ),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) colors.onBackground else ext.muted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun AudioQualityRow(
    quality: AudioQuality,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    // Hairline-rule treatment: no card, no fill. Selected state is communicated by a small
    // filled primary bullet on the left and the title shifting to primary; cleaner and more
    // editorial than a tinted background.
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressableScale(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Bullet — 6 dp filled primary circle when selected, hollow outline otherwise.
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.primary else Color.Transparent)
                    .border(
                        width = if (selected) 0.dp else 1.dp,
                        color = ext.muted,
                        shape = CircleShape,
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) colors.primary else colors.onBackground,
                )
                Text(description, style = CaptionItalic, color = ext.muted)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = ext.borderGlass)
    }
}

// ── Glow rows ────────────────────────────────────────────────────────────────

@Composable
private fun GlowPatternRow(selected: GlowStyle, onSelect: (GlowStyle) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Pattern", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
        SegmentedChoice(
            options = GlowStyle.entries,
            selected = selected,
            label = { it.displayName },
            onSelect = onSelect,
        )
        Text(
            "Loom weaves soft geometric threads through the flow (needs Android 13+).",
            style = CaptionItalic,
            color = ext.muted,
        )
    }
}

@Composable
private fun GlowToggleRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    availableInTheme: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Ambient glow",
                style = MaterialTheme.typography.titleMedium,
                color = if (availableInTheme) colors.onBackground else ext.muted,
            )
            Text(
                text = if (availableInTheme)
                    "Soft warm halo behind the content."
                else
                    "Only visible on dark themes.",
                style = CaptionItalic,
                color = ext.muted,
            )
        }
        Switch(
            checked = enabled,
            enabled = availableInTheme,
            onCheckedChange = onToggle,
        )
    }
}

@Composable
private fun GlowReactivityRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // We re-check the permission every recomposition rather than caching it, so a user who
    // grants the permission via system settings while this screen is open sees the toggle
    // light up immediately.
    val hasPermission = remember(enabled) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Launcher for the runtime permission ask. If granted, persist the toggle; if denied,
    // surface a brief Toast so the user knows why nothing changed and where to flip it on.
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onToggle(true)
        } else {
            android.widget.Toast.makeText(
                context,
                "Sound reactivity needs the audio permission. You can grant it later in system settings.",
                android.widget.Toast.LENGTH_LONG,
            ).show()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Sound reactivity",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground,
            )
            Text(
                text = "Glow moves with the music. Reads playback audio only — never the microphone. Needs the audio permission.",
                style = CaptionItalic,
                color = ext.muted,
            )
        }
        Switch(
            checked = enabled && hasPermission,
            onCheckedChange = { newState ->
                if (newState) {
                    if (hasPermission) onToggle(true)
                    else permLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                } else {
                    onToggle(false)
                }
            },
        )
    }
}

@Composable
private fun MusicHapticsRow(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val context = LocalContext.current

    val hasPermission = remember(enabled) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onToggle(true)
        else Toast.makeText(
            context,
            "Music haptics needs the audio permission. You can grant it later in system settings.",
            Toast.LENGTH_LONG,
        ).show()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Feel the beat",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground,
            )
            Text(
                text = "Subtle vibration in time with the bass. Reads playback audio only — never the microphone. Needs the audio permission.",
                style = CaptionItalic,
                color = ext.muted,
            )
        }
        Switch(
            checked = enabled && hasPermission,
            onCheckedChange = { newState ->
                if (newState) {
                    if (hasPermission) onToggle(true)
                    else permLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                } else {
                    onToggle(false)
                }
            },
        )
    }
}

@Composable
private fun GlowColorRow(
    selected: GlowColorPreset,
    onSelect: (GlowColorPreset) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val available = GlowColorPreset.entries.filter {
        it != GlowColorPreset.SYSTEM || com.verza.ui.theme.DynamicColorSupported
    }
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Glow color",
            style = MaterialTheme.typography.labelSmall,
            color = ext.muted,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            available.forEach { preset ->
                val swatch = preset.resolveColor()
                val isSelected = preset == selected
                // "From album art" has no fixed colour — show a multi-hue sweep so the swatch
                // reads as "adaptive" rather than a single flat colour.
                val swatchModifier = if (preset == GlowColorPreset.ALBUM_ART) {
                    Modifier.background(
                        androidx.compose.ui.graphics.Brush.sweepGradient(
                            listOf(
                                androidx.compose.ui.graphics.Color(0xFFE0556E),
                                androidx.compose.ui.graphics.Color(0xFFE8B14A),
                                androidx.compose.ui.graphics.Color(0xFF5A8068),
                                androidx.compose.ui.graphics.Color(0xFF6B8BA8),
                                androidx.compose.ui.graphics.Color(0xFFE0556E),
                            )
                        )
                    )
                } else {
                    Modifier.background(swatch)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(preset) }
                        .padding(6.dp),
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .then(swatchModifier)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) colors.onBackground else ext.borderGlass,
                                shape = CircleShape,
                            ),
                    )
                    Text(
                        preset.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) colors.onBackground else ext.muted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun GlowIntensityRow(
    selected: GlowIntensity,
    onSelect: (GlowIntensity) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Intensity",
            style = MaterialTheme.typography.labelSmall,
            color = ext.muted,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlowIntensity.entries.forEach { intensity ->
                val isSelected = intensity == selected
                Surface(
                    onClick = { onSelect(intensity) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) colors.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) colors.primary else ext.borderGlass,
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = intensity.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) colors.onBackground else ext.muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(theme: VerzaTheme, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val scheme = remember(theme) { theme.toColorScheme() }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressableScale(onClick = onClick)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Swatch trio — kept; this is the only place the theme is *visible* in the row.
            Row(modifier = Modifier.clip(RoundedCornerShape(6.dp))) {
                Box(Modifier.size(20.dp).background(scheme.background))
                Box(Modifier.size(20.dp).background(scheme.primary))
                Box(Modifier.size(20.dp).background(scheme.secondary))
                Box(Modifier.size(20.dp).background(scheme.tertiary))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    theme.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) colors.primary else colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when {
                        theme == VerzaTheme.DYNAMIC -> "Follows system"
                        theme.isLight -> "Light"
                        else -> "Dark"
                    },
                    style = CaptionItalic,
                    color = ext.muted,
                )
            }
            // Quiet 6-dp bullet selected state, matching AudioQualityRow.
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (selected) colors.primary else Color.Transparent)
                    .border(
                        width = if (selected) 0.dp else 1.dp,
                        color = ext.muted,
                        shape = CircleShape,
                    ),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = ext.borderGlass)
    }
}
