package com.verza.ui.expressive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The two things the app has to say for itself after a release: what changed, and that there is
 * something newer.
 *
 * Both are sheets rather than dialogs. A dialog interrupts and demands a decision; neither of these
 * is a decision — one is news you can ignore and the other is an offer — so they arrive from the
 * bottom, in the app's own colours, and go away when dismissed.
 */

/** Shown once after an update, listing what changed in the version now installed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogSheet(
    version: String,
    notes: String,
    onDismiss: () -> Unit,
) {
    val colors = LocalExpressiveColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surface,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WHAT'S NEW", style = MetaLabel, color = colors.onSurfaceMuted)
                    Spacer(Modifier.height(4.dp))
                    Text("Verza $version", style = HeroTitle, color = colors.onSurface)
                }
                ExpressiveControl(
                    onClick = onDismiss,
                    icon = Icons.Filled.Close,
                    contentDescription = "Close",
                    container = colors.container,
                    content = colors.onContainer,
                    iconSize = 18.dp,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .clip(ShapeLargeIncreased)
                    .background(colors.surfaceLow)
                    .padding(16.dp),
            ) {
                Text(
                    text = notes.ifBlank { "No notes were published for this version." },
                    style = BodyText,
                    color = colors.onSurface,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

/**
 * A labelled bar. [fraction] null means "working, no idea how long", which is the honest state
 * while the system installer is being handed the file.
 */
@Composable
private fun Progress(label: String, fraction: Float?, colors: ExpressiveColors) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = BodyStrong, color = colors.onSurface, modifier = Modifier.weight(1f))
            if (fraction != null) {
                Text("${(fraction * 100).toInt()}%", style = BodyText, color = colors.onSurfaceMuted)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(PillShape)
                .background(colors.surfaceHigh),
        ) {
            if (fraction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(PillShape)
                        .background(colors.accent),
                )
            } else {
                androidx.compose.material3.LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(PillShape),
                    color = colors.accent,
                    trackColor = colors.surfaceHigh,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Keep Verza open until this finishes",
            style = BodyText,
            color = colors.onSurfaceMuted,
        )
    }
}
/**
 * Shown when a newer release exists. Deliberately not a nag: it offers the update and a way to say
 * not now, and the caller remembers the answer so the same version is not offered again.
 *
 * Saying yes does the whole thing from here. It used to send you to Settings to find the same offer
 * a second time and press download, then find it a third time and press install; three taps in two
 * places to accept something you had already accepted. Now the only thing left for you to do is the
 * one step Android will not let an app skip, which is confirming the install itself.
 *
 * [progress] is null until a download starts, then runs 0 to 1. [installing] covers the moment
 * between the file landing and the system installer appearing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableSheet(
    version: String,
    notes: String,
    progress: Float?,
    installing: Boolean,
    error: String?,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
) {
    val colors = LocalExpressiveColors.current
    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = rememberModalBottomSheetState(),
        containerColor = colors.surface,
        dragHandle = null,
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(22.dp))
            Text("UPDATE AVAILABLE", style = MetaLabel, color = colors.onSurfaceMuted)
            Spacer(Modifier.height(4.dp))
            Text("Verza $version", style = HeroTitle, color = colors.onSurface)

            if (notes.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .clip(ShapeLargeIncreased)
                        .background(colors.surfaceLow)
                        .padding(16.dp),
                ) {
                    Text(
                        text = notes,
                        style = BodyText,
                        color = colors.onSurface,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            if (error != null) {
                Text(error, style = BodyText, color = colors.onSurfaceMuted)
                Spacer(Modifier.height(12.dp))
            }

            when {
                installing -> Progress("Opening the installer", null, colors)
                progress != null -> Progress("Downloading", progress, colors)
                else -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(PillShape)
                            .background(colors.container)
                            .clickable(onClick = onLater),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Not now", style = BodyStrong, color = colors.onContainer)
                    }
                    Row(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(56.dp)
                            .clip(PillShape)
                            .background(colors.accent)
                            .clickable(onClick = onUpdate)
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            tint = colors.onAccent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (error != null) "Try again" else "Update",
                            style = BodyStrong.copy(fontWeight = FontWeight.Bold),
                            color = colors.onAccent,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}
