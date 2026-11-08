package com.verza.ui.kit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.ui.theme.Corner
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type

/**
 * How much of the foot of the screen the dock covers. Pages run on under it, so every list adds this
 * to its bottom padding, or its last row sits behind the dock.
 */
val LocalDockHeight = compositionLocalOf { 0.dp }

/** A page's name over its dateline: the Zune's masthead. */
@Composable
fun Masthead(title: String, dateline: String? = null, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)) {
        if (dateline != null) Text(dateline, style = Type.Body, color = p.inkMuted)
        Text(title, style = Type.Masthead, color = p.ink)
    }
}

/** A heading inside a page: "recently played". */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Type.Section, color = LocalPalette.current.ink, modifier = modifier.padding(top = 24.dp, bottom = 8.dp))
}

/** A button that is just the word, in large light type: "play", "shuffle". */
@Composable
fun Word(text: String, color: Color = LocalPalette.current.ink, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        style = Type.Section.copy(fontSize = 24.sp, lineHeight = 30.sp),
        color = if (enabled) color else color.copy(alpha = 0.4f),
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(vertical = 6.dp),
    )
}

/** A cover, square, with a hairline on a light page so a white cover isn't a hole in the list. */
@Composable
fun Cover(url: String?, size: Dp, modifier: Modifier = Modifier, round: Boolean = false) {
    val p = LocalPalette.current
    val shape = if (round) CircleShape else Corner
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(p.surfaceHigh)
            .then(if (p.dark) Modifier else Modifier.border(1.dp, p.line, shape)),
    )
}

/** A list row: the cover and two lines, no edges. The playing row's title takes the accent. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    art: String? = null,
    icon: ImageVector? = null,
    round: Boolean = false,
    playing: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Corner)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            icon != null -> Box(Modifier.padding(vertical = 6.dp).size(50.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(26.dp))
            }
            else -> Cover(art, 50.dp, Modifier.padding(vertical = 6.dp), round)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).heightIn(min = 62.dp).padding(vertical = 8.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = Type.RowTitle, color = if (playing) p.accent else p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = Type.RowSubtitle, color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing?.invoke()
    }
}

/** Nothing here yet: a quiet symbol over one line saying what to do. */
@Composable
fun Empty(text: String, icon: ImageVector = Icons.Rounded.MusicNote) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().padding(bottom = LocalDockHeight.current), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            Icon(icon, contentDescription = null, tint = p.inkMuted.copy(alpha = 0.55f), modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(14.dp))
            Text(text, style = Type.RowTitle, color = p.inkMuted, textAlign = TextAlign.Center)
        }
    }
}

/**
 * For anything drawn over the pages (the dock, Now Playing, Lyrics, the welcome): takes the taps that
 * land on it. A background alone lets them through, and a tap on an empty part of the layer pressed
 * whatever was underneath, playing a song nobody chose.
 */
fun Modifier.takesTouches() = pointerInput(Unit) { detectTapGestures { } }

/** Light status-bar icons while this is on screen, for the pages that are dark whatever the system says. */
@Composable
fun LightBarIcons() {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.DisposableEffect(view) {
        val window = (view.context as? android.app.Activity)?.window
        val bars = window?.let { androidx.core.view.WindowCompat.getInsetsController(it, view) }
        val was = bars?.isAppearanceLightStatusBars
        bars?.isAppearanceLightStatusBars = false
        onDispose { if (was != null) bars.isAppearanceLightStatusBars = was }
    }
}
