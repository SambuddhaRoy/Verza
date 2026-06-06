package com.verza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verza.data.LyricLine
import com.verza.ui.share.LyricShareOverlay
import com.verza.ui.theme.LocalVerzaExtendedColors

private data class ShareLine(val line: String, val prev: String?, val next: String?)

@Composable
fun LyricsScreen(
    title: String,
    artist: String,
    durationMs: Long,
    positionMs: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    artworkUrl: String? = null,
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var shareLine by remember { mutableStateOf<ShareLine?>(null) }

    LaunchedEffect(title, artist, durationMs) {
        viewModel.load(title, artist, durationMs)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
            // ── Header ─────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Close", tint = colors.onBackground)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Lyrics", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val s = state) {
                    is LyricsUiState.Loading,
                    LyricsUiState.Idle -> CircularProgressIndicator(
                        color = colors.primary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                    is LyricsUiState.None -> CenterHint("No lyrics available", ext.muted)
                    is LyricsUiState.Error -> CenterHint(s.message, colors.error)
                    is LyricsUiState.Plain -> PlainLyrics(s.text)
                    is LyricsUiState.Synced -> SyncedLyrics(
                        lines = s.lines,
                        positionMs = positionMs,
                        onShareLine = { line, prev, next -> shareLine = ShareLine(line, prev, next) },
                    )
                }
            }
        }

        // Tap a synced line → preview & share it as an editorial card.
        shareLine?.let { sel ->
            LyricShareOverlay(
                line = sel.line,
                prevLine = sel.prev,
                nextLine = sel.next,
                title = title,
                artist = artist,
                artworkUrl = artworkUrl,
                onDismiss = { shareLine = null },
            )
        }
    }
}

@Composable
private fun PlainLyrics(text: String) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun SyncedLyrics(
    lines: List<LyricLine>,
    positionMs: Long,
    onShareLine: (line: String, prev: String?, next: String?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val listState = rememberLazyListState()

    // Find the last line whose timestamp has passed.
    val currentIndex by remember(lines, positionMs) {
        derivedStateOf {
            lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
        }
    }

    // Centre the current line as it changes.
    LaunchedEffect(currentIndex, lines.size) {
        if (lines.isNotEmpty()) {
            // The list has top padding equal to half the viewport via a Spacer item; scrolling
            // to (currentIndex) puts it just under that spacer, i.e. near vertical centre.
            listState.animateScrollToItem(currentIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Top spacer pushes the first line down to viewport centre.
        item { Spacer(Modifier.height(220.dp)) }

        itemsIndexed(lines) { index, line ->
            val isCurrent = index == currentIndex
            val shareable = line.text.isNotBlank()
            Text(
                text = line.text.ifBlank { "♪" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (isCurrent) colors.onBackground else ext.muted.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        // Tap any line to turn it into a shareable card.
                        if (shareable) Modifier.clickable {
                            onShareLine(line.text, lines.getOrNull(index - 1)?.text, lines.getOrNull(index + 1)?.text)
                        } else Modifier,
                    ),
            )
        }

        // Bottom spacer so the last line can centre too.
        item { Spacer(Modifier.height(320.dp)) }
    }
}

@Composable
private fun BoxScope.CenterHint(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = color,
        modifier = Modifier.align(Alignment.Center),
    )
}

