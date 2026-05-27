package com.lstn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.lstn.data.LyricLine
import com.lstn.ui.theme.LocalLstnExtendedColors

@Composable
fun LyricsScreen(
    title: String,
    artist: String,
    durationMs: Long,
    positionMs: Long,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(title, artist, durationMs) {
        viewModel.load(title, artist, durationMs)
    }

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
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

        Box(modifier = Modifier.weight(1f)) {
            when (val s = state) {
                is LyricsUiState.Loading,
                LyricsUiState.Idle -> CircularProgressIndicator(
                    color = colors.primary,
                    modifier = Modifier.align(Alignment.Center),
                )
                is LyricsUiState.None -> CenterHint("No lyrics available", ext.muted)
                is LyricsUiState.Error -> CenterHint(s.message, colors.error)
                is LyricsUiState.Plain -> PlainLyrics(s.text)
                is LyricsUiState.Synced -> SyncedLyrics(s.lines, positionMs)
            }
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
private fun SyncedLyrics(lines: List<LyricLine>, positionMs: Long) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
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

        items(lines) { line ->
            val isCurrent = lines.indexOf(line) == currentIndex
            Text(
                text = line.text.ifBlank { "♪" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (isCurrent) colors.onBackground else ext.muted.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
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

