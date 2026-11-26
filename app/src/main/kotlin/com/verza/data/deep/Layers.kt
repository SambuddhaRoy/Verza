package com.verza.data.deep

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/*
 * The arithmetic of a 3D cover (Deep.kt), on plain arrays so it runs in a test: where a cover's near
 * things end and its far ones begin, whether it has layers worth moving at all, and the fills for
 * what a tilt uncovers. Depth here is the model's: relative, near is large, scaled to 0..1.
 */

/** [v] scaled so its 1st percentile is 0 and its 99th is 1 (a few stray pixels don't set the range). */
fun normalise(v: FloatArray): FloatArray {
    val sorted = v.copyOf().also { it.sort() }
    val lo = sorted[(sorted.size * 0.01f).toInt()]
    val hi = sorted[min(sorted.size - 1, (sorted.size * 0.99f).toInt())]
    val span = max(hi - lo, 1e-6f)
    return FloatArray(v.size) { ((v[it] - lo) / span).coerceIn(0f, 1f) }
}

/** The value that best splits 0..1 values in two (Otsu's threshold, over a 64-bin histogram). */
fun otsu(v: FloatArray, bins: Int = 64): Float {
    val hist = DoubleArray(bins)
    for (x in v) hist[min(bins - 1, (x.coerceIn(0f, 1f) * bins).toInt())]++
    val total = v.size.toDouble()
    var best = -1.0; var split = 0.5f
    for (k in 1 until bins) {
        var w0 = 0.0; var m0 = 0.0; var w1 = 0.0; var m1 = 0.0
        for (i in 0 until k) { w0 += hist[i]; m0 += hist[i] * (i + 0.5) }
        for (i in k until bins) { w1 += hist[i]; m1 += hist[i] * (i + 0.5) }
        if (w0 == 0.0 || w1 == 0.0) continue
        val between = (w0 / total) * (w1 / total) * (m0 / w0 - m1 / w1).let { it * it }
        if (between > best) { best = between; split = k.toFloat() / bins }
    }
    return split
}

/**
 * Whether a cover has layers: things standing clear in front of what's behind them, with an edge
 * between. [edge] is how steep the depth is where near meets far, against how steep it is anywhere
 * (its median). A person or a building in front of a sky is a cliff (Flower Boy 218, Astroworld 217,
 * Rumours 124, IGOR 105, at 384 pixels); a flat graphic is given a gentle floor by the model, or a ball
 * that rounds off into the dark, and has no cliff (Dark Side of the Moon 4, Parachutes 17, Currents 30).
 */
class Verdict(val split: Float, val near: Float, val edge: Float) {
    val layered: Boolean get() = edge >= 50f && near in 0.05f..0.85f
    override fun toString() = "split %.2f, near %.2f, edge %.0f".format(split, near, edge)
}

fun judge(d: FloatArray, w: Int, h: Int): Verdict {
    val split = otsu(d)
    var nearCount = 0
    val grad = FloatArray(d.size)
    val boundary = ArrayList<Float>()
    for (y in 0 until h) for (x in 0 until w) {
        val i = y * w + x
        val here = d[i] > split
        if (here) nearCount++
        // central differences, one-sided at the edges (as numpy's gradient)
        val gx = when (x) { 0 -> d[i + 1] - d[i]; w - 1 -> d[i] - d[i - 1]; else -> (d[i + 1] - d[i - 1]) / 2 }
        val gy = when (y) { 0 -> d[i + w] - d[i]; h - 1 -> d[i] - d[i - w]; else -> (d[i + w] - d[i - w]) / 2 }
        grad[i] = hypot(gx, gy)
        val changes = (x > 0 && (d[i - 1] > split) != here) || (x < w - 1 && (d[i + 1] > split) != here) ||
            (y > 0 && (d[i - w] > split) != here) || (y < h - 1 && (d[i + w] > split) != here)
        if (changes) boundary += grad[i]
    }
    val median = grad.copyOf().also { it.sort() }[grad.size / 2]
    val edge = if (boundary.isEmpty()) 0f else boundary.sorted()[(boundary.size * 0.9f).toInt().coerceAtMost(boundary.size - 1)] / (median + 1e-4f)
    return Verdict(split, nearCount / d.size.toFloat(), edge)
}

/**
 * Near things [r] pixels wider (a max filter). An outline then lies over the background's pixels, so
 * what a tilt uncovers beside a subject is stretched background, never a streak of the subject's edge.
 */
fun growNear(d: FloatArray, w: Int, h: Int, r: Int): FloatArray {
    val across = FloatArray(d.size)
    for (y in 0 until h) for (x in 0 until w) {
        var m = 0f
        for (k in max(0, x - r)..min(w - 1, x + r)) m = max(m, d[y * w + k])
        across[y * w + x] = m
    }
    val out = FloatArray(d.size)
    for (y in 0 until h) for (x in 0 until w) {
        var m = 0f
        for (k in max(0, y - r)..min(h - 1, y + r)) m = max(m, across[k * w + x])
        out[y * w + x] = m
    }
    return out
}

