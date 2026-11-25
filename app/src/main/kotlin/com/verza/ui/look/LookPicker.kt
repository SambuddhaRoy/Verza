package com.verza.ui.look

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.graph
import com.verza.ui.kit.bouncy
import com.verza.ui.kit.squish
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.launch

/**
 * The looks side by side as cards, each drawn in the look itself with the playing cover's colour, so
 * the choice is made by seeing it. A tap wears it at once and the page redraws in it.
 */
@Composable
fun LookPicker(modifier: Modifier = Modifier) {
    val graph = LocalContext.current.graph
    val chosen by graph.settings.look.flow.collectAsState(initial = GlassLook.id)
    val scope = rememberCoroutineScope()
    val dark = isSystemInDarkTheme()
    val page = LocalPalette.current
    val swatches = com.verza.ui.theme.coverSwatches(graph.player.state.collectAsState().value.song?.art)
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        for (look in Looks.all) {
            val on = look.id == chosen
            val p = remember(look, dark, swatches) { look.palette(swatches, dark) }
            val card = RoundedCornerShape(18.dp)
            val ring by animateDpAsState(if (on) 3.dp else 1.dp, bouncy(), label = "ring")
            val source = remember { MutableInteractionSource() }
            Column(
                Modifier.weight(1f).squish(source).clip(card).clickable(source, null) { scope.launch { graph.settings.look.set(look.id) } }
                    .semantics(mergeDescendants = true) { role = Role.RadioButton; selected = on; contentDescription = "Look: ${look.name}" },
            ) {
                Box(Modifier.fillMaxWidth().height(170.dp).clip(card).border(ring, if (on) page.accent else page.line, card)) {
                    CompositionLocalProvider(LocalLook provides look, LocalPalette provides p) { look.Preview(Modifier.fillMaxSize()) }
                }
                Text(look.name, style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = page.ink, maxLines = 1, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
                Text(look.note, style = Type.Small, color = page.inkMuted, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
            }
        }
    }
}
