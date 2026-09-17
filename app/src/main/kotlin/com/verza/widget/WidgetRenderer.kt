package com.verza.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.verza.R
import com.verza.ui.expressive.ExpressiveColors
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Draws every Verza widget from one playback state and one palette.
 *
 * The palette is the app's own, derived from the cover with the user's flavour and accent choices,
 * so a widget is the same two-tone pair as the player rather than a dark card with some text on it.
 * Shapes are white drawables tinted here. The cover is the one thing a launcher cannot shape for us,
 * so it is masked into its cookie, arch or clover in this process and sent over as a bitmap.
 */
internal object WidgetRenderer {

    /** Side of a cover bitmap. Big enough for the largest slot on a dense screen, small enough to parcel. */
    private const val ART_PX = 360

    /** How thick the cream rim is, as a share of the art's side. About 4dp on a typical 2x2 widget. */
    private const val RIM_PERCENT = 3

    fun render(context: Context, state: WidgetState, cover: Bitmap?, colors: ExpressiveColors) {
        val manager = AppWidgetManager.getInstance(context)
        val ink = Ink(colors)
        update(context, manager, NowPlayingWidget::class.java) { strip(context, state, cover, ink) }
        update(context, manager, StickerWidget::class.java) { sticker(context, state, cover, ink) }
        update(context, manager, PosterWidget::class.java) { poster(context, state, cover, ink) }
        update(context, manager, RecordWidget::class.java) { record(context, state, cover, ink) }
        update(context, manager, TotemWidget::class.java) { totem(context, state, cover, ink) }
    }

