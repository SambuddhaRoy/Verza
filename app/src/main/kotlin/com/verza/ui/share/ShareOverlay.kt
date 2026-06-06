package com.verza.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.verza.ui.theme.VerzaShape
import kotlinx.coroutines.launch

/**
 * Generic full-screen preview + share scaffold for any export card. Draws [card] (passed a modifier
 * that records it into an offscreen capture layer) over a tap-to-dismiss scrim, with a Share button
 * that exports the card to a PNG and opens the system share sheet. Rendered inside the host
 * composition (not a Dialog) so the capture layer uses the window's graphics context.
 */
@Composable
fun ShareCardOverlay(
    fileName: String,
    chooserTitle: String,
    onDismiss: () -> Unit,
    aspectRatio: Float = 4f / 5f,
    allowVideo: Boolean = true,
    card: @Composable (Modifier) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    // null = idle; "image" / "video" = an export is in flight (both buttons disabled meanwhile).
    var working by remember { mutableStateOf<String?>(null) }
    val baseName = remember(fileName) { fileName.substringBeforeLast('.') }

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim — tap to dismiss.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        )
        // Content — consumes its own taps so tapping the card doesn't dismiss.
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .pointerInput(Unit) { detectTapGestures { } },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            card(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(VerzaShape)
                    .captureInto(layer),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = working == null) { Text("Close") }
                Button(
                    enabled = working == null,
                    onClick = {
                        if (working == null) {
                            working = "image"
                            scope.launch {
                                runCatching { layer.shareAsPng(context, fileName, chooserTitle) }
                                working = null
                            }
                        }
                    },
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (working == "image") "Preparing…" else "Image")
                }
                if (allowVideo) {
                    Button(
                        enabled = working == null,
                        onClick = {
                            if (working == null) {
                                working = "video"
                                scope.launch {
                                    runCatching { layer.shareAsVideo(context, baseName, chooserTitle) }
                                    working = null
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (working == "video") "Rendering…" else "Video")
                    }
                }
            }
        }
    }
}
