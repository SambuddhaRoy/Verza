package com.verza.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.player.QueueItem

/**
 * Playback speed, as a row of pills.
 *
 * Pitch is corrected, so a slowed track stays in key — which is the only reason this belongs in a
 * music player rather than a podcast one. Half speed with the pitch dropping a fifth is a novelty;
 * half speed in the right key is how people work a part out.
 */
@Composable
private fun SpeedRow(speed: Float, onSelect: (Float) -> Unit, colors: ExpressiveColors) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text("SPEED", style = MetaLabel, color = colors.onSurfaceMuted)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (option in floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)) {
                val selected = kotlin.math.abs(option - speed) < 0.01f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(PillShape)
                        .background(if (selected) colors.accent else colors.surfaceHigh)
                        .clickable { onSelect(option) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        // "1x" reads better than "1.0x"; the rest need their decimal.
                        text = if (option == 1f) "1x" else "${option}x".replace(".0x", "x"),
                        style = BodyStrong,
                        color = if (selected) colors.onAccent else colors.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressiveMoreSheet(
    isDownloading: Boolean,
    focusActive: Boolean,
    speed: Float,
    onSetSpeed: (Float) -> Unit,
    onDiscoveryRadio: () -> Unit,
    onAmbient: () -> Unit,
    onLinerNotes: () -> Unit,
    onFocus: () -> Unit,
    onShareSession: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalExpressiveColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surface,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 8.dp)) {
            SheetHeader(
                title = "More",
                subtitle = if (isDownloading) "Downloading this track…" else null,
                onDismiss = onDismiss,
            )
            SpeedRow(speed = speed, onSelect = onSetSpeed, colors = colors)
            ExpressiveSheetRow(
                icon = Icons.Filled.Explore,
                label = "Discovery radio",
                onClick = onDiscoveryRadio,
                colors = colors,
                modifier = Modifier.fillMaxWidth(),
            )
            ExpressiveSheetRow(
                icon = Icons.Filled.SelfImprovement,
                label = "Focus session",
                onClick = onFocus,
                colors = colors,
                active = focusActive,
                modifier = Modifier.fillMaxWidth(),
            )
            ExpressiveSheetRow(
                icon = Icons.Filled.Nightlight,
                label = "Ambient display",
                onClick = onAmbient,
                colors = colors,
                modifier = Modifier.fillMaxWidth(),
            )
            ExpressiveSheetRow(
                icon = Icons.Filled.MenuBook,
                label = "Liner notes",
                onClick = onLinerNotes,
                colors = colors,
                modifier = Modifier.fillMaxWidth(),
            )
            ExpressiveSheetRow(
                icon = Icons.Filled.Link,
                label = "Share listening session",
                onClick = onShareSession,
                colors = colors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String?, onDismiss: () -> Unit) {
    val colors = LocalExpressiveColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = colors.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(text = subtitle, color = colors.onContainerMuted, fontSize = 13.sp)
            }
        }
        ExpressiveControl(
            onClick = onDismiss,
            icon = Icons.Filled.Close,
            contentDescription = "Close",
            container = colors.container,
            content = colors.onSurface,
            shape = CircleShape,
            iconSize = 18.dp,
            modifier = Modifier.size(40.dp),
        )
    }
}
