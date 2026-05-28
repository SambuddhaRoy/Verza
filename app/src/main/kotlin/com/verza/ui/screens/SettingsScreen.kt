package com.verza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verza.innertube.AudioQuality
import com.verza.ui.theme.DynamicColorSupported
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaTheme
import com.verza.ui.theme.toColorScheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val currentTheme by viewModel.theme.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()

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
                        style = MaterialTheme.typography.bodySmall,
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

        // ── Audio quality ──────────────────────────────────────────────────────
        item { SectionHeader("Audio quality") }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AudioQualityRow(AudioQuality.HIGH, "High", "Best available bitrate", audioQuality == AudioQuality.HIGH) { viewModel.setAudioQuality(AudioQuality.HIGH) }
                AudioQualityRow(AudioQuality.MEDIUM, "Medium", "About 128 kbps", audioQuality == AudioQuality.MEDIUM) { viewModel.setAudioQuality(AudioQuality.MEDIUM) }
                AudioQualityRow(AudioQuality.LOW, "Low", "Data saver", audioQuality == AudioQuality.LOW) { viewModel.setAudioQuality(AudioQuality.LOW) }
            }
        }

        // ── Theme ──────────────────────────────────────────────────────────────
        item { SectionHeader("Theme") }
        items(VerzaTheme.entries.filter { it != VerzaTheme.DYNAMIC || DynamicColorSupported }) { theme ->
            ThemeRow(theme = theme, selected = theme == currentTheme) { viewModel.setTheme(theme) }
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
                        style = MaterialTheme.typography.bodySmall,
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
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val ext = LocalVerzaExtendedColors.current
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = ext.muted,
        modifier = Modifier.padding(start = 24.dp),
    )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) colors.primaryContainer.copy(alpha = 0.5f) else colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                Text(description, style = MaterialTheme.typography.bodySmall, color = ext.muted)
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = colors.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ThemeRow(theme: VerzaTheme, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val scheme = remember(theme) { theme.toColorScheme() }

    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) colors.primaryContainer.copy(alpha = 0.5f) else colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Swatch trio for the theme.
            Row(modifier = Modifier.clip(RoundedCornerShape(8.dp))) {
                Box(Modifier.size(24.dp).background(scheme.background))
                Box(Modifier.size(24.dp).background(scheme.primary))
                Box(Modifier.size(24.dp).background(scheme.secondary))
                Box(Modifier.size(24.dp).background(scheme.tertiary))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(theme.displayName, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        theme == VerzaTheme.DYNAMIC -> "Follows system"
                        theme.isLight -> "Light"
                        else -> "Dark"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                )
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = colors.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
