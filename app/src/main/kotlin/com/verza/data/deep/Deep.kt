package com.verza.data.deep

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * 3D covers (an experiment): a cover with layers, someone standing in front of a scene, moves as the
 * phone tilts, the scene sliding behind them while they hold still, the way iOS 26 turns a photo on
 * the lock screen into a spatial scene. The cover's colours flow out from it to the screen's edges,
 * going out of focus as they go, so it isn't a picture in a frame any more.
 *
 * The depth comes from Depth Anything V2 Small (Apache-2.0), run on the phone by LiteRT: a 28 MB model
 * downloaded the first time it's turned on, one pinned file checked against its SHA-256. A cover's depth
 * is worked out once and kept (well under a second here, a second or two on a phone); the rest is
 * arithmetic (Layers.kt) and a shader (ui/nowplaying/DeepCover.kt). Covers without layers (graphics,
 * type, a flat picture) stay as they are: [judge] says which.
 */
class Deep(private val context: Context, private val http: OkHttpClient) {
    sealed interface Model {
        data object Missing : Model
        data class Fetching(val share: Float) : Model
        data object Ready : Model
        data object Failed : Model
    }

    private val dir = File(context.filesDir, "deep").apply { mkdirs() }
    private val file = File(dir, FILE)
    /** Covers' depths, 147 KB each, the 150 last used kept (the system may clear them too). */
    private val kept = File(context.cacheDir, "deep")
    private val _model = MutableStateFlow(if (file.length() == SIZE) Model.Ready else Model.Missing)
    val model: StateFlow<Model> = _model

    /** Downloads the model, checked against its digest; a bad or partial download is thrown away. */
    suspend fun fetch() = withContext(Dispatchers.IO) {
        if (_model.value is Model.Fetching || _model.value == Model.Ready) return@withContext
        _model.value = Model.Fetching(0f)
        val part = File(dir, "$FILE.part")
        val ok = runCatching {
            http.newCall(Request.Builder().url(URL).build()).execute().use { res ->
                check(res.isSuccessful) { "HTTP ${res.code}" }
                val digest = MessageDigest.getInstance("SHA-256")
                var got = 0L
                part.outputStream().use { out ->
                    res.body!!.byteStream().use { input ->
                        val buf = ByteArray(1 shl 16)
                        while (true) {
                            val n = input.read(buf); if (n < 0) break
                            out.write(buf, 0, n); digest.update(buf, 0, n); got += n
                            check(got <= SIZE) { "longer than the model" }
                            val share = (got * 100 / SIZE) / 100f
                            if ((_model.value as? Model.Fetching)?.share != share) _model.value = Model.Fetching(share)
                        }
                    }
                }
                check(digest.digest().joinToString("") { "%02x".format(it) } == SHA256) { "not the model" }
                check(part.renameTo(file))
            }
        }.isSuccess
        if (!ok) part.delete()
        _model.value = if (ok) Model.Ready else Model.Failed
    }

    /** Turned off: the model and every kept depth go, so trying it costs nothing afterwards. */
    fun forget() {
        if (_model.value is Model.Fetching) return
        file.delete(); kept.deleteRecursively(); last = null
        _model.value = Model.Missing
    }

    private val running = Mutex()
    private var last: Pair<String, Scene?>? = null

    /**
     * The 3D scene for the cover at [art], or null: no model yet, a cover without layers, or one that
     * wouldn't load. Worked out on a background thread; a cover's depth is kept on disk.
     */
    suspend fun scene(art: String): Scene? = withContext(Dispatchers.Default) {
        last?.let { (key, scene) -> if (key == art) return@withContext scene }
        if (_model.value != Model.Ready) return@withContext null
        val cover = runCatching {
            SingletonImageLoader.get(context).execute(ImageRequest.Builder(context).data(art).size(1024).allowHardware(false).build()).image?.toBitmap()
        }.getOrNull()?.let(::square) ?: return@withContext null
        val t0 = System.nanoTime()
        val depth = runCatching { depthOf(art, cover) }.onFailure { android.util.Log.w("Verza", "deep: depth failed", it) }.getOrNull() ?: return@withContext null
        val t1 = System.nanoTime()
        val scene = build(cover, depth)
        android.util.Log.i("Verza", "deep: depth ${(t1 - t0) / 1_000_000} ms, scene ${(System.nanoTime() - t1) / 1_000_000} ms, ${scene?.verdict ?: "no layers"}")
        last = art to scene
        scene
    }

    /** The cover's depth at S x S, 0..1, near large: from disk, or from the model and then kept. */
    private suspend fun depthOf(art: String, cover: Bitmap): FloatArray {
        val one = File(kept.apply { mkdirs() }, MessageDigest.getInstance("SHA-1").digest(art.toByteArray()).joinToString("") { "%02x".format(it) } + ".depth")
        if (one.length() == (S * S).toLong()) {
            one.setLastModified(System.currentTimeMillis())
            return one.readBytes().let { b -> FloatArray(S * S) { (b[it].toInt() and 255) / 255f } }
        }
        val d = running.withLock { estimate(cover) }
        one.writeBytes(ByteArray(S * S) { (d[it] * 255f + 0.5f).toInt().toByte() })
        kept.listFiles()?.sortedByDescending { it.lastModified() }?.drop(150)?.forEach { it.delete() }
        return d
    }

