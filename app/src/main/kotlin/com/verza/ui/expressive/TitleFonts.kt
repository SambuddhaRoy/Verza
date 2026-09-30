package com.verza.ui.expressive

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.verza.R

/**
 * The display faces the Now Playing title is set in, a different one for every song.
 *
 * All twelve come from Uncut (uncut.wtf), the free-type index, and every one is under the SIL Open
 * Font License, which is what allows them inside an APK. Their licence texts ship alongside them
 * in `assets/font-licenses/`, as the OFL requires. Uncut listed 47 display faces; the ones not here
 * either carry no licence, a no-derivatives or GPL one, or are too thin or too ornamental to hold a
 * song title at poster size. PicNic was on the list and is not here because it has since moved from
 * the OFL to a licence with conditions attached, whatever Uncut still says.
 */
object TitleFonts {

    val faces: List<Int> = listOf(
        R.font.display_basteleur,
        R.font.display_eckmann,
        R.font.display_frick,
        R.font.display_gloock,
        R.font.display_gtl001,
        R.font.display_gulax,
        R.font.display_le_murmure,
        R.font.display_ouroboros,
        R.font.display_outward,
        R.font.display_solide_mirage,
        R.font.display_sunday,
        R.font.display_trickster,
    )

    private val typefaces = HashMap<Int, Typeface?>()
    private val paint = Paint()

    /**
     * Whether [face] can draw every character of [text].
     *
     * Most of these faces are Latin only and some have no punctuation at all. A title with a
     * character the face lacks does not fail, Android quietly draws that one glyph in the system
     * font, and a poster title in two typefaces looks broken. So a face that cannot set the whole
     * title is passed over for one that can.
     */
    fun covers(context: Context, face: Int, text: String): Boolean {
        val tf = synchronized(typefaces) {
            typefaces.getOrPut(face) { runCatching { ResourcesCompat.getFont(context, face) }.getOrNull() }
        } ?: return false
        return synchronized(paint) {
            paint.typeface = tf
            text.all { it.isWhitespace() || paint.hasGlyph(it.toString()) }
        }
    }
}

/**
 * Which face a song's title is set in, or null when none of them can set it.
 *
 * - **Stable per song.** The start is a hash of [key], so the same song always opens in the same
 *   face: recomposing, rotating or coming back to it does not reshuffle the type.
 * - **Different from the song before.** "A new font for every song" is the promise, and with twelve
 *   faces a plain hash repeats one song in twelve. [avoid] is the previous song's face and is
 *   stepped past.
 * - **Complete.** A face that is missing a character of the title is skipped (see
 *   [TitleFonts.covers]). A title no face covers, such as one in Devanagari, gets null, and the
 *   caller falls back to the app's own type rather than a half-drawn one.
 *
 * Pure, with coverage passed in, so the rules above can be checked on the JVM.
 */
fun pickTitleFace(
    key: String,
    count: Int,
    avoid: Int?,
    covers: (index: Int) -> Boolean,
): Int? {
    if (count <= 0) return null
    val start = Math.floorMod(key.hashCode(), count)
    val order = (0 until count).map { (start + it) % count }
    return order.firstOrNull { it != avoid && covers(it) }
        ?: avoid?.takeIf { covers(it) } // the only face that fits is the last song's; better than none
}
