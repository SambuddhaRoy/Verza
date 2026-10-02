package com.verza.ui.expressive

/**
 * Where one block of the Home collage sits, in grid cells. [index] is the tile it shows, or
 * [FILLER] for a slab of flat colour that plugs a hole.
 */
data class CollageCell(val index: Int, val col: Int, val row: Int, val width: Int, val height: Int) {
    val isFiller: Boolean get() = index == FILLER

    companion object {
        const val FILLER = -1
    }
}

/**
 * Packs [count] square tiles into a grid [columns] wide with no gaps and no overlaps.
 *
 * The collage has no uniform size: each tile asks for a side through [preferredSpan], and gets the
 * largest side it can have, up to that, at the first free cell reading left to right, top to
 * bottom. Shrinking to fit is what keeps the packing dense: a tile never leaves a hole by being too
 * big for the space it lands in. A tile smaller than [minSpan] would be too small to read its name,
 * so where not even that fits, a one-cell colour slab goes in instead and the tile tries the next
 * free cell. At the end every hole left above the bottom row is plugged with slabs too, merged
 * along each row, so the collage ends in a straight edge.
 *
 * Pure and deterministic, so the layout is the same on every recomposition and testable on the JVM.
 */
fun packCollage(
    count: Int,
    columns: Int,
    minSpan: Int = 2,
    preferredSpan: (index: Int) -> Int,
): List<CollageCell> {
    require(columns >= minSpan && minSpan >= 1)
    val grid = ArrayList<BooleanArray>()
    fun row(r: Int): BooleanArray {
        while (grid.size <= r) grid.add(BooleanArray(columns))
        return grid[r]
    }
    fun free(r: Int, c: Int) = c < columns && !row(r)[c]
    fun fits(r: Int, c: Int, k: Int) =
        c + k <= columns && (r until r + k).all { rr -> (c until c + k).all { cc -> free(rr, cc) } }
    fun take(r: Int, c: Int, w: Int, h: Int) {
        for (rr in r until r + h) for (cc in c until c + w) row(rr)[cc] = true
    }

    val cells = ArrayList<CollageCell>()
    var r = 0
    var c = 0
    fun advance() {
        while (!free(r, c)) {
            c++
            if (c >= columns) { c = 0; r++ }
        }
    }

    for (i in 0 until count) {
        while (true) {
            advance()
            var k = preferredSpan(i).coerceIn(minSpan, columns)
            while (k >= minSpan && !fits(r, c, k)) k--
            if (k >= minSpan) {
                take(r, c, k, k)
                cells += CollageCell(i, c, r, k, k)
                break
            }
            take(r, c, 1, 1)
            cells += CollageCell(CollageCell.FILLER, c, r, 1, 1)
        }
    }

    // Square off the bottom: every free cell above the lowest tile's foot becomes colour.
    val rows = cells.maxOfOrNull { it.row + it.height } ?: 0
    for (rr in 0 until rows) {
        var cc = 0
        while (cc < columns) {
            if (!free(rr, cc)) { cc++; continue }
            val start = cc
            while (cc < columns && free(rr, cc)) cc++
            take(rr, start, cc - start, 1)
            cells += CollageCell(CollageCell.FILLER, start, rr, cc - start, 1)
        }
    }
    return cells
}

/**
 * A tile's side, in cells, drawn from a stable hash so the same item is the same size every time
 * the collage is built that day. Mostly middling, now and then huge. [boost] is added for things
 * worth more room, such as Verza's own mixes.
 */
fun collageSpan(key: String, seed: Long, columns: Int, boost: Int = 0): Int {
    val h = Math.floorMod((key.hashCode().toLong() * 31 + seed).hashCode() * -0x61c88647, 100)
    val base = when {
        h < 42 -> 2
        h < 72 -> 3
        h < 92 -> 4
        else -> columns // the occasional full-width cover
    }
    return (base + boost).coerceAtMost(columns)
}

/**
 * [url] asked for at [px] on a side, where the host lets the size be chosen.
 *
 * YouTube Music's art (googleusercontent / ggpht) carries its size in the suffix, as `=w120-h120`
 * or `=s120`, and serves any size asked for. Feed thumbnails come at around 120px, which is a blur
 * on a collage tile half the width of the screen, while asking every tile for the largest size
 * would decode far more than is drawn. Anything else is returned untouched.
 */
fun sizedArt(url: String?, px: Int): String? {
    if (url == null) return null
    if (!Regex("""^https://[^/]*(googleusercontent|ggpht)\.com/""").containsMatchIn(url)) return url
    val side = px.coerceIn(60, 1200)
    return when {
        Regex("""=w\d+-h\d+""").containsMatchIn(url) -> url.replace(Regex("""=w\d+-h\d+"""), "=w$side-h$side")
        Regex("""=s\d+""").containsMatchIn(url) -> url.replace(Regex("""=s\d+"""), "=s$side")
        else -> url
    }
}
