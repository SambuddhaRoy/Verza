package com.verza.ui.screens

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
import com.verza.ui.expressive.HeroTitle
import com.verza.ui.expressive.LocalExpressiveColors
import com.verza.ui.expressive.MetaLabel
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
import com.verza.innertube.SearchFilter
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.MusicItem
import com.verza.ui.components.TrackActionsMenu
import com.verza.ui.components.rememberSongArtwork
import com.verza.ui.expressive.BodyStrong
import com.verza.ui.expressive.BodyText
import com.verza.ui.expressive.ExpressiveChip
import com.verza.ui.expressive.PillShape
import com.verza.ui.expressive.ShapeLargeIncreased
import com.verza.ui.expressive.ShapeMedium
import com.verza.ui.theme.LocalVerzaExtendedColors

@Composable
fun SearchScreen(
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()

    val xc = LocalExpressiveColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ─────────────────────────────────────────────────────────
        // Same masthead as Home and Library: mono dateline over an italic display serif.
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text("EXPLORE", style = MetaLabel, color = xc.onContainerMuted)
            Spacer(Modifier.height(6.dp))
            Text("Search", style = HeroTitle, color = xc.onContainer)
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
                    label = f.label,
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
            is SearchUiState.Loading -> CenterBox { CircularProgressIndicator(color = xc.accent) }
            is SearchUiState.Empty -> CenterHint("No results")
            is SearchUiState.Error -> CenterHint(state.message)
            is SearchUiState.Results -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
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
    val colors = LocalExpressiveColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(colors.surface)
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = colors.onSurfaceMuted,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
            if (value.isEmpty()) {
                Text("Artists, songs, or albums", style = BodyText, color = colors.onSurfaceMuted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = BodyText.copy(color = colors.onSurface),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            // A 40dp target rather than a bare 20dp glyph. Clearing a query is a thing people do
            // constantly and it was the smallest tap target on the screen.
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(PillShape)
                    .background(colors.surfaceHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onValueChange("") },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Clear",
                    tint = colors.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    // The same chip Home's genre row and Library's tabs use, so all three read as one control.
    ExpressiveChip(label = label, selected = selected, onClick = onClick)
}

@Composable
private fun ResultRow(item: HomeItem, onClick: () -> Unit) {
    val colors = LocalExpressiveColors.current
    // Songs benefit from iTunes album art; other result types keep their YT thumbnail.
    val art = if (item.isSong) rememberSongArtwork(item.title, item.subtitle, item.thumbnailUrl)
              else item.thumbnailUrl

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeLargeIncreased)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                // Artists are round, everything else is a rounded square. It is the fastest way to
                // tell a person from a record in a mixed list.
                .clip(if (item.browseId?.startsWith("UC") == true) PillShape else ShapeMedium)
                .background(colors.surfaceHigh),
        ) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = BodyStrong,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = BodyText,
                    color = colors.onSurfaceMuted,
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
    val ext = LocalVerzaExtendedColors.current
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
    val ext = LocalVerzaExtendedColors.current
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
    val ext = LocalVerzaExtendedColors.current
    CenterBox { Text(text, style = MaterialTheme.typography.bodyMedium, color = ext.muted) }
}
