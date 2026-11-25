package com.verza.ui.look

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.verza.data.Card
import com.verza.data.Section
import com.verza.playback.NowPlaying
import com.verza.ui.theme.GroupCorner
import com.verza.ui.theme.Palette
import com.verza.ui.theme.artCorner

/*
 * A look is a whole way for Verza to look: its colours and type, the ground under the pages, how a
 * page names itself and its sections, its buttons, how a shelf of covers sits, the dock, and how Now
 * Playing shows the song. One is chosen in Settings, and LocalLook carries it everywhere; the pages
 * themselves are the same in every look, drawn through it.
 *
 * The defaults are Liquid glass. Each other look is after a trend in "Top 2026 Web Design Trends"
 * (Codex Community), and its file says which sites it learned from and what it took.
 */

/** The text styles a look sets. [label] is its small voice: a caption, a tag, often mono. */
@Immutable
data class Types(
    val masthead: TextStyle,
    val section: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val rowTitle: TextStyle,
    val rowSubtitle: TextStyle,
    val body: TextStyle,
    val small: TextStyle,
    val tab: TextStyle,
    val label: TextStyle,
)

/** How a look sets the words that name things: pages, sections, buttons, tabs. */
enum class Case {
    Sentence, Upper, Lower;

    fun of(s: String): String = when (this) {
        Sentence -> s.replaceFirstChar { it.titlecase() }
        Upper -> s.uppercase()
        Lower -> s.lowercase()
    }
}

/** A place in the dock. [search] is the one that searches, which some looks set apart. */
@Immutable
data class Tab(val route: String, val label: String, val icon: ImageVector, val search: Boolean = false)

/** What a dock needs: its tabs, which is current, how to go to one, the playing song, how to open it. */
class DockState(
    val tabs: List<Tab>,
    val current: String?,
    val go: (String) -> Unit,
    val now: NowPlaying,
    val openPlayer: () -> Unit,
    /** "Downloading 3 of 12 · Yellow", while downloads run. */
    val downloads: String?,
)

abstract class Look(val id: String, val name: String, val note: String) {
    /** The colours, from the playing cover's [swatches] and the system's [dark] mode. */
    abstract fun palette(swatches: List<Color>, dark: Boolean): Palette

    abstract val type: Types

    /** How page names, section titles, buttons and tabs are set. */
    open val case: Case = Case.Sentence

    /** A cover's shape at [size] (0 for one that fills its width). */
    open fun cover(size: Dp): Shape = artCorner(size)

    /** An inset group's shape (Settings). */
    open val group: Shape = GroupCorner

    /** Drawn over every cover in lists and shelves: Minimalism's grey. */
    open val coverFilter: androidx.compose.ui.graphics.ColorFilter? = null

    /** A frame round inset groups, for the looks that draw one. */
    open val panel: androidx.compose.foundation.BorderStroke? = null

    /** How far the dock reaches up from the navigation bar, with a song playing or not. */
    open fun dockHeight(playing: Boolean): Dp = 8.dp + 62.dp + (if (playing) 8.dp + 56.dp else 0.dp) + 12.dp

    /** Lists soften into the status bar and under the dock, as content does under glass. */
    open val edgeFade: Boolean = true

    // The pages.

    /** The ground under every page, the whole screen. */
    @Composable open fun Backdrop(modifier: Modifier) {}

    @Composable open fun Masthead(title: String, modifier: Modifier, dateline: String?, trailing: (@Composable () -> Unit)?) =
        GlassMasthead(title, modifier, dateline, trailing)

    @Composable open fun SectionTitle(text: String, modifier: Modifier) = GlassSectionTitle(text, modifier)

    /** A button: Play, Shuffle, New playlist. [color] is the accent for the one to press. */
    @Composable open fun Button(text: String, color: Color, modifier: Modifier, icon: ImageVector?, enabled: Boolean, onClick: () -> Unit) =
        GlassWordButton(text, color, modifier, icon, enabled, onClick)

    /** A round button floating over a page: back, a page's menu, download. */
    @Composable open fun ChromeButton(icon: ImageVector, label: String, modifier: Modifier, size: Dp, tint: Color, stain: Color?, iconSize: Dp, onClick: () -> Unit) =
        com.verza.ui.kit.GlassButton(icon, label, modifier, size, tint, stain, iconSize, onClick)

    /** The line between list rows, from [start]. */
    @Composable open fun Divider(start: Dp) = GlassHairline(start)

    /** A shelf of covers across the page: songs to play, albums and mixes to open. */
    @Composable open fun Shelf(section: Section, onOpen: (String) -> Unit) = GlassShelf(section, onOpen)

    /** The look's own block at the top of Home, under its name. */
    @Composable open fun HomeHero() {}

    /** The tabs and the playing song, at the foot of the screen over the pages. */
    @Composable open fun Dock(state: DockState, modifier: Modifier) = GlassDock(state, modifier)

    // Now Playing.

    /** Now Playing's colours, made from the page's. */
    open fun nowPlaying(page: Palette): Palette = page.overMedia()

    /** Now Playing's ground, behind everything on it. [art] is a small copy of the cover. */
    @Composable open fun NowPlayingBackground(art: String?) = com.verza.ui.nowplaying.FlowingCover(art, true)

    /**
     * The cover on Now Playing, [side] square. [rest] is 1 playing and less paused; [swell] the bass;
     * [fly] a flick carrying it away (0 to 1), with [label] where it went.
     */
    @Composable open fun NowPlayingCover(art: String?, side: Dp, rest: Float, swell: Float, fly: Float, label: String?) =
        GlassNowPlayingCover(art, side, rest, swell, fly, label)

    @Composable open fun PlayButton(playing: Boolean, onClick: () -> Unit) = GlassPlayButton(playing, onClick)

    /** Draws the scrubber's track: [shown] played of 1, thicker while [dragging]. */
    open fun DrawScope.scrubber(shown: Float, dragging: Float, p: Palette) = glassScrubber(shown, dragging, p)

    /** Anything the look floats over Now Playing (a mascot, a badge, a HUD). */
    @Composable open fun NowPlayingDecor() {}

    /** A small likeness of the look for its card in Settings. */
    @Composable open fun Preview(modifier: Modifier) = DefaultPreview(this, modifier)
}

val LocalLook = staticCompositionLocalOf<Look> { GlassLook }

/** Every look, in the order Settings shows them. */
object Looks {
    val all: List<Look> by lazy {
        listOf<Look>(GlassLook, GridLook, BrutalLook, GenZLook, MinimalLook)
    }

    fun byId(id: String?): Look = all.firstOrNull { it.id == id } ?: GlassLook
}

/** An offset that's [x] and [y] dp, for the look files' drawing. */
internal fun DrawScope.dp(x: Float, y: Float) = Offset(x * density, y * density)

