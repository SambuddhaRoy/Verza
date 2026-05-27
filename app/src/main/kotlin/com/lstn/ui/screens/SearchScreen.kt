package com.lstn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lstn.innertube.SearchFilter
import com.lstn.innertube.models.HomeItem
import com.lstn.innertube.models.MusicItem
import com.lstn.ui.components.TrackActionsMenu
import com.lstn.ui.components.rememberSongArtwork
import com.lstn.ui.theme.LocalLstnExtendedColors

@Composable
fun SearchScreen(
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header (flourish + eyebrow + title) ────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.primary),
            )
            Spacer(Modifier.height(12.dp))
            Text("Explore", style = MaterialTheme.typography.labelSmall, color = colors.primary)
            Spacer(Modifier.height(4.dp))
            Text("Search", style = MaterialTheme.typography.displaySmall, color = colors.onBackground)
        }

        // ── Pill search bar ────────────────────────────────────────────────
        SearchPill(
            value = viewModel.query,
            onValueChange = viewModel::onQueryChange,
            onSearch = { viewModel.search() },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        // ── Filter chips ───────────────────────────────────────────────────
        // Scrollable so the row doesn't squeeze chips and wrap their text mid-word.
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchFilter.entries.forEach { f ->
                FilterPill(
                    label = f.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = viewModel.filter == f,
                    onClick = { viewModel.onFilterChange(f) },
                )
            }
        }

        // ── Body ───────────────────────────────────────────────────────────
        if (viewModel.showSuggestions && suggestions.isNotEmpty()) {
            SuggestionsView(suggestions = suggestions, onPick = { viewModel.applyHistory(it) })
            return@Column
        }

        when (val state = uiState) {
            is SearchUiState.Idle ->
                if (history.isEmpty()) CenterHint("Search YouTube Music")
                else HistoryView(
                    history = history,
                    onPick = { viewModel.applyHistory(it) },
                    onClear = { viewModel.clearHistory() },
                )
            is SearchUiState.Loading -> CenterBox { CircularProgressIndicator(color = colors.primary) }
            is SearchUiState.Empty -> CenterHint("No results")
            is SearchUiState.Error -> CenterHint(state.message)
            is SearchUiState.Results -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(state.items) { item -> ResultRow(item = item, onClick = { onItemClick(item) }) }
            }
        }
    }
}

@Composable
private fun SearchPill(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.surfaceVariant)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = ext.muted, modifier = Modifier.size(20.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    "Artists, songs, or albums",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.muted,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Clear",
                tint = ext.muted,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onValueChange("") },
                    ),
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val bg = if (selected) colors.primary else colors.primaryContainer.copy(alpha = 0.5f)
    val fg = if (selected) colors.onPrimary else colors.primary
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun ResultRow(item: HomeItem, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    // Songs benefit from iTunes album art; other result types keep their YT thumbnail.
    val art = if (item.isSong) rememberSongArtwork(item.title, item.subtitle, item.thumbnailUrl)
              else item.thumbnailUrl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(if (item.browseId?.startsWith("UC") == true) CircleShape else RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant),
        ) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Overflow menu only on actual playable songs (skip on artist/playlist cards).
        if (item.isSong && item.videoId != null) {
            TrackActionsMenu(
                item = MusicItem(
                    id = item.videoId!!,
                    title = item.title,
                    artist = item.subtitle,
                    thumbnailUrl = item.thumbnailUrl,
                )
            )
        }
    }
}

@Composable
private fun SuggestionsView(suggestions: List<String>, onPick: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(suggestions) { s ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = { onPick(s) })
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = ext.muted, modifier = Modifier.size(18.dp))
                Text(
                    s,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HistoryView(history: List<String>, onPick: (String) -> Unit, onClear: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    Column(
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recent", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
            Text(
                "Clear",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onClear)
                    .padding(6.dp),
            )
        }
        history.forEach { q ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onPick(q) },
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Filled.History, contentDescription = null, tint = ext.muted, modifier = Modifier.size(18.dp))
                Text(q, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun CenterBox(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center, content = content)
}

@Composable
private fun CenterHint(text: String) {
    val ext = LocalLstnExtendedColors.current
    CenterBox { Text(text, style = MaterialTheme.typography.bodyMedium, color = ext.muted) }
}
