package com.verza.ui.library

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.data.Song
import com.verza.data.artAt
import com.verza.data.phone.Facts
import com.verza.data.youtube.Filter
import com.verza.graph
import com.verza.ui.kit.ListRow
import com.verza.ui.kit.LocalDockHeight
import com.verza.ui.kit.SongMenu
import com.verza.ui.kit.Word
import com.verza.ui.theme.LocalPalette
import com.verza.ui.theme.Type
import java.text.Collator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


/**
 * Downloaded and On device as the desktop shows them (renderer.js, renderCollection): songs, albums
 * or artists, each in an order you pick, with the letter (or month, or year) a run starts at over
 * it. "The" is ignored, as a record shop files it. The view and order are kept per section.
 */
@Composable
fun Collection(key: String, songs: List<Song>, facts: (Song) -> Facts, onOpenGroup: (title: String, kind: String, songs: List<Song>) -> Unit, folders: List<Pair<String, () -> Unit>> = emptyList()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("collections", Context.MODE_PRIVATE) }
    var view by rememberSaveable { mutableStateOf(prefs.getString("$key.view", "albums")!!) }
    var sort by rememberSaveable { mutableStateOf(prefs.getString("$key.sort", null) ?: SORTS.getValue(view).first()) }
    if (sort !in SORTS.getValue(view)) sort = SORTS.getValue(view).first()
    fun save() = prefs.edit().putString("$key.view", view).putString("$key.sort", sort).apply()
    val p = LocalPalette.current
    val player = context.graph.player
    val now by player.state.collectAsState()

    val rows: List<Line> = remember(songs, view, sort) { arrange(songs, facts, view, sort) }
    val order = rows.flatMap { (it as? Line.Item)?.songs.orEmpty() }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp + LocalDockHeight.current)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Word("play", p.accent) { player.play(order) }
                Word("shuffle") { player.shuffle(order) }
                Spacer(Modifier.weight(1f))
                Text(if (songs.size == 1) "1 song" else "${songs.size} songs", style = Type.Small, color = p.inkMuted)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                for (v in SORTS.keys) Text(v, style = Type.Section.copy(fontSize = 19.sp, lineHeight = 24.sp), color = if (v == view) p.ink else p.inkMuted,
                    modifier = Modifier.clickable { view = v; sort = SORTS.getValue(v).first(); save() }.semantics { role = Role.Tab }.padding(end = 16.dp, top = 12.dp, bottom = 12.dp))
                Spacer(Modifier.weight(1f))
                Box {
                    var open by remember { mutableStateOf(false) }
                    Text("by $sort ▾", style = Type.Section.copy(fontSize = 19.sp, lineHeight = 24.sp), color = p.inkMuted,
                        modifier = Modifier.clickable { open = true }.semantics { role = Role.DropdownList }.padding(vertical = 12.dp))
                    DropdownMenu(open, onDismissRequest = { open = false }) {
                        for (s in SORTS.getValue(view)) DropdownMenuItem(text = { Text(s, color = if (s == sort) p.accent else p.ink) }, onClick = { sort = s; save(); open = false })
                    }
                }
            }
        }
        items(rows, key = { it.key }) { row ->
            when (row) {
                is Line.Head -> Text(row.label, style = Type.Section, color = p.accent, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
                is Line.Item -> if (view == "songs") {
                    val song = row.songs.single()
                    ListRow(song.title, listOfNotNull(song.artist, facts(song).album).joinToString(" • "), art = song.art?.let { artAt(it, 226) },
                        playing = song.id == now.song?.id, onClick = { player.play(order, order.indexOf(song)) }, trailing = { SongMenu(song) })
                } else {
                    val artist = view == "artists"
                    ListRow(row.title, row.subtitle, art = if (artist) null else row.art?.let { artAt(it, 226) }, round = artist,
                        icon = null, onClick = { onOpenGroup(row.title, if (artist) "artist" else "album", row.songs) },
                        leading = if (artist) ({ ArtistPicture(row.title) }) else null)
                }
            }
        }
        // Folders of songs that aren't an album, on the phone: after the albums.
        if (view == "albums" && folders.isNotEmpty()) {
            item { Text("folders", style = Type.Section, color = p.accent, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)) }
            items(folders, key = { "f" + it.first }) { (title, open) -> ListRow(title, null, icon = androidx.compose.material.icons.Icons.Rounded.Folder, onClick = open) }
        }
    }
}

/** An artist's picture from YouTube Music for an exact name, or their initial until there is one. */
@Composable
fun ArtistPicture(name: String) {
    val context = LocalContext.current
    val p = LocalPalette.current
    val url by produceState(ArtistArt.cached(context, name), name) { if (value == null) value = ArtistArt.find(context, name) }
    Box(Modifier.padding(vertical = 6.dp).size(50.dp).clip(CircleShape).background(p.surfaceHigh), contentAlignment = Alignment.Center) {
        if (url.isNullOrEmpty()) Text(sortName(name).take(1).lowercase().ifEmpty { "?" }, style = Type.Section, color = p.inkMuted)
        else coil3.compose.AsyncImage(artAt(url!!, 226), null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(50.dp))
    }
}

/** Artist pictures, kept for good; a name with none is kept as "" so it isn't asked again. */
object ArtistArt {
    private fun prefs(c: Context) = c.getSharedPreferences("artist-art", Context.MODE_PRIVATE)
    fun cached(c: Context, name: String): String? = prefs(c).getString(name.lowercase(), null)
    suspend fun find(c: Context, name: String): String {
        if (name.isBlank() || name == "Unknown artist") return ""
        val found = runCatching { c.graph.youtube.search(name, Filter.ARTISTS) }.getOrNull() ?: return ""   // offline: ask again later
        val url = found.firstOrNull { it.title.equals(name, ignoreCase = true) }?.art.orEmpty()
        prefs(c).edit().putString(name.lowercase(), url).apply()
        return url
    }
}

