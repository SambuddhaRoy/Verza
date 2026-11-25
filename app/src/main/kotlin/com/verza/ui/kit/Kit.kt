package com.verza.ui.kit

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.ui.theme.Corner
import com.verza.ui.theme.GroupCorner
import com.verza.ui.theme.artCorner
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type

/**
 * How much of the foot of the screen the dock covers. Pages run on under it, so every list adds this
 * to its bottom padding, or its last row sits behind the dock.
 */
val LocalDockHeight = compositionLocalOf { 0.dp }

/** "recently played" as "Recently played": Apple's sentence case, for words that arrive lowercase. */
fun String.sentence() = replaceFirstChar { it.titlecase() }

/** A page's name, large and bold, with [trailing] (a button or two) level with it. */
@Composable
fun Masthead(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val p = LocalPalette.current
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.sentence(), style = Type.Masthead, color = p.ink, modifier = Modifier.weight(1f).semantics { heading() })
        trailing?.invoke()
    }
}

/** A heading inside a page: "Recently played". */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.sentence(), style = Type.Section, color = LocalPalette.current.ink, modifier = modifier.padding(top = 26.dp, bottom = 10.dp).semantics { heading() })
}

/** A row's own small action, "Forget", "Remove": plain words in [color], 48dp tall. */
@Composable
fun SmallWord(text: String, color: Color, label: String = text, onClick: () -> Unit) {
    Text(
        text.sentence(),
        style = Type.RowTitle,
        color = color,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label }
            .padding(horizontal = 8.dp)
            .wrapContentHeight(),
    )
}

/**
 * A button: its words (and [icon]) in [color] on a capsule of the fill, as Apple Music's Play and
 * Shuffle are. The accent marks the one to press; the rest are in ink.
 */
@Composable
fun Word(text: String, color: Color = LocalPalette.current.ink, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val p = LocalPalette.current
    val ink = if (enabled) color else color.copy(alpha = 0.4f)
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(p.fill)
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text.sentence(), style = Type.Headline, color = ink, maxLines = 1)
    }
}

/** A round button with one symbol on the fill, for the content layer (glass is for the controls over it). */
@Composable
fun IconCircle(icon: ImageVector, label: String, modifier: Modifier = Modifier, size: Dp = 40.dp, tint: Color = LocalPalette.current.ink, onClick: () -> Unit) {
    Box(
        modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick).semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(size).clip(CircleShape).background(LocalPalette.current.fill), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f))
        }
    }
}

/** A cover, square, its corner growing with its size; a hairline on a light page so a white cover isn't a hole. */
@Composable
fun Cover(url: String?, size: Dp, modifier: Modifier = Modifier, round: Boolean = false) {
    val p = LocalPalette.current
    val shape = if (round) CircleShape else artCorner(size)
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(p.surfaceHigh)
            .then(if (p.dark) Modifier else Modifier.border(0.5.dp, p.line, shape)),
    )
}

/** The thin line between rows, starting at [start] so it runs under the words and not the picture. */
@Composable
fun Hairline(start: Dp = 0.dp) {
    Box(Modifier.fillMaxWidth().padding(start = start).height(0.5.dp).background(LocalPalette.current.line))
}

/**
 * A list row: the cover and two lines, a hairline under the words. The playing row's title takes the
 * accent.
 */
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
    leading: (@Composable () -> Unit)? = null,
) {
    val p = LocalPalette.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(Corner)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            leading != null -> leading()
            icon != null -> Box(Modifier.padding(vertical = 6.dp).size(48.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(24.dp))
            }
            else -> Cover(art, 48.dp, Modifier.padding(vertical = 6.dp), round)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).heightIn(min = 60.dp).padding(vertical = 8.dp), verticalArrangement = Arrangement.Center) {
                    // A video's title shown as a song's; the byline's dots the same as everywhere else.
                    Text(com.verza.data.shownTitle(title, subtitle?.substringBefore(" • ")), style = Type.RowTitle.copy(fontSize = 16.sp, lineHeight = 21.sp), color = if (playing) p.accent else p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!subtitle.isNullOrBlank()) {
                        Text(subtitle.replace(" • ", " · "), style = Type.RowSubtitle.copy(fontSize = 14.sp, lineHeight = 19.sp), color = p.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                trailing?.invoke()
            }
            Hairline()
        }
    }
}

