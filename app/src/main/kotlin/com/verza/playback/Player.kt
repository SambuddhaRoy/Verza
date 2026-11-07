package com.verza.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player.Listener
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.verza.data.Settings
import com.verza.data.Song
import com.verza.data.artAt
import com.verza.data.sources.losslessLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What's playing, as the screens draw it. */
data class NowPlaying(
    val song: Song? = null,
    val playing: Boolean = false,
    val loading: Boolean = false,
    val durationMs: Long = 0,
    val queue: List<Song> = emptyList(),
    val index: Int = 0,
    val shuffle: Boolean = false,
    /** Off, all or one, as Media3 numbers them. */
    val repeat: Int = REPEAT_MODE_OFF,
    /** "FLAC · 24-bit / 96 kHz" when the song really is lossless, else null. */
    val lossless: String? = null,
)

/**
 * A sleep timer: pause at [atMs] (elapsedRealtime), the volume falling over the last [fadeMs] first;
 * or, with no [atMs], at the end of the song playing. The service carries it out.
 */
data class Sleep(val atMs: Long?, val fadeMs: Long = 4_000)

/**
 * The screens' handle on the player in [PlaybackService]: one connection for the whole app, made on
 * first use. On the first connection of a launch an empty player gets the saved queue back, paused
 * where it was.
 */
