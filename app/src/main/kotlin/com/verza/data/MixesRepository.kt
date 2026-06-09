package com.verza.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.verza.data.db.PlayEventDao
import com.verza.data.db.SongStat
import com.verza.di.ApplicationScope
import com.verza.innertube.SearchFilter
import com.verza.innertube.models.HomeItem
import com.verza.innertube.models.MusicItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

// ── Curated mix model ────────────────────────────────────────────────────────

enum class MixKind { DAYLIST, DISCOVER, RELEASE_RADAR }

/**
 * A Verza-curated playlist generated *on device* from the user's own play history plus YouTube's
 * radio / artist data — no backend, no tracking. [items] are [HomeItem]s so a mix can mix playable
 * songs (Daylist, Discover) and browseable release cards (Release Radar) and reuse the standard
 * row + open routing.
 */
@Serializable
data class CuratedMix(
    val id: String,
    val kind: MixKind,
    val title: String,
    val subtitle: String,
    val items: List<HomeItem>,
    val generatedAt: Long,
) {
    /** The directly-playable songs in this mix (for a "Play all"). */
    val playableSongs: List<HomeItem> get() = items.filter { it.videoId != null }
}

private val Context.mixesStore by preferencesDataStore(name = "verza_mixes")

/**
 * Generates and caches Verza's on-device curated mixes (Daylist, Discover, Release Radar). Mixes are
 * regenerated only when stale (Daylist when the daypart turns or after a few hours; the others
 * weekly) and persisted so a cold start reuses them instead of re-hitting the network. Generation
 * runs in the background on the application scope; [mixes] updates as results land.
 */