/**
 * The unknown part of [v] filled smoothly from the known part around it: averaged down a pyramid until
 * everything has a value, then brought back up, each level keeping what it knew. What's behind a subject
 * comes out as its surroundings run together, which is all a tilt ever shows of it.
 */
fun pushPull(v: FloatArray, w: Int, h: Int, known: BooleanArray): FloatArray {
    val sums = ArrayList<FloatArray>(); val weights = ArrayList<FloatArray>(); val sizes = ArrayList<IntArray>()
    sums += FloatArray(v.size) { if (known[it]) v[it] else 0f }
    weights += FloatArray(v.size) { if (known[it]) 1f else 0f }
    sizes += intArrayOf(w, h)
    while (sizes.last()[0] > 1 || sizes.last()[1] > 1) {
        val (pw, ph) = sizes.last().let { it[0] to it[1] }
        val cw = ceil(pw / 2.0).toInt(); val ch = ceil(ph / 2.0).toInt()
        val s = FloatArray(cw * ch); val wt = FloatArray(cw * ch)
        val ps = sums.last(); val pwt = weights.last()
        for (y in 0 until ph) for (x in 0 until pw) {
            val c = (y / 2) * cw + x / 2
            s[c] += ps[y * pw + x]; wt[c] += pwt[y * pw + x]
        }
        sums += s; weights += wt; sizes += intArrayOf(cw, ch)
    }
    var filled = sums.last().let { s -> FloatArray(s.size) { if (weights.last()[it] > 0f) s[it] / weights.last()[it] else 0f } }
    for (level in sizes.size - 2 downTo 0) {
        val (fw, fh) = sizes[level].let { it[0] to it[1] }
        val (cw, ch) = sizes[level + 1].let { it[0] to it[1] }
        val s = sums[level]; val wt = weights[level]
        val up = filled
        filled = FloatArray(fw * fh) { i ->
            if (wt[i] > 0f) s[i] / wt[i] else {
                // bilinear from the level above
                val cx = ((i % fw + 0.5f) / 2f - 0.5f).coerceIn(0f, (cw - 1).toFloat())
                val cy = ((i / fw + 0.5f) / 2f - 0.5f).coerceIn(0f, (ch - 1).toFloat())
                val x0 = cx.toInt(); val y0 = cy.toInt(); val x1 = min(cw - 1, x0 + 1); val y1 = min(ch - 1, y0 + 1)
                val fx = cx - x0; val fy = cy - y0
                (up[y0 * cw + x0] * (1 - fx) + up[y0 * cw + x1] * fx) * (1 - fy) + (up[y1 * cw + x0] * (1 - fx) + up[y1 * cw + x1] * fx) * fy
            }
        }
    }
    return FloatArray(v.size) { if (known[it]) v[it] else filled[it] }
}

/** [v] (w x h) resampled to tw x th, bilinear. */
fun resize(v: FloatArray, w: Int, h: Int, tw: Int, th: Int): FloatArray = FloatArray(tw * th) { i ->
    val sx = ((i % tw + 0.5f) * w / tw - 0.5f).coerceIn(0f, (w - 1).toFloat())
    val sy = ((i / tw + 0.5f) * h / th - 0.5f).coerceIn(0f, (h - 1).toFloat())
    val x0 = sx.toInt(); val y0 = sy.toInt(); val x1 = min(w - 1, x0 + 1); val y1 = min(h - 1, y0 + 1)
    val fx = sx - x0; val fy = sy - y0
    (v[y0 * w + x0] * (1 - fx) + v[y0 * w + x1] * fx) * (1 - fy) + (v[y1 * w + x0] * (1 - fx) + v[y1 * w + x1] * fx) * fy
}

/** The value [q] (0..1) of the way up [v]'s sorted values. */
fun percentile(v: FloatArray, q: Float): Float = v.copyOf().also { it.sort() }[min(v.size - 1, (v.size * q).toInt())]

/** A box blur of radius [r], three times over (close to a Gaussian), edges held. */
fun blur(v: FloatArray, w: Int, h: Int, r: Int): FloatArray {
    if (r <= 0) return v.copyOf()
    var a = v.copyOf(); val b = FloatArray(v.size); val n = 2 * r + 1
    repeat(3) {
        for (y in 0 until h) {
            var sum = 0f
            for (k in -r..r) sum += a[y * w + k.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                b[y * w + x] = sum / n
                sum += a[y * w + min(w - 1, x + r + 1)] - a[y * w + max(0, x - r)]
            }
        }
        for (x in 0 until w) {
            var sum = 0f
            for (k in -r..r) sum += b[k.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                a[y * w + x] = sum / n
                sum += b[min(h - 1, y + r + 1) * w + x] - b[max(0, y - r) * w + x]
            }
        }
    }
    return a
}
