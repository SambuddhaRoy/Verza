package com.verza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.data.LinerNotes
import com.verza.data.LinerNotesRepository
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.LocalVerzaExtendedColors
import com.verza.ui.theme.VerzaShape
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface LinerNotesEntryPoint {
    fun linerNotesRepository(): LinerNotesRepository
}

private sealed interface LinerState {
    data object Loading : LinerState
    data object Empty : LinerState
    data class Loaded(val notes: LinerNotes) : LinerState
}

/**
 * "Liner notes" for the current track — album · year · genre eyebrow and a short editorial blurb
 * about the artist, sourced (no-auth) from iTunes + Wikipedia and presented like a record's notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinerNotesSheet(
    title: String,
    artist: String,
    artworkUrl: String?,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val context = LocalContext.current.applicationContext
    val repo = remember {
        EntryPointAccessors.fromApplication(context, LinerNotesEntryPoint::class.java).linerNotesRepository()
    }
    var state by remember(title, artist) { mutableStateOf<LinerState>(LinerState.Loading) }
    LaunchedEffect(title, artist) {
        val notes = repo.fetch(title, artist)
        state = if (notes == null) LinerState.Empty else LinerState.Loaded(notes)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(modifier = Modifier.size(56.dp).clip(VerzaShape).background(colors.surfaceVariant)) {
                    if (artworkUrl != null) {
                        AsyncImage(model = artworkUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Liner notes", style = MaterialTheme.typography.labelSmall, color = colors.primary)
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            when (val s = state) {
                LinerState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = colors.primary) }

                LinerState.Empty -> Text(
                    "No notes found for this track.",
                    style = CaptionItalic,
                    color = ext.muted,
                    modifier = Modifier.padding(vertical = 12.dp),
                )

                is LinerState.Loaded -> {
                    val n = s.notes
                    val eyebrow = listOfNotNull(n.album, n.year, n.genre).joinToString("  ·  ")
                    if (eyebrow.isNotBlank()) {
                        Text(
                            eyebrow.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                        )
                    }
                    if (!n.blurb.isNullOrBlank()) {
                        Text(
                            n.blurb,
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            color = colors.onSurface,
                        )
                    }
                    Text(
                        "Notes via iTunes & Wikipedia",
                        style = CaptionItalic,
                        color = ext.muted,
                    )
                }
            }
        }
    }
}
