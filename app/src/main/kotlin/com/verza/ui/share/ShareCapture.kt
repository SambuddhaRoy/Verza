package com.verza.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.core.content.FileProvider
import java.io.File

/**
 * Shared "render a Composable to an image and share it" plumbing, used by the lyric cards and the
 * Sleeve Now-Playing poster. The card is drawn into an offscreen [GraphicsLayer] (via
 * [captureInto]) which can then be exported to a PNG and handed to other apps through the app's
 * FileProvider.
 */

/** Records this node into [layer] on every draw so it can later be exported with [shareAsPng]. */
fun Modifier.captureInto(layer: GraphicsLayer): Modifier = this.drawWithContent {
    layer.record { this@drawWithContent.drawContent() }
    drawLayer(layer)
}

/** Snapshots the recorded layer to a PNG in cache and opens the system share sheet for it. */
suspend fun GraphicsLayer.shareAsPng(
    context: Context,
    fileName: String,
    chooserTitle: String = "Share",
) {
    val bitmap = toImageBitmap().asAndroidBitmap()
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, fileName)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