    /** The model on the cover: stretched to its fixed 686 x 518 (as it was exported), back to S x S. */
    private fun estimate(cover: Bitmap): FloatArray {
        val w = 686; val h = 518
        val px = IntArray(w * h)
        Bitmap.createScaledBitmap(cover, w, h, true).getPixels(px, 0, w, 0, 0, w, h)
        val input = ByteBuffer.allocateDirect(4 * 3 * w * h).order(ByteOrder.nativeOrder())
        val f = input.asFloatBuffer()
        for (c in 0..2) for (p in px) f.put((((p shr (16 - 8 * c)) and 255) / 255f - MEAN[c]) / STD[c])
        val output = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.nativeOrder())
        Interpreter(file, Interpreter.Options().setNumThreads(4)).use { it.run(input, output) }
        output.rewind()
        val raw = FloatArray(w * h).also { output.asFloatBuffer().get(it) }
        return normalise(resize(raw, w, h, S, S))
    }

    private fun build(cover: Bitmap, d: FloatArray): Scene? {
        val verdict = judge(d, S, S)
        if (!verdict.layered) { android.util.Log.i("Verza", "deep: flat, $verdict"); return null }
        val grown = growNear(d, S, S, 2)
        val near = FloatArray(S * S) { ((grown[it] - (verdict.split - 0.04f)) / 0.08f).coerceIn(0f, 1f) }
        val farOnly = BooleanArray(S * S) { near[it] < 0.02f }
        val far = pushPull(d, S, S, farOnly)
        // The plate: the cover with its near things taken out, their surroundings run together into the hole.
        val small = IntArray(S * S).also { Bitmap.createScaledBitmap(cover, S, S, true).getPixels(it, 0, S, 0, 0, S, S) }
        val plate = channels(small).map { pushPull(it, S, S, farOnly) }
        val layers = Bitmap.createBitmap(IntArray(S * S) { Color.rgb(byte(grown[it]), byte(near[it]), byte(far[it])) }, S, S, Bitmap.Config.ARGB_8888)
        val plateBitmap = Bitmap.createBitmap(IntArray(S * S) { Color.rgb(byte(plate[0][it] / 255f), byte(plate[1][it] / 255f), byte(plate[2][it] / 255f)) }, S, S, Bitmap.Config.ARGB_8888)

        // Around it: the cover in the middle of a canvas a few covers across, its colours flowing out
        // over the rest, then out of focus (a little near the cover, a lot further out).
        val ew = (EXT_W * U).toInt(); val eh = (EXT_H * U).toInt()
        val ox = (-EXT_X * U).toInt(); val oy = (-EXT_Y * U).toInt()
        val tiny = IntArray(U * U).also { Bitmap.createScaledBitmap(cover, U, U, true).getPixels(it, 0, U, 0, 0, U, U) }
        val inCover = BooleanArray(ew * eh) { (it % ew - ox) in 0 until U && (it / ew - oy) in 0 until U }
        val flowing = channels(IntArray(ew * eh) { i -> if (inCover[i]) tiny[(i / ew - oy) * U + (i % ew - ox)] else 0 }).map { pushPull(it, ew, eh, inCover) }
        val ext = bitmapOf(flowing.map { blur(it, ew, eh, 1) }, ew, eh)
        val extFar = bitmapOf(flowing.map { blur(it, ew, eh, 7) }, ew, eh)
        val lum = tiny.map { (0.2126f * Color.red(it) + 0.7152f * Color.green(it) + 0.0722f * Color.blue(it)) / 255f }.average().toFloat()
        return Scene(cover, layers, plateBitmap, ext, extFar, focus = percentile(d, 0.88f), lum = lum, verdict = verdict)
    }

    /** The middle square of [b], as Now Playing crops a video's 16:9 picture. */
    private fun square(b: Bitmap): Bitmap {
        val side = minOf(b.width, b.height)
        return if (b.width == b.height) b else Bitmap.createBitmap(b, (b.width - side) / 2, (b.height - side) / 2, side, side)
    }

    private fun channels(px: IntArray) = listOf(16, 8, 0).map { s -> FloatArray(px.size) { ((px[it] shr s) and 255).toFloat() } }
    private fun bitmapOf(c: List<FloatArray>, w: Int, h: Int) =
        Bitmap.createBitmap(IntArray(w * h) { Color.rgb(byte(c[0][it] / 255f), byte(c[1][it] / 255f), byte(c[2][it] / 255f)) }, w, h, Bitmap.Config.ARGB_8888)
    private fun byte(v: Float) = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt()

    companion object {
        /** The size the depth and the layers are worked at. */
        const val S = 384
        /** Pixels per cover side in the canvas around it, which is only ever seen out of focus. */
        const val U = 48
        /** The canvas around the cover, in cover sides (whole pixels at [U]): -0.625 to 1.625 across, -1.375 to 3.0 down. */
        const val EXT_X = -0.625f; const val EXT_Y = -1.375f; const val EXT_W = 2.25f; const val EXT_H = 4.375f

        private const val FILE = "depth_anything_v2_small_wi8_afp32.tflite"
        // litert-community's LiteRT export, int8 weights, at the revision that was checked (November 2026).
        private const val URL = "https://huggingface.co/litert-community/depth-anything-v2-small/resolve/3bcf9458f0491af76e60346ab3cd2d2cd4bbc566/tflite/$FILE"
        private const val SHA256 = "f74509422e4a9270a354b249a9193abdd4903354be63701262238a7f4b869611"
        const val SIZE = 27_733_680L
        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}

/**
 * A cover made ready to move: [image] itself; [layers] (red: depth with near things grown a little, green:
 * how much is near, blue: the depth of what's behind them); [plate], the cover with its near things
 * painted out; [ext] and [extFar], its colours flowing out around it, a little and a lot out of focus.
 * [focus] is the depth that holds still (the subject's), [lum] how bright the cover is.
 */
class Scene(
    val image: Bitmap, val layers: Bitmap, val plate: Bitmap, val ext: Bitmap, val extFar: Bitmap,
    val focus: Float, val lum: Float, val verdict: Verdict,
)