private sealed class Line(val key: String) {
    class Head(val label: String) : Line("h$label")
    class Item(key: String, val title: String, val subtitle: String, val art: String?, val songs: List<Song>) : Line(key)
}

private val SORTS = linkedMapOf(
    "songs" to listOf("a–z", "artist", "album", "date added", "year"),
    "albums" to listOf("artist", "a–z", "date added", "year"),
    "artists" to listOf("a–z", "most songs"),
)

private val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
internal fun sortName(s: String) = s.trim().replace(Regex("^the\\s+", RegexOption.IGNORE_CASE), "")
private fun letter(s: String): String = sortName(s).firstOrNull()?.takeIf { it.isLetter() }?.lowercase() ?: "#"
private val byName = Comparator<String> { a, b -> collator.compare(sortName(a), sortName(b)) }
private fun mainArtist(s: String) = s.split(Regex(",|\\s&\\s|\\s(?:feat|ft)\\.?\\s", RegexOption.IGNORE_CASE)).first().trim().ifEmpty { "unknown artist" }
private val month = DateTimeFormatter.ofPattern("MMMM yyyy")
private fun monthOf(sec: Long) = if (sec <= 0) "earlier" else Instant.ofEpochSecond(sec).atZone(ZoneId.systemDefault()).format(month).lowercase()

private class Group(val title: String, val artist: String, val songs: MutableList<Song> = ArrayList(), var year: Int = 0, var added: Long = 0)

private fun arrange(songs: List<Song>, facts: (Song) -> Facts, view: String, sort: String): List<Line> {
    val f = songs.associateWith(facts)
    fun filed(s: Song) = mainArtist(f.getValue(s).albumArtist ?: s.artist)
    fun album(s: Song) = f.getValue(s).album ?: s.album
    val byTrack = compareBy<Song> { f.getValue(it).track.takeIf { n -> n > 0 } ?: 9999 }.thenBy(byName) { it.title }
    val out = ArrayList<Line>()
    fun headed(items: List<Pair<String?, Line.Item>>) { var last: String? = "\u0001"; for ((g, r) in items) { if (g != null && g != last) { out += Line.Head(g); last = g }; out += r } }
    when (view) {
        "songs" -> {
            val cmp: Comparator<Song> = when (sort) {
                "artist" -> compareBy<Song, String>(byName) { filed(it) }.thenBy(byName) { album(it).orEmpty() }.then(byTrack)
                "album" -> compareBy<Song, String>(byName) { album(it) ?: "￿" }.then(byTrack)
                "date added" -> compareByDescending { f.getValue(it).addedSec }
                "year" -> compareByDescending<Song> { f.getValue(it).year ?: 0 }.thenBy(byName) { filed(it) }.then(byTrack)
                else -> compareBy(byName) { it.title }
            }
            headed(songs.sortedWith(cmp).map { s ->
                val g = when (sort) {
                    "artist" -> letter(filed(s)); "album" -> album(s)?.let(::letter) ?: "other songs"
                    "date added" -> monthOf(f.getValue(s).addedSec); "year" -> f.getValue(s).year?.toString() ?: "no year"
                    else -> letter(s.title)
                }
                g to Line.Item("s" + s.id, s.title, s.artist, s.art, listOf(s))
            })
        }
        else -> {
            val albums = LinkedHashMap<String, Group>()
            for (s in songs) {
                val a = filed(s)
                val g = albums.getOrPut((album(s).orEmpty() + "\u0000" + a).lowercase()) { Group(album(s) ?: "other songs", a) }
                g.songs += s; g.year = maxOf(g.year, f.getValue(s).year ?: 0); g.added = maxOf(g.added, f.getValue(s).addedSec)
            }
            albums.values.forEach { it.songs.sortWith(byTrack) }
            if (view == "albums") {
                val list = albums.values.sortedWith(when (sort) {
                    "a–z" -> compareBy(byName) { it.title }
                    "date added" -> compareByDescending { it.added }
                    "year" -> compareByDescending<Group> { it.year }.thenBy(byName) { it.artist }
                    else -> compareBy<Group, String>(byName) { it.artist }.thenBy { it.year }.thenBy(byName) { it.title }
                })
                headed(list.map { g ->
                    val head = when (sort) { "a–z" -> letter(g.title); "date added" -> monthOf(g.added); "year" -> if (g.year > 0) g.year.toString() else "no year"; else -> letter(g.artist) }
                    head to Line.Item("a" + g.title + g.artist, g.title, g.artist + (if (g.year > 0) " · ${g.year}" else ""), g.songs.firstNotNullOfOrNull { it.art }, g.songs)
                })
            } else {
                val artists = albums.values.groupBy { it.artist.lowercase() }.values.map { gs ->
                    val sorted = gs.sortedWith(compareByDescending<Group> { it.year }.thenBy(byName) { it.title })
                    Triple(gs.first().artist, gs.size, sorted.flatMap { it.songs })
                }
                val list = if (sort == "most songs") artists.sortedWith(compareByDescending<Triple<String, Int, List<Song>>> { it.third.size }.thenBy(byName) { it.first })
                    else artists.sortedWith(compareBy(byName) { it.first })
                headed(list.map { (name, albumCount, ss) ->
                    val sub = (if (albumCount > 1) "$albumCount albums · " else "") + if (ss.size == 1) "1 song" else "${ss.size} songs"
                    (if (sort == "most songs") null else letter(name)) to Line.Item("r$name", name, sub, null, ss)
                })
            }
        }
    }
    return out
}