class Player(
    private val context: Context,
    private val settings: Settings,
    private val scope: CoroutineScope,
    private val radio: suspend (Song) -> List<Song>,
) {
    private val _state = MutableStateFlow(NowPlaying())
    val state: StateFlow<NowPlaying> = _state.asStateFlow()

    /** The sleep timer, if one is set. In memory: the service lives in this process. */
    val sleep = MutableStateFlow<Sleep?>(null)

    /** Sleep in [minutes]; [windDown] fades over the last third of it (one to five minutes). */
    fun sleepIn(minutes: Int, windDown: Boolean = false) {
        val ms = minutes * 60_000L
        sleep.value = Sleep(android.os.SystemClock.elapsedRealtime() + ms, if (windDown) (ms / 3).coerceIn(60_000, 300_000) else 4_000)
    }

    private var controller: MediaController? = null
    private val connecting = Mutex()

    /** Where the song is now. Read on demand (the scrubber polls it), not pushed through [state]. */
    val position: Long get() = controller?.currentPosition ?: 0

    // A connection is dropped when the service is destroyed and made again; commands to a dropped one
    // change the controller's own idea of the queue and play nothing, so a dropped one is replaced.
    private suspend fun ready(): MediaController = connecting.withLock {
        controller?.takeIf { it.isConnected } ?: MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .buildAsync().await().also { c ->
                controller = c
                c.addListener(object : Listener {
                    override fun onEvents(player: androidx.media3.common.Player, events: androidx.media3.common.Player.Events) = publish(c)
                })
                if (c.mediaItemCount == 0 && settings.resumeOnOpen.get()) {
                    val saved = settings.savedQueue.get()
                    if (saved.tracks.isNotEmpty()) {
                        c.setMediaItems(saved.tracks.map { item(it.toSong()) }, saved.index.coerceIn(0, saved.tracks.lastIndex), saved.positionMs)
                        c.prepare()
                    }
                }
                publish(c)
            }
    }

    /** Connects now, so the mini player shows the last song as soon as the app opens. */
    fun connect() { scope.launch(Dispatchers.Main) { ready() } }

    private fun act(block: (MediaController) -> Unit) { scope.launch(Dispatchers.Main) { block(ready()) } }

    fun play(songs: List<Song>, index: Int = 0, startMs: Long = 0) = act { c ->
        c.setMediaItems(songs.map(::item), index.coerceIn(0, songs.lastIndex.coerceAtLeast(0)), startMs)
        c.shuffleModeEnabled = false
        c.prepare(); c.play()
    }

    /** This song now, then its radio after it, so the music doesn't stop after one song. */
    fun playWithRadio(song: Song) {
        play(listOf(song))
        scope.launch {
            val more = runCatching { radio(song) }.getOrDefault(emptyList()).filter { it.id != song.id }
            act { c -> if (c.currentMediaItem?.mediaId == song.id && c.mediaItemCount == 1) c.addMediaItems(more.map(::item)) }
        }
    }

    /** A song known only by its id, from a shared link: its radio, which starts with it. */
    fun playId(id: String) {
        scope.launch {
            val songs = runCatching { radio(Song(id, "", "")) }.getOrDefault(emptyList())
            val at = songs.indexOfFirst { it.id == id }
            if (at >= 0) play(songs, at)
            else kotlinx.coroutines.withContext(Dispatchers.Main) { android.widget.Toast.makeText(context, "Couldn't open that song", android.widget.Toast.LENGTH_SHORT).show() }
        }
    }

    /** Keeps the song playing and puts [songs] after it, in place of whatever came next. */
    fun replaceUpcoming(songs: List<Song>) = act { c ->
        c.removeMediaItems(c.currentMediaItemIndex + 1, c.mediaItemCount)
        c.addMediaItems(songs.map(::item))
    }

    fun shuffle(songs: List<Song>) = act { c ->
        c.setMediaItems(songs.map(::item)); c.shuffleModeEnabled = true
        c.seekTo(c.currentTimeline.getFirstWindowIndex(true), 0); c.prepare(); c.play()
    }

    fun toggle() = act { if (it.isPlaying) it.pause() else { if (it.playbackState == androidx.media3.common.Player.STATE_IDLE) it.prepare(); it.play() } }
    fun next() = act { it.seekToNextMediaItem() }
    /** Back to the start of the song, or to the one before when it's only just begun. */
    fun previous() = act { it.seekToPrevious() }
    fun seek(ms: Long) = act { it.seekTo(ms) }
    fun jumpTo(index: Int) = act { it.seekTo(index, 0); it.play() }
    fun playNext(song: Song) = act { it.addMediaItem((it.currentMediaItemIndex + 1).coerceAtMost(it.mediaItemCount), item(song)) }
    fun enqueue(songs: List<Song>) = act { it.addMediaItems(songs.map(::item)) }
    fun remove(index: Int) = act { it.removeMediaItem(index) }
    fun move(from: Int, to: Int) = act { it.moveMediaItem(from, to) }
    fun toggleShuffle() = act { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    fun cycleRepeat() = act { it.repeatMode = when (it.repeatMode) { REPEAT_MODE_OFF -> REPEAT_MODE_ALL; REPEAT_MODE_ALL -> REPEAT_MODE_ONE; else -> REPEAT_MODE_OFF } }

    private fun publish(c: MediaController) {
        val queue = (0 until c.mediaItemCount).map { c.getMediaItemAt(it).toSong() }
        _state.value = NowPlaying(
            song = c.currentMediaItem?.toSong(),
            playing = c.isPlaying,
            loading = c.playbackState == androidx.media3.common.Player.STATE_BUFFERING,
            durationMs = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: c.currentMediaItem?.mediaMetadata?.durationMs ?: 0,
            queue = queue,
            index = c.currentMediaItemIndex,
            shuffle = c.shuffleModeEnabled,
            repeat = c.repeatMode,
            lossless = lossless(c.currentTracks),
        )
    }

    /** The format the player really opened, for the Lossless mark. */
    private fun lossless(tracks: Tracks): String? {
        val group = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected } ?: return null
        val i = (0 until group.length).firstOrNull(group::isTrackSelected) ?: return null
        val f = group.getTrackFormat(i)
        return losslessLabel(f.sampleMimeType, f.pcmEncoding, f.sampleRate, f.containerMimeType)
    }
}

/** A song as a queued item. YouTube songs get the placeholder the resolver replaces. */
fun item(song: Song): MediaItem = MediaItem.Builder()
    .setMediaId(song.id)
    .setUri(if (song.onPhone) Uri.parse(song.id) else placeholder(song.id, song.title, song.artist, song.durationMs))
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.art?.let { Uri.parse(artAt(it, 544)) })
            .setDurationMs(song.durationMs.takeIf { it > 0 })
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build(),
    )
    .build()
