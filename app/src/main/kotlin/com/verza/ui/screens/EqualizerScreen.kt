package com.verza.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verza.ui.components.EditorialSectionHeader
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.LocalVerzaExtendedColors
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val md = state.metadata

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onBackground)
            }
        }
        Text(
            "Equalizer",
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackground,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
        )
        Text(
            "Shape the sound. Effects apply to everything you play. Gapless playback is automatic.",
            style = CaptionItalic,
            color = ext.muted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
        )

        Spacer(Modifier.height(20.dp))

        // ── Master switch ─────────────────────────────────────────────────────────
        ToggleLine(
            title = "Equalizer",
            subtitle = if (state.enabled) "On — band gains below are live" else "Off — bands are flat",
            checked = state.enabled,
            onToggle = viewModel::setEnabled,
        )

        // ── Bands ─────────────────────────────────────────────────────────────────
        EditorialSectionHeader(title = "Bands")
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            state.bandLevelsMb.forEachIndexed { i, levelMb ->
                val freqHz = md.bands.getOrNull(i)?.centerFreqHz ?: 0
                BandSlider(
                    freqLabel = formatFreq(freqHz),
                    levelMb = levelMb,
                    minMb = md.minLevelMb,
                    maxMb = md.maxLevelMb,
                    enabled = state.enabled,
                    onCommit = { viewModel.setBand(i, it) },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Reset to flat",
                style = MaterialTheme.typography.labelLarge,
                color = if (state.enabled) colors.primary else ext.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .then(if (state.enabled) Modifier.clickable(onClick = viewModel::resetBands) else Modifier)
                    .padding(vertical = 12.dp),
            )
        }

        // ── Bass boost ────────────────────────────────────────────────────────────
        EditorialSectionHeader(title = "Bass boost")
        BassSlider(
            strength = state.bassStrength,
            onCommit = viewModel::setBassStrength,
        )

        // ── Loudness leveling ──────────────────────────────────────────────────────
        EditorialSectionHeader(title = "Loudness")
        ToggleLine(
            title = "Volume leveling",
            subtitle = "Lift quiet tracks toward a steadier perceived loudness",
            checked = state.loudnessEnabled,
            onToggle = viewModel::setLoudnessEnabled,
        )
    }
}

@Composable
private fun ToggleLine(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                Text(subtitle, style = CaptionItalic, color = ext.muted)
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colors.onPrimary,
                    checkedTrackColor = colors.primary,
                ),
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = ext.borderGlass)
    }
}

@Composable
private fun BandSlider(
    freqLabel: String,
    levelMb: Int,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    onCommit: (Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    // Local drag state re-seeds whenever the committed value changes (e.g. "reset to flat").
    var live by remember(levelMb) { mutableStateOf(levelMb.toFloat()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            freqLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontMono),
            color = if (enabled) colors.onBackground else ext.muted,
            modifier = Modifier.width(56.dp),
        )
        Slider(
            value = live,
            onValueChange = { live = it },
            onValueChangeFinished = { onCommit(live.roundToInt()) },
            valueRange = minMb.toFloat()..maxMb.toFloat(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatDb(live.roundToInt()),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontMono),
            color = if (enabled) colors.primary else ext.muted,
            textAlign = TextAlign.End,
            modifier = Modifier.width(52.dp),
        )
    }
}

@Composable
private fun BassSlider(strength: Int, onCommit: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    var live by remember(strength) { mutableStateOf(strength.toFloat()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Slider(
            value = live,
            onValueChange = { live = it },
            onValueChangeFinished = { onCommit(live.roundToInt()) },
            valueRange = 0f..1000f,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${(live / 10f).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontMono),
            color = if (live > 0f) colors.primary else ext.muted,
            textAlign = TextAlign.End,
            modifier = Modifier.width(52.dp),
        )
    }
}

/** "60 Hz" / "1.0 kHz" / "14.0 kHz". */
private fun formatFreq(hz: Int): String =
    if (hz >= 1000) "%.1f kHz".format(hz / 1000f) else "$hz Hz"

/** Millibels → "+3 dB" / "0 dB" / "-2 dB". */
private fun formatDb(mb: Int): String {
    val db = (mb / 100f).roundToInt()
    return when {
        db > 0 -> "+$db dB"
        else -> "$db dB"
    }
}