@Singleton
class MixesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val dao: PlayEventDao,
    private val music: MusicRepository,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mixesKey = stringPreferencesKey("curated_mixes_v1")

    private val _mixes = MutableStateFlow<List<CuratedMix>>(emptyList())
    val mixes: StateFlow<List<CuratedMix>> = _mixes.asStateFlow()

    init {
        scope.launch {
            _mixes.value = loadCached()
            refresh()
        }
    }

    fun getMix(id: String): CuratedMix? = _mixes.value.firstOrNull { it.id == id }

    /** Regenerates any stale mix (keeping the fresh ones) and persists the result. */
    suspend fun refresh() {
        val current = _mixes.value.associateBy { it.id }
        coroutineScope {
            val daylist = async { current["daylist"]?.takeUnless { isStale(it) } ?: generateDaylist() }
            val discover = async { current["discover"]?.takeUnless { isStale(it) } ?: generateDiscover() }
            val radar = async { current["release_radar"]?.takeUnless { isStale(it) } ?: generateReleaseRadar() }
            val list = listOfNotNull(daylist.await(), discover.await(), radar.await())
            _mixes.value = list
            persist(list)
        }
    }

    // ── Generators ────────────────────────────────────────────────────────────

    /** Daylist: the songs you favour in the current part of the day, expanded with a radio mix. */
    private suspend fun generateDaylist(): CuratedMix? {
        val (hours, label) = currentDaypart()
        var seeds = dao.topSongsInHours(hours, 8).filter { isStreamable(it.id) }
        if (seeds.isEmpty()) seeds = dao.topSongsOnce(8).filter { isStreamable(it.id) }
        if (seeds.isEmpty()) return null

        val items = LinkedHashMap<String, HomeItem>()
        seeds.forEach { items[it.id] = it.toHomeSong() }
        music.radio(seeds.first().id).getOrNull()?.drop(1)?.forEach { m ->
            if (isStreamable(m.id)) items.putIfAbsent(m.id, m.toHomeSong())
        }
        val list = items.values.toList().take(40)
        if (list.size < 5) return null
        return CuratedMix(
            id = "daylist",
            kind = MixKind.DAYLIST,
            title = label,
            subtitle = "The songs that score your ${label.lowercase()}",
            items = list,
            generatedAt = System.currentTimeMillis(),
        )
    }

    /** Discover: radio seeded from your most-listened tracks, minus anything you've already heard. */
    private suspend fun generateDiscover(): CuratedMix? {
        val seeds = dao.topSongsOnce(8).filter { isStreamable(it.id) }.take(5)
        if (seeds.size < 2) return null
        val heard = dao.playedSongIds().toHashSet()
        val seedIds = seeds.mapTo(hashSetOf()) { it.id }
        val out = LinkedHashMap<String, HomeItem>()
        for (seed in seeds) {
            music.radio(seed.id).getOrDefault(emptyList()).drop(1).forEach { m ->
                if (isStreamable(m.id) && m.id !in heard && m.id !in seedIds) out.putIfAbsent(m.id, m.toHomeSong())
            }
            if (out.size >= 45) break
        }
        val list = out.values.shuffled().take(30)
        if (list.size < 8) return null
        return CuratedMix(
            id = "discover",
            kind = MixKind.DISCOVER,
            title = "Discover weekly",
            subtitle = "Fresh tracks you haven't heard, picked from what you love",
            items = list,
            generatedAt = System.currentTimeMillis(),
        )
    }

    /** Release radar: the newest singles/albums from the artists you follow (or, if signed out, play most). */
    private suspend fun generateReleaseRadar(): CuratedMix? {
        val followed = music.subscribedArtists().getOrDefault(emptyList())
            .filter { it.browseId?.startsWith("UC") == true }
        val artists: List<HomeItem> = if (followed.isNotEmpty()) {
            followed.take(6)
        } else {
            dao.topArtistsOnce(6).mapNotNull { a ->
                music.searchItems(a.artist, SearchFilter.ARTISTS).getOrDefault(emptyList())
                    .firstOrNull { it.browseId?.startsWith("UC") == true }
            }
        }
        if (artists.isEmpty()) return null

        val out = LinkedHashMap<String, HomeItem>()
        for (artist in artists) {
            val bid = artist.browseId ?: continue
            val detail = music.artistPage(bid).getOrNull() ?: continue
            detail.sections
                .filter { sec -> RELEASE_SHELVES.any { sec.title.contains(it, ignoreCase = true) } }
                .flatMap { it.items }
                .filter { it.browseId != null || it.playlistId != null }
                .take(3)
                .forEach { rel -> out.putIfAbsent(rel.browseId ?: rel.playlistId!!, rel) }
            if (out.size >= 24) break
        }
        val list = out.values.toList().take(20)
        if (list.size < 4) return null
        return CuratedMix(
            id = "release_radar",
            kind = MixKind.RELEASE_RADAR,
            title = "Release radar",
            subtitle = "The latest from artists you love",
            items = list,
            generatedAt = System.currentTimeMillis(),
        )
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun isStale(mix: CuratedMix): Boolean {
        val age = System.currentTimeMillis() - mix.generatedAt
        return when (mix.kind) {
            // Daylist turns over with the day; also refresh if it's a few hours old.
            MixKind.DAYLIST -> mix.title != currentDaypart().second || age > 3 * 60 * 60_000L
            else -> age > 7 * 24 * 60 * 60_000L
        }
    }

    private fun currentDaypart(): Pair<List<Int>, String> =
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> (5..11).toList() to "Morning"
            in 12..16 -> (12..16).toList() to "Afternoon"
            in 17..21 -> (17..21).toList() to "Evening"
            else -> listOf(22, 23, 0, 1, 2, 3, 4) to "Late night"
        }

    private fun isStreamable(id: String): Boolean =
        id.isNotBlank() && !id.startsWith("content://") && !id.startsWith("file://")

    private fun SongStat.toHomeSong() = HomeItem(title = title, subtitle = artist, thumbnailUrl = thumbnailUrl, videoId = id)
    private fun MusicItem.toHomeSong() = HomeItem(title = title, subtitle = artist, thumbnailUrl = thumbnailUrl, videoId = id)

    private suspend fun persist(list: List<CuratedMix>) {
        runCatching { context.mixesStore.edit { it[mixesKey] = json.encodeToString(list) } }
    }

    private suspend fun loadCached(): List<CuratedMix> = runCatching {
        context.mixesStore.data.first()[mixesKey]?.let { json.decodeFromString<List<CuratedMix>>(it) }
    }.getOrNull() ?: emptyList()

    private companion object {
        val RELEASE_SHELVES = listOf("single", "album", "release", "new")
    }
}
