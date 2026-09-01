package com.verza.tile

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.verza.R
import com.verza.player.MusicService

/**
 * Play and pause from the notification shade.
 *
 * The tile binds a MediaController to our own session rather than dispatching a media key, which
 * would go to whatever session the system currently considers active — often a different app's, and
 * from a tile labelled Verza that is simply wrong.
 *
 * The controller is held only while the shade is open. TileService is started and stopped around
 * that window by the system, so [onStartListening] and [onStopListening] are the natural lifetime,
 * and holding a session binding any longer would keep the service warm for no reason.
 */
class PlayPauseTileService : TileService() {

    private var controller: MediaController? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = render()
        override fun onPlaybackStateChanged(playbackState: Int) = render()
        override fun onMediaItemTransition(item: androidx.media3.common.MediaItem?, reason: Int) = render()
    }

    override fun onStartListening() {
        super.onStartListening()
        render() // paint something immediately; the controller may take a moment
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            // Same guard as PlayerConnection: this runs on the completing thread, and a session
            // that cannot be bound throws out of get() with nowhere to catch it.
            controller = runCatching { future.get() }.getOrNull()?.also {
                it.addListener(listener)
            }
            render()
        }, MoreExecutors.directExecutor())
    }

    override fun onStopListening() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val ctrl = controller ?: return
        if (ctrl.isPlaying) {
            ctrl.pause()
        } else {
            // Nothing queued yet — send the user to the app rather than starting silence.
            if (ctrl.mediaItemCount == 0) {
                startActivityAndCollapse(
                    android.app.PendingIntent.getActivity(
                        this,
                        0,
                        packageManager.getLaunchIntentForPackage(packageName),
                        android.app.PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                return
            }
            ctrl.play()
        }
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val ctrl = controller
        val playing = ctrl?.isPlaying == true
        val title = ctrl?.currentMediaItem?.mediaMetadata?.title?.toString()

        tile.state = if (ctrl == null) Tile.STATE_UNAVAILABLE else Tile.STATE_ACTIVE
        tile.icon = Icon.createWithResource(
            this,
            if (playing) R.drawable.ic_tile_pause else R.drawable.ic_tile_play,
        )
        // The track name is more useful on the tile than the word "Verza", which the label already
        // says. Falls back to the app name before anything has played.
        tile.label = title?.takeIf { it.isNotBlank() } ?: getString(R.string.app_name)
        tile.contentDescription = if (playing) "Pause" else "Play"
        tile.updateTile()
    }
}
