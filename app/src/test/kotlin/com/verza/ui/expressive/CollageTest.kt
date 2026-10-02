package com.verza.ui.expressive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The collage's one promise: every cover placed, nothing overlapping, no hole anywhere, and a
 * straight bottom edge. Swept over many random size requests and widths, because a packing bug
 * shows up as a gap on some particular sequence of sizes, not on the obvious one.
 */
class CollageTest {

    private fun check(count: Int, columns: Int, sizes: (Int) -> Int) {
        val cells = packCollage(count, columns, preferredSpan = sizes)
        val rows = cells.maxOf { it.row + it.height }
        val owner = Array(rows) { IntArray(columns) { -2 } }
        cells.forEachIndexed { n, cell ->
            assertTrue("$cell runs off the side", cell.col >= 0 && cell.col + cell.width <= columns)
            for (r in cell.row until cell.row + cell.height) for (c in cell.col until cell.col + cell.width) {
                assertEquals("cell ($r,$c) is covered twice", -2, owner[r][c])
                owner[r][c] = n
            }
        }
        for (r in 0 until rows) for (c in 0 until columns) {
            assertTrue("hole at ($r,$c) with $columns columns", owner[r][c] != -2)
        }
        val tiles = cells.filterNot { it.isFiller }
        assertEquals("not every cover was placed", (0 until count).toList(), tiles.map { it.index }.sorted())
        tiles.forEach {
            assertEquals("a cover must be square", it.width, it.height)
            assertTrue("a cover too small to read", it.width >= 2)
        }
    }

    @Test
    fun `no gaps, no overlaps, every cover placed, for any run of sizes`() {
        val rnd = Random(7)
        repeat(400) {
            val columns = listOf(4, 6, 7, 8, 12).random(rnd)
            val count = rnd.nextInt(1, 70)
            val sizes = IntArray(count) { rnd.nextInt(1, columns + 2) }
            check(count, columns) { sizes[it] }
        }
    }

    @Test
    fun `the sizes really do vary`() {
        val spans = (0 until 200).map { collageSpan("item-$it", seed = 20_000, columns = 6) }.toSet()
        assertTrue("expected several tile sizes, got $spans", spans.size >= 3)
    }

    @Test
    fun `a tile keeps its size for the day`() {
        assertEquals(
            collageSpan("song-abc", seed = 20_000, columns = 6),
            collageSpan("song-abc", seed = 20_000, columns = 6),
        )
    }

    @Test
    fun `art is asked for at the size it is drawn, where the host allows it`() {
        assertEquals(
            "https://lh3.googleusercontent.com/abc=w480-h480-l90-rj",
            sizedArt("https://lh3.googleusercontent.com/abc=w120-h120-l90-rj", 480),
        )
        assertEquals("https://yt3.ggpht.com/abc=s480", sizedArt("https://yt3.ggpht.com/abc=s88", 480))
        // Hosts that do not take a size, and URLs with no size in them, are left alone.
        assertEquals("https://i.ytimg.com/vi/x/hqdefault.jpg", sizedArt("https://i.ytimg.com/vi/x/hqdefault.jpg", 480))
        assertEquals("https://lh3.googleusercontent.com/abc", sizedArt("https://lh3.googleusercontent.com/abc", 480))
        // Never absurdly large, however big the tile.
        assertEquals("https://yt3.ggpht.com/abc=s1200", sizedArt("https://yt3.ggpht.com/abc=s88", 4000))
    }
}
