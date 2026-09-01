package com.verza.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.verza.MainActivity
import com.verza.R
import com.verza.player.MusicService

/**
 * The home-screen widget: cover, title, artist, and transport.
 *
 * RemoteViews rather than Glance. A widget of one image, two labels and three buttons is the case
 * RemoteViews is actually good at, and it costs no dependency — Glance would pull in a second
 * Compose runtime to draw four views.
 *
 * Updates arrive from [NowPlayingWidgetUpdater], which lives in the app process alongside the
 * playback service. The widget itself never binds a controller: a broadcast receiver that outlives
 * its own onReceive to wait on a session binding is how widgets end up permanently stale.
 */
class NowPlayingWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Paint from whatever the updater last saw. On a cold boot that is the empty state, which is
        // correct — there is nothing playing.
        NowPlayingWidgetUpdater.refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val command = intent.action?.takeIf { it.startsWith(ACTION_PREFIX) } ?: return
        // Transport goes through a controller bound for the length of the command and released
        // straight after. Widgets get no lifecycle of their own to hold one across.
        val pending = goAsync()
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching {
                val ctrl = future.get()
                when (command) {
                    ACTION_TOGGLE -> if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
                    ACTION_NEXT -> ctrl.seekToNextMediaItem()
                    ACTION_PREVIOUS -> ctrl.seekToPreviousMediaItem()
                }
                ctrl.release()
            }
            NowPlayingWidgetUpdater.refresh(context)
            pending.finish()
        }, MoreExecutors.directExecutor())
    }

    companion object {
        private const val ACTION_PREFIX = "com.verza.widget."
        const val ACTION_TOGGLE = ACTION_PREFIX + "TOGGLE"
        const val ACTION_NEXT = ACTION_PREFIX + "NEXT"
        const val ACTION_PREVIOUS = ACTION_PREFIX + "PREVIOUS"

        /** Draw [state] onto every instance of the widget. */
        fun render(context: Context, state: WidgetState, cover: Bitmap?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NowPlayingWidget::class.java))
            if (ids.isEmpty()) return

            val views = RemoteViews(context.packageName, R.layout.widget_now_playing).apply {
                setTextViewText(R.id.widget_title, state.title.ifBlank { "Nothing playing" })
                setTextViewText(R.id.widget_artist, state.artist)
                setImageViewResource(
                    R.id.widget_toggle,
                    if (state.isPlaying) R.drawable.ic_tile_pause else R.drawable.ic_tile_play,
                )
                if (cover != null) {
                    setImageViewBitmap(R.id.widget_cover, cover)
                } else {
                    setImageViewResource(R.id.widget_cover, R.mipmap.ic_launcher)
                }
                setOnClickPendingIntent(R.id.widget_toggle, broadcast(context, ACTION_TOGGLE))
                setOnClickPendingIntent(R.id.widget_next, broadcast(context, ACTION_NEXT))
                setOnClickPendingIntent(R.id.widget_previous, broadcast(context, ACTION_PREVIOUS))
                setOnClickPendingIntent(R.id.widget_root, openApp(context))
            }
            manager.updateAppWidget(ids, views)
        }

        private fun broadcast(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(context, NowPlayingWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun openApp(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
    }
}

/** What the widget needs to draw itself. */
data class WidgetState(
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
)
