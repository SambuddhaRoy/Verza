package com.lstn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lstn.innertube.models.ArtistDetail
import com.lstn.innertube.models.HomeItem
import com.lstn.ui.components.SectionRow
import com.lstn.ui.theme.LocalLstnExtendedColors

@Composable
fun ArtistScreen(
    onBack: () -> Unit,
    onItemClick: (HomeItem) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalLstnExtendedColors.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            is ArtistUiState.Loading -> CircularProgressIndicator(
                color = colors.primary,
                modifier = Modifier.align(Alignment.Center),
            )
            is ArtistUiState.Error -> Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(s.message, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
                OutlinedButton(onClick = viewModel::load, shape = CircleShape) { Text("Retry") }
            }
            is ArtistUiState.Content -> ArtistContent(s.detail, onItemClick)
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.surface),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface)
        }
    }
}

@Composable
private fun ArtistContent(detail: ArtistDetail, onItemClick: (HomeItem) -> Unit) {
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                if (detail.thumbnailUrl != null) {
                    AsyncImage(
                        model = detail.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(listOf(colors.primary, colors.tertiary)))
                    )
                }
                // Gradient scrim keeps the artist name legible.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(colors.background.copy(alpha = 0.15f), colors.background)
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                ) {
                    Text(
                        "Artist",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = detail.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        items(detail.sections) { section ->
            SectionRow(title = section.title, items = section.items, onItemClick = onItemClick)
        }
    }
}
