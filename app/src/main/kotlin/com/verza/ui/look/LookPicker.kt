package com.verza.ui.look

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import kotlinx.coroutines.launch

/**
 * Every look as a card, each drawn in the look itself, so the choice is made by seeing it. A tap
 * wears it at once; the page you're on redraws in it.
 */
@Composable
fun LookPicker() {
    val graph = LocalContext.current.graph
    val chosen by graph.settings.look.flow.collectAsState(initial = GlassLook.id)
    val scope = rememberCoroutineScope()
    val dark = isSystemInDarkTheme()
    val page = LocalPalette.current
    val list = rememberLazyListState()
    // Open on the one being worn.
    LaunchedEffect(Unit) { list.scrollToItem(Looks.all.indexOfFirst { it.id == chosen }.coerceAtLeast(0)) }
    LazyRow(state = list, contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(Looks.all, key = { it.id }) { look ->
            val on = look.id == chosen
            val p = remember(look, dark) { look.palette(emptyList(), dark) }
            val card = RoundedCornerShape(16.dp)
            Column(
                Modifier.width(140.dp).clip(card).clickable { scope.launch { graph.settings.look.set(look.id) } }
                    .semantics(mergeDescendants = true) { role = Role.RadioButton; selected = on; contentDescription = "Look: ${look.name}" },
            ) {
                Box(Modifier.size(140.dp, 176.dp).clip(card).border(if (on) 3.dp else 1.dp, if (on) page.accent else page.line, card)) {
                    CompositionLocalProvider(LocalLook provides look, LocalPalette provides p) { look.Preview(Modifier.fillMaxSize()) }
                }
                Text(look.name, style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = page.ink, maxLines = 1, modifier = Modifier.padding(top = 8.dp, start = 2.dp))
                Text(look.note, style = Type.Small, color = page.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 2.dp))
            }
        }
    }
}