    /** Builds a widget's views only if one is actually on a home screen; masking art is not free. */
    private inline fun update(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<*>,
        build: () -> RemoteViews,
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isNotEmpty()) manager.updateAppWidget(ids, build())
    }

    // ── The widgets ──────────────────────────────────────────────────────────────────────────────

    private fun strip(context: Context, state: WidgetState, cover: Bitmap?, ink: Ink) =
        views(context, R.layout.widget_now_playing).apply {
            tint(R.id.widget_bg, ink.container)
            setImageViewBitmap(R.id.widget_cover, shaped(context, cover, R.drawable.widget_shape_cookie9, ink))
            labels(context, state, ink.onContainer, ink.onContainerMuted)
            button(R.id.widget_previous_bg, R.id.widget_previous, ink.surface, ink.onSurface, context, NowPlayingWidget.ACTION_PREVIOUS)
            button(R.id.widget_next_bg, R.id.widget_next, ink.surface, ink.onSurface, context, NowPlayingWidget.ACTION_NEXT)
            toggle(context, state, R.id.widget_toggle_bg, R.id.widget_toggle, ink.accent, ink.onAccent)
            setOnClickPendingIntent(R.id.widget_root, NowPlayingWidget.openApp(context))
        }

    private fun sticker(context: Context, state: WidgetState, cover: Bitmap?, ink: Ink) =
        views(context, R.layout.widget_sticker).apply {
            tint(R.id.widget_burst, ink.container)
            setImageViewBitmap(R.id.widget_cover, shaped(context, cover, R.drawable.widget_shape_cookie12, ink))
            tint(R.id.widget_tag_bg, ink.tertiary)
            setTextViewText(R.id.widget_title, title(context, state))
            setTextColor(R.id.widget_title, ink.onTertiary)
            toggle(context, state, R.id.widget_toggle_bg, R.id.widget_toggle, ink.accent, ink.onAccent)
            root(context, state)
        }

    private fun poster(context: Context, state: WidgetState, cover: Bitmap?, ink: Ink) =
        views(context, R.layout.widget_poster).apply {
            tint(R.id.widget_bg, ink.container)
            setImageViewBitmap(R.id.widget_cover, shaped(context, cover, R.drawable.widget_shape_arch, ink))
            tint(R.id.widget_tag_bg, ink.tertiary)
            setTextViewText(
                R.id.widget_status,
                context.getString(
                    when {
                        state.title.isBlank() -> R.string.widget_status_idle
                        state.isPlaying -> R.string.widget_status_playing
                        else -> R.string.widget_status_paused
                    },
                ),
            )
            setTextColor(R.id.widget_status, ink.onTertiary)
            labels(context, state, ink.onContainer, ink.onContainerMuted)
            // The squiggle goes flat when paused, the way the expressive progress indicator does.
            setImageViewResource(R.id.widget_wave, if (state.isPlaying) R.drawable.widget_wave else R.drawable.widget_wave_flat)
            tint(R.id.widget_wave, ink.accent)
            button(R.id.widget_previous_bg, R.id.widget_previous, ink.surface, ink.onSurface, context, NowPlayingWidget.ACTION_PREVIOUS)
            button(R.id.widget_next_bg, R.id.widget_next, ink.surface, ink.onSurface, context, NowPlayingWidget.ACTION_NEXT)

            // The wide PLAY pill: the whole frame is the button, the icon and label only draw.
            tint(R.id.widget_toggle_bg, ink.accent)
            setImageViewResource(R.id.widget_toggle_icon, if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
            tint(R.id.widget_toggle_icon, ink.onAccent)
            setTextViewText(
                R.id.widget_toggle_label,
                context.getString(if (state.isPlaying) R.string.widget_pause_label else R.string.widget_play_label),
            )
            setTextColor(R.id.widget_toggle_label, ink.onAccent)
            setContentDescription(R.id.widget_toggle, toggleLabel(context, state))
            setOnClickPendingIntent(R.id.widget_toggle, NowPlayingWidget.broadcast(context, NowPlayingWidget.ACTION_TOGGLE))
            setOnClickPendingIntent(R.id.widget_root, NowPlayingWidget.openApp(context))
        }

    private fun record(context: Context, state: WidgetState, cover: Bitmap?, ink: Ink) =
        views(context, R.layout.widget_record).apply {
            setImageViewBitmap(R.id.widget_cover, vinyl(context, cover, ink))
            // The arm drops on while it plays and parks beside the record when it does not.
            setImageViewResource(R.id.widget_arm, if (state.isPlaying) R.drawable.widget_tonearm_on else R.drawable.widget_tonearm_off)
            tint(R.id.widget_arm, ink.accent)
            toggle(context, state, R.id.widget_toggle_bg, R.id.widget_toggle, ink.accent, ink.onAccent)
            root(context, state)
        }

    private fun totem(context: Context, state: WidgetState, cover: Bitmap?, ink: Ink) =
        views(context, R.layout.widget_totem).apply {
            setImageViewBitmap(R.id.widget_cover, shaped(context, cover, R.drawable.widget_shape_clover4, ink, rim = true))
            toggle(
                context, state, R.id.widget_toggle_bg, R.id.widget_toggle, ink.accent, ink.onAccent,
                play = R.drawable.ic_widget_play_large, pause = R.drawable.ic_widget_pause_large,
            )
            button(R.id.widget_next_bg, R.id.widget_next, ink.container, ink.onContainer, context, NowPlayingWidget.ACTION_NEXT)
            root(context, state)
        }

    // ── Shared pieces ────────────────────────────────────────────────────────────────────────────

    private fun views(context: Context, layout: Int) = RemoteViews(context.packageName, layout)

    /** Colours a white drawable. ImageView.setColorFilter is one of the few tints RemoteViews allows below API 31. */
    private fun RemoteViews.tint(@IdRes id: Int, color: Int) = setInt(id, "setColorFilter", color)

    private fun title(context: Context, state: WidgetState) =
        state.title.ifBlank { context.getString(R.string.widget_nothing_playing) }

    private fun RemoteViews.labels(context: Context, state: WidgetState, ink: Int, muted: Int) {
        setTextViewText(R.id.widget_title, title(context, state))
        setTextColor(R.id.widget_title, ink)
        setTextViewText(
            R.id.widget_artist,
            if (state.title.isBlank()) context.getString(R.string.widget_tap_to_start) else state.artist,
        )
        setTextColor(R.id.widget_artist, muted)
    }

    private fun RemoteViews.button(
        @IdRes bg: Int,
        @IdRes icon: Int,
        container: Int,
        content: Int,
        context: Context,
        action: String,
    ) {
        tint(bg, container)
        tint(icon, content)
        setOnClickPendingIntent(icon, NowPlayingWidget.broadcast(context, action))
    }

    private fun RemoteViews.toggle(
        context: Context,
        state: WidgetState,
        @IdRes bg: Int,
        @IdRes icon: Int,
        container: Int,
        content: Int,
        @DrawableRes play: Int = R.drawable.ic_widget_play,
        @DrawableRes pause: Int = R.drawable.ic_widget_pause,
    ) {
        tint(bg, container)
        setImageViewResource(icon, if (state.isPlaying) pause else play)
        tint(icon, content)
        setContentDescription(icon, toggleLabel(context, state))
        setOnClickPendingIntent(icon, NowPlayingWidget.broadcast(context, NowPlayingWidget.ACTION_TOGGLE))
    }

    private fun toggleLabel(context: Context, state: WidgetState) =
        context.getString(if (state.isPlaying) R.string.widget_pause else R.string.widget_play)

    /** For the widgets with no text on them: the tap opens the app, and TalkBack says what is playing. */
    private fun RemoteViews.root(context: Context, state: WidgetState) {
        setContentDescription(
            R.id.widget_root,
            if (state.title.isBlank()) context.getString(R.string.widget_tap_to_start)
            else listOf(state.title, state.artist).filter { it.isNotBlank() }.joinToString(", "),
        )
        setOnClickPendingIntent(R.id.widget_root, NowPlayingWidget.openApp(context))
    }

    // ── Art ──────────────────────────────────────────────────────────────────────────────────────

    /**
     * The cover, cropped to fill a square and cut to [shape]. With no cover, the shape in the palette's
     * third colour with the Verza glyph on it, so an idle widget still looks deliberate.
     *
     * With [rim], the shape sits inside a cream outline of itself, the die-cut edge the loose widget
     * shapes use to stay visible on any wallpaper.
     */
    private fun shaped(
        context: Context,
        cover: Bitmap?,
        @DrawableRes shape: Int,
        ink: Ink,
        side: Int = ART_PX,
        rim: Boolean = false,
    ): Bitmap {
        if (rim) {
            val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val inset = side * RIM_PERCENT / 100
            val inner = side - 2 * inset
            canvas.outline(context, shape, inset, inner, ContextCompat.getColor(context, R.color.widget_paper))
            canvas.drawBitmap(shaped(context, cover, shape, ink, inner), inset.toFloat(), inset.toFloat(), null)
            return out
        }
        val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        ContextCompat.getDrawable(context, shape)!!.mutate().apply {
            setBounds(0, 0, side, side)
            draw(canvas)
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        if (cover != null) {
            canvas.drawBitmap(cover, centreCrop(cover), RectF(0f, 0f, side.toFloat(), side.toFloat()), fill)
        } else {
            fill.color = ink.tertiary
            canvas.drawRect(0f, 0f, side.toFloat(), side.toFloat(), fill)
            ContextCompat.getDrawable(context, R.drawable.ic_verza_glyph)?.mutate()?.apply {
                val inset = side * 3 / 10
                setBounds(inset, inset, side - inset, side - inset)
                setTint(ink.onTertiary)
                draw(canvas)
            }
        }
        return out
    }

    /** A record: a scalloped mat in the container colour, the disc, the cover as its label, and the hole. */
    private fun vinyl(context: Context, cover: Bitmap?, ink: Ink): Bitmap {
        val side = ART_PX
        val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val rim = side * RIM_PERCENT / 100
        canvas.outline(context, R.drawable.widget_shape_scallop24, rim, side - 2 * rim, ContextCompat.getColor(context, R.color.widget_paper))
        ContextCompat.getDrawable(context, R.drawable.widget_shape_scallop24)!!.mutate().apply {
            setBounds(rim, rim, side - rim, side - rim)
            setTint(ink.container)
            draw(canvas)
        }
        val discInset = side * 7 / 100
        ContextCompat.getDrawable(context, R.drawable.widget_vinyl)!!.mutate().apply {
            setBounds(discInset, discInset, side - discInset, side - discInset)
            draw(canvas)
        }
        val label = side * 36 / 100
        val offset = ((side - label) / 2).toFloat()
        canvas.drawBitmap(shaped(context, cover, R.drawable.widget_shape_circle, ink, label), offset, offset, null)
        canvas.drawCircle(side / 2f, side / 2f, side * 0.018f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink.container })
        return out
    }

    /**
     * An even outline around a [size]-px [shape] drawn at ([offset], [offset]), [offset] px thick.
     *
     * The shape stamped at points around a circle, which fattens it by the same amount everywhere.
     * Just drawing the shape bigger behind it does not: the rim goes thin on the inside of a deep notch
     * and cuts a white line into the art, which is what the four-leaf clover did.
     */
    private fun Canvas.outline(context: Context, @DrawableRes shape: Int, offset: Int, size: Int, color: Int) {
        val stamp = ContextCompat.getDrawable(context, shape)!!.mutate().apply { setTint(color) }
        for (step in 0 until 16) {
            val angle = step * Math.PI / 8
            val x = offset + (offset * cos(angle)).roundToInt()
            val y = offset + (offset * sin(angle)).roundToInt()
            stamp.setBounds(x, y, x + size, y + size)
            stamp.draw(this)
        }
    }

    /** The square in the middle of [bitmap], which is what centre-crop into a square slot shows. */
    private fun centreCrop(bitmap: Bitmap): Rect {
        val edge = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - edge) / 2
        val top = (bitmap.height - edge) / 2
        return Rect(left, top, left + edge, top + edge)
    }

    /** The palette as the plain ARGB ints RemoteViews takes. */
    private class Ink(c: ExpressiveColors) {
        val container = c.container.toArgb()
        val onContainer = c.onContainer.toArgb()
        val onContainerMuted = c.onContainerMuted.toArgb()
        val surface = c.surface.toArgb()
        val onSurface = c.onSurface.toArgb()
        val accent = c.accent.toArgb()
        val onAccent = c.onAccent.toArgb()
        val tertiary = c.tertiary.toArgb()
        val onTertiary = c.onTertiary.toArgb()
    }
}
