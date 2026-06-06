package com.verza.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.effect.MatrixTransformation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

/**
 * Renders a still share-card [Bitmap] into a short MP4 with a slow cinematic push-in, using Media3
 * Transformer's (tested) encoder pipeline. The motion is a gentle 1.0→1.08 zoom over the clip — the
 * poster "breathes" rather than sitting flat, which loops cleanly on social feeds.
 *
 * Everything is best-effort: if the device's encoder can't oblige, [shareAsVideo] silently falls
 * back to sharing the still PNG, so the user always gets *something*.
 */
private const val DURATION_MS = 5_000L
private const val FRAME_RATE = 30

/** Captures this recorded layer, renders an MP4, and shares it — falling back to a PNG on failure. */
suspend fun GraphicsLayer.shareAsVideo(
    context: Context,
    baseName: String,
    chooserTitle: String = "Share",
) {
    val bitmap = toImageBitmap().asAndroidBitmap()
    val mp4 = runCatching { renderToMp4(context, bitmap, baseName) }.getOrNull()
    if (mp4 != null && mp4.length() > 0) {
        shareCacheFile(context, mp4, "video/mp4", chooserTitle)
    } else {
        // Fallback: hand over the still image instead of failing outright.
        val png = cacheFile(context, "$baseName.png")
        withContext(Dispatchers.IO) {
            png.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        shareCacheFile(context, png, "image/png", chooserTitle)
    }
}

/** Encodes [bitmap] to an MP4 in the cache and returns the file, or null if the export failed. */
private suspend fun renderToMp4(context: Context, bitmap: Bitmap, baseName: String): File? {
    val even = bitmap.toEvenDimensions() // H.264 requires even width/height
    val srcPng = cacheFile(context, "$baseName-frame.png")
    withContext(Dispatchers.IO) {
        srcPng.outputStream().use { even.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    val out = cacheFile(context, "$baseName.mp4").also { it.delete() }

    // A 1.0 → 1.08 scale across the clip (always ≥ 1 so the frame never shrinks into black bars).
    val durationUs = DURATION_MS * 1000f
    val pushIn = MatrixTransformation { presentationTimeUs ->
        val t = (presentationTimeUs / durationUs).coerceIn(0f, 1f)
        val scale = 1.0f + 0.08f * t
        Matrix().apply { setScale(scale, scale) }
    }

    val imageItem = MediaItem.Builder()
        .setUri(Uri.fromFile(srcPng))
        .setImageDurationMs(DURATION_MS)
        .build()
    val editedItem = EditedMediaItem.Builder(imageItem)
        .setFrameRate(FRAME_RATE)
        .setEffects(Effects(emptyList(), listOf<Effect>(pushIn)))
        .build()

    // Transformer must be built + started on a thread with a Looper; the callbacks come back on it.
    return withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { cont ->
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, result: ExportResult) {
                        if (cont.isActive) cont.resume(out)
                    }

                    override fun onError(
                        composition: Composition,
                        result: ExportResult,
                        exception: ExportException,
                    ) {
                        if (cont.isActive) cont.resume(null)
                    }
                })
                .build()
            try {
                transformer.start(editedItem, out.absolutePath)
                cont.invokeOnCancellation { runCatching { transformer.cancel() } }
            } catch (t: Throwable) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }
}

private fun Bitmap.toEvenDimensions(): Bitmap {
    val w = width - (width % 2)
    val h = height - (height % 2)
    return if (w == width && h == height) this else Bitmap.createBitmap(this, 0, 0, w.coerceAtLeast(2), h.coerceAtLeast(2))
}

private fun cacheFile(context: Context, name: String): File =
    File(File(context.cacheDir, "shared").apply { mkdirs() }, name)

private fun shareCacheFile(context: Context, file: File, mime: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
