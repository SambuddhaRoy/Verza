package com.verza

import android.content.Context
import androidx.room.Room
import com.verza.data.Account
import com.verza.data.Settings
import com.verza.data.library.Library
import com.verza.data.library.LibraryDb
import com.verza.data.sources.Source
import com.verza.data.sources.Sources
import com.verza.data.youtube.Quality
import com.verza.data.youtube.Streams
import com.verza.data.youtube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Every long-lived object, built once and on first use. Screens and the playback service take what
 * they need from here; nothing else creates these.
 */
class Graph(val context: Context) {
    /** Lives as long as the process: settings mirrored into memory, background writes. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The one HTTP client: YouTube, sources, covers, lyrics and the player share its pool. */
    val http: OkHttpClient by lazy {
        OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()
    }

    // The old app's database file, opened as it is (see data/library/Tables.kt).
    val db: LibraryDb by lazy { Room.databaseBuilder(context, LibraryDb::class.java, "verza.db").build() }

    val settings: Settings by lazy { Settings(context) }
    val saved: com.verza.data.Saved by lazy { com.verza.data.Saved(context) }
    // The mixes on Home: made on the phone from what you play, and from ListenBrainz when connected.
    private val mixSource by lazy { com.verza.data.mixes.MusicRepository(youtube) }
    val mixPrefs by lazy { com.verza.data.mixes.PreferencesRepository(context) }
    val listenBrainz by lazy { com.verza.data.mixes.ListenBrainzRepository(context, http, mixSource) }
    val mixes by lazy {
        com.verza.data.mixes.MixesRepository(context, scope, db.stats(), mixSource, com.verza.data.mixes.GenreRepository(context, http), mixPrefs, listenBrainz)
    }
    val discovery by lazy { com.verza.data.mixes.Discovery(mixSource, db, settings.discoveryServed) }
    /** The equaliser, bass and loudness on the player's output (bound by PlaybackService). */
    val effects by lazy { com.verza.playback.AudioEffectsController() }
    val updates: com.verza.data.Updates by lazy { com.verza.data.Updates(context, http) }
    val lyrics: com.verza.data.LyricsSource by lazy { com.verza.data.LyricsSource(http) }
    val artwork: com.verza.data.ArtworkRepository by lazy { com.verza.data.ArtworkRepository(http, context) }
    val phone: com.verza.data.phone.PhoneMusic by lazy { com.verza.data.phone.PhoneMusic(context, library, artwork) }
    val library: Library by lazy { Library(db) }
    val backup by lazy { com.verza.data.library.Backup(db) }
    val sync: com.verza.data.library.Sync by lazy { com.verza.data.library.Sync(db, youtube, account) }
    val account: Account by lazy { Account(context, scope) }
    val youtube: YouTube by lazy { YouTube(http) { account.cookie.value } }

    // Read on the player's loader threads, so held in memory rather than read from disk each time.
    private val quality: StateFlow<Quality> by lazy {
        settings.quality.flow.map { runCatching { Quality.valueOf(it) }.getOrDefault(Quality.HIGH) }
            .stateIn(scope, SharingStarted.Eagerly, Quality.HIGH)
    }
    val sourceList: StateFlow<List<Source>> by lazy { settings.sources.flow.stateIn(scope, SharingStarted.Eagerly, emptyList()) }
    val sourcesOnMobileData: StateFlow<Boolean> by lazy { settings.sourcesOnMobileData.flow.stateIn(scope, SharingStarted.Eagerly, false) }

    val streams: Streams by lazy { Streams(http) { quality.value } }
    val sources: Sources by lazy { Sources(http) { sourceList.value } }
    val downloads: com.verza.data.downloads.Downloads by lazy {
        com.verza.data.downloads.Downloads(com.verza.data.downloads.DownloadStore(context), streams, http, library, settings, scope)
    }
    /** Likes or unlikes a song, and sends it to the account straight away when it can. */
    fun like(song: com.verza.data.Song, liked: Boolean) {
        scope.launch { library.setLiked(song, liked); sync.flush() }
    }

    val player: com.verza.playback.Player by lazy { com.verza.playback.Player(context, settings, scope) { youtube.radio(it.id) } }
}
