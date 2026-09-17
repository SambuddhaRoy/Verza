package com.verza.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.verza.MainActivity
import com.verza.player.MusicService
import com.verza.ui.expressive.AccentSource
import com.verza.ui.expressive.ColorFlavour

/**
 * Every Verza home-screen widget. They differ only in how they draw, which is [WidgetRenderer]'s
 * business, so a provider does nothing but ask for a redraw when the launcher places or refreshes it.
 *
 * RemoteViews rather than Glance. Glance would pull in a second Compose runtime, and every shape
 * here is a tinted vector or a cover bitmap masked in the app process, which RemoteViews carries fine.
 *
 * Updates arrive from [NowPlayingWidgetUpdater], which lives in the app process alongside the
 * playback service. A widget never binds a controller to watch playback: a broadcast receiver that
 * outlives its own onReceive to wait on a session binding is how widgets end up permanently stale.
 */
open class VerzaWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Paint from whatever the updater last saw. On a cold boot that is the empty state, which is
        // correct: there is nothing playing.
        NowPlayingWidgetUpdater.refresh(context)
    }
}

class StickerWidget : VerzaWidget()
class PosterWidget : VerzaWidget()
class RecordWidget : VerzaWidget()
class TotemWidget : VerzaWidget()

/**
 * The original strip widget, and the one receiver that handles transport. Every widget's buttons
 * send here, so there is one place commands arrive and one manifest entry that accepts them.
 */
class NowPlayingWidget : VerzaWidget() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val command = intent.action?.takeIf { it.startsWith(ACTION_PREFIX) } ?: return
        // Transport goes through a controller bound for the length of the command and released
        // straight after. Widgets get no lifecycle of their own to hold one across.
        val pending = goAsync()
        // The application context, not the receiver's. A receiver's context refuses to bind to a
        // service, so building the controller from it threw, which took the whole app process and the
        // music down with it on every widget button press.
        val app = context.applicationContext
        val token = SessionToken(app, ComponentName(app, MusicService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
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

        fun broadcast(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(context, NowPlayingWidget::class.java).setAction(action),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        fun openApp(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
    }
}

/** What the widgets need to draw themselves. */
data class WidgetState(
    val title: String = "",
    val artist: String = "",
    val artworkUrl: String? = null,
    val isPlaying: Boolean = false,
    // The app's own theme choices, so a widget matches the app rather than a default of its own.
    val flavour: ColorFlavour = ColorFlavour.SIGNATURE,
    val accentSource: AccentSource = AccentSource.COMPLEMENT,
)
