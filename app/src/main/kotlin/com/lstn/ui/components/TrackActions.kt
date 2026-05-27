package com.lstn.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lstn.innertube.models.MusicItem
import com.lstn.ui.theme.LocalLstnExtendedColors

/**
 * Per-track actions surfaced from a row's overflow (⋯) menu. Wired once at the navigation root
 * and made available to every row via [LocalTrackActions], so individual row composables don't
 * need to thread half a dozen callbacks each.
 */
data class TrackActions(
    val onPlayNext: (MusicItem) -> Unit,
    val onAddToQueue: (MusicItem) -> Unit,
    val onToggleLike: (MusicItem) -> Unit,
    val onDownload: (MusicItem) -> Unit,
    val onShare: (MusicItem) -> Unit,
    val onGoToArtist: (MusicItem) -> Unit,
    /** Optional — when set, an "Add to playlist" item appears in the menu. */
    val onAddToPlaylist: ((MusicItem) -> Unit)? = null,
)

val LocalTrackActions = compositionLocalOf<TrackActions> {
    error("TrackActions not provided — wrap content in CompositionLocalProvider(LocalTrackActions provides …).")
}

/** A trailing overflow icon that, when tapped, opens the standard per-track menu. */
@Composable
fun TrackActionsMenu(item: MusicItem, modifier: Modifier = Modifier) {
    val actions = LocalTrackActions.current
    val ext = LocalLstnExtendedColors.current
    var open by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = "More",
                tint = ext.muted,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Play next") },
                onClick = { open = false; actions.onPlayNext(item) },
            )
            DropdownMenuItem(
                text = { Text("Add to queue") },
                onClick = { open = false; actions.onAddToQueue(item) },
            )
            actions.onAddToPlaylist?.let { add ->
                DropdownMenuItem(
                    text = { Text("Add to playlist…") },
                    onClick = { open = false; add(item) },
                )
            }
            DropdownMenuItem(
                text = { Text("Like") },
                onClick = { open = false; actions.onToggleLike(item) },
            )
            DropdownMenuItem(
                text = { Text("Download") },
                onClick = { open = false; actions.onDownload(item) },
            )
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = { open = false; actions.onShare(item) },
            )
            DropdownMenuItem(
                text = { Text("Go to artist") },
                onClick = { open = false; actions.onGoToArtist(item) },
            )
        }
    }
}
