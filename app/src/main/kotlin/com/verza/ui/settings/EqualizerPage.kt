package com.verza.ui.settings

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
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
import com.verza.ui.kit.Slider
import com.verza.ui.kit.Switch
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

    com.verza.ui.kit.Glassed(p.canvas, Modifier.fillMaxSize(), content = {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 24.dp + LocalDockHeight.current)) {
        item { Text("Equaliser", style = Type.Masthead, color = p.ink, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) }
        item {
            Row(Modifier.fillMaxWidth().clickable { scope.launch { s.eqOn.set(!on) } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Equaliser", style = Type.RowTitle, color = p.ink, modifier = Modifier.weight(1f))
                Switch(on, onCheckedChange = null)
            }
        }
        item {
            // The presets as chips, the chosen one in ink, as the search's filters are.
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (preset0 in EqPreset.entries) {
                    val chosen = preset0.name == preset
                    Text(
                        preset0.displayName, style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp),
                        color = if (chosen) p.canvas else p.ink,
                        modifier = Modifier.padding(vertical = 4.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (chosen) p.ink else p.fill)
                            .clickable { scope.launch { s.eqPreset.set(preset0.name); s.eqBands.set(preset0.levelsFor(layout)); s.eqOn.set(true) } }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
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
    }, glass = { com.verza.ui.kit.GlassButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), onClick = onBack) })
}
