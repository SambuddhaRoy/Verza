package com.verza.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.graph
import com.verza.playback.EqPreset
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.SectionTitle
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.launch

/** The equaliser: on or off, a preset or your own curve band by band, bass, and loudness. */
@Composable
fun EqualizerPage(onBack: () -> Unit) {
    val graph = LocalContext.current.graph
    val s = graph.settings
    val scope = rememberCoroutineScope()
    val p = LocalPalette.current
    LaunchedEffect(Unit) { graph.effects.ensureMetadata() }
    val layout by graph.effects.metadata.collectAsState()
    val on by s.eqOn.flow.collectAsState(initial = false)
    val stored by s.eqBands.flow.collectAsState(initial = emptyList())
    val preset by s.eqPreset.flow.collectAsState(initial = "")
    val bass by s.bass.flow.collectAsState(initial = 0)
    val loud by s.loudness.flow.collectAsState(initial = false)
    val bands = List(layout.bandCount) { stored.getOrElse(it) { 0 } }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp + LocalDockHeight.current)) {
        item {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = p.ink) }
            Text("equaliser", style = Type.Masthead, color = p.ink)
        }
        item {
            Row(Modifier.fillMaxWidth().clickable { scope.launch { s.eqOn.set(!on) } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Equaliser", style = Type.RowTitle, color = p.ink, modifier = Modifier.weight(1f))
                Switch(on, onCheckedChange = null)
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                for (preset0 in EqPreset.entries) {
                    Text(
                        preset0.displayName.lowercase(), style = Type.Section.copy(fontSize = 22.sp),
                        color = if (preset0.name == preset) p.accent else p.inkMuted,
                        modifier = Modifier.clickable { scope.launch { s.eqPreset.set(preset0.name); s.eqBands.set(preset0.levelsFor(layout)); s.eqOn.set(true) } },
                    )
                }
            }
        }
        item { SectionTitle("bands") }
        for ((i, band) in layout.bands.withIndex()) item(key = "band$i") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val hz = band.centerFreqHz
                Text(if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz", style = Type.RowSubtitle, color = p.inkMuted, modifier = Modifier.width(64.dp))
                Slider(
                    value = bands[i].toFloat(),
                    onValueChange = { v -> scope.launch { s.eqBands.set(bands.toMutableList().also { it[i] = v.toInt() }); s.eqPreset.set(""); s.eqOn.set(true) } },
                    valueRange = layout.minLevelMb.toFloat()..layout.maxLevelMb.toFloat(),
                    modifier = Modifier.weight(1f),
                )
                Text("${bands[i] / 100} dB", style = Type.RowSubtitle, color = p.inkMuted, modifier = Modifier.width(52.dp))
            }
        }
        item { SectionTitle("bass") }
        item { Slider(bass.toFloat(), { v -> scope.launch { s.bass.set(v.toInt()) } }, valueRange = 0f..1000f) }
        item {
            Row(Modifier.fillMaxWidth().clickable { scope.launch { s.loudness.set(!loud) } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Loudness", style = Type.RowTitle, color = p.ink)
                    Text("Lifts quiet songs toward a steadier level", style = Type.RowSubtitle, color = p.inkMuted)
                }
                Switch(loud, onCheckedChange = null)
            }
        }
    }
}