/** Nothing here yet: a quiet symbol over one line saying what to do, and the button that does it. */
@Composable
fun Empty(text: String, icon: ImageVector = Icons.Rounded.MusicNote, action: String? = null, onAction: () -> Unit = {}) {
    val p = LocalPalette.current
    Box(Modifier.fillMaxSize().padding(bottom = LocalDockHeight.current), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            Icon(icon, contentDescription = null, tint = p.inkMuted.copy(alpha = 0.55f), modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(14.dp))
            Text(text, style = Type.RowTitle, color = p.inkMuted, textAlign = TextAlign.Center)
            if (action != null) { Spacer(Modifier.height(16.dp)); Word(action, p.accent, onClick = onAction) }
        }
    }
}

/**
 * A text field: the words on a rounded fill, as iOS draws one. Every field in the app is one.
 */
@Composable
fun Field(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = Type.RowTitle,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
) {
    val p = LocalPalette.current
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style.copy(color = p.ink),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(p.accent),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier.heightIn(min = 44.dp).clip(shape).background(p.fill).padding(start = if (leading != null) 10.dp else 14.dp, end = if (trailing != null) 0.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) { leading(); Spacer(Modifier.width(6.dp)) }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = style, color = p.inkMuted, maxLines = 1)
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

/**
 * An inset group of rows, as iOS's settings have: [title] over it, [content] on a rounded panel.
 * Rows inside put a [Hairline] between them.
 */
@Composable
fun Group(title: String? = null, modifier: Modifier = Modifier, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column(modifier.fillMaxWidth().padding(top = 22.dp)) {
        if (title != null) Text(title.sentence(), style = Type.Headline.copy(fontSize = 15.sp, lineHeight = 20.sp), color = p.ink, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp).semantics { heading() })
        Column(Modifier.fillMaxWidth().clip(GroupCorner).background(p.cell).padding(horizontal = 16.dp, vertical = 2.dp), content = content)
        if (footer != null) Text(footer, style = Type.Small, color = p.inkMuted, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
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

/** A switch as iOS draws one: a white knob the same size on or off, the track the accent when on. */
@Composable
fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, enabled: Boolean = true) {
    val p = LocalPalette.current
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        // Content in the knob keeps it full size when off; Material shrinks an empty one.
        thumbContent = { Box(Modifier.size(1.dp)) },
        colors = androidx.compose.material3.SwitchDefaults.colors(
            checkedThumbColor = Color.White, checkedTrackColor = p.accent, checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFF787880).copy(alpha = if (p.dark) 0.36f else 0.18f), uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** A slider as iOS draws one: a thin track, the accent as far as the value, a round white knob. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun Slider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier = Modifier, valueRange: ClosedFloatingPointRange<Float> = 0f..1f) {
    val p = LocalPalette.current
    val colors = androidx.compose.material3.SliderDefaults.colors(
        thumbColor = Color.White, activeTrackColor = p.accent, inactiveTrackColor = Color(0xFF787880).copy(alpha = if (p.dark) 0.36f else 0.2f),
    )
    androidx.compose.material3.Slider(
        value = value, onValueChange = onValueChange, modifier = modifier, valueRange = valueRange, colors = colors,
        thumb = { Box(Modifier.size(26.dp).shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f)).background(Color.White, CircleShape)) },
        track = { state ->
            androidx.compose.material3.SliderDefaults.Track(sliderState = state, colors = colors, drawStopIndicator = null, thumbTrackGapSize = 0.dp, modifier = Modifier.height(4.dp))
        },
    )
}
