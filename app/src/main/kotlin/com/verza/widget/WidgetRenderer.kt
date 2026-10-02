package com.verza.widget

import android.util.SizeF
import android.os.Build
import com.verza.ui.expressive.pickTitleFace
import com.verza.ui.expressive.TitleFonts
import androidx.core.content.res.ResourcesCompat
import android.text.TextUtils
import android.text.TextPaint
import android.text.StaticLayout
import android.graphics.Typeface
import com.verza.ui.theme.DesignScheme
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

    /**
     * Whether this render is in the Poster design, where every shape is a square.
     *
     * ponytail: state on a singleton rather than a parameter threaded through every helper. Safe
     * because renders are serialised (one updater, one coroutine), and it is set at the top of
     * each one. Thread it through as a parameter if renders ever run concurrently.
     */
    @Volatile private var square = false

    /** The drawable a shape is cut from: its own silhouette, or the square in the Poster design. */
    @DrawableRes
    private fun mask(@DrawableRes shape: Int) = if (square) R.drawable.widget_shape_square else shape

    /**
     * The images whose shape the layout XML picks rather than this code. In the Poster design each
     * gets the square. Listed per layout: pointing a RemoteViews action at an id the layout does
     * not have fails the whole update on the launcher's side.
     */
    private fun RemoteViews.squareUp(@IdRes vararg ids: Int) {
        if (square) ids.forEach { setImageViewResource(it, R.drawable.widget_shape_square) }
    }

    fun render(context: Context, state: WidgetState, cover: Bitmap?, colors: ExpressiveColors) {
        square = state.design == DesignScheme.POSTER
        val manager = AppWidgetManager.getInstance(context)
        val ink = Ink(colors)
        if (square) {
            // The Poster design draws every widget the same way, the way it draws Now Playing:
            // the one-row strip side by side, the rest stacked. See [gass].
            update(context, manager, NowPlayingWidget::class.java) { stripOrStack(context, state, cover, ink) }
            for (provider in listOf(StickerWidget::class.java, PosterWidget::class.java, RecordWidget::class.java, TotemWidget::class.java)) {
                update(context, manager, provider) { gass(context, state, cover, ink, strip = false) }
            }
            return
        }
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
            squareUp(R.id.widget_bg, R.id.widget_previous_bg, R.id.widget_toggle_bg, R.id.widget_next_bg)
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
            squareUp(
                R.id.widget_burst_shadow, R.id.widget_burst, R.id.widget_tag_bg_shadow, R.id.widget_tag_bg,
                R.id.widget_toggle_bg_shadow, R.id.widget_toggle_bg,
            )
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
            squareUp(
                R.id.widget_bg, R.id.widget_tag_bg_shadow, R.id.widget_tag_bg,
                R.id.widget_previous_bg, R.id.widget_toggle_bg, R.id.widget_next_bg,
            )
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
            squareUp(R.id.widget_toggle_bg_shadow, R.id.widget_toggle_bg)
            setImageViewBitmap(R.id.widget_cover, vinyl(context, cover, ink))
            // The arm drops on while it plays and parks beside the record when it does not.
            setImageViewResource(R.id.widget_arm, if (state.isPlaying) R.drawable.widget_tonearm_on else R.drawable.widget_tonearm_off)
            tint(R.id.widget_arm, ink.accent)
            toggle(context, state, R.id.widget_toggle_bg, R.id.widget_toggle, ink.accent, ink.onAccent)
            root(context, state)
        }

    private fun totem(context: Context, state: WidgetState, cover: Bitmap?, ink: Ink) =
        views(context, R.layout.widget_totem).apply {
            squareUp(R.id.widget_toggle_bg_shadow, R.id.widget_toggle_bg, R.id.widget_next_bg_shadow, R.id.widget_next_bg)
            setImageViewBitmap(R.id.widget_cover, shaped(context, cover, R.drawable.widget_shape_clover4, ink, rim = true))
            toggle(
                context, state, R.id.widget_toggle_bg, R.id.widget_toggle, ink.accent, ink.onAccent,
                play = R.drawable.ic_widget_play_large, pause = R.drawable.ic_widget_pause_large,
            )
            button(R.id.widget_next_bg, R.id.widget_next, ink.container, ink.onContainer, context, NowPlayingWidget.ACTION_NEXT)
            root(context, state)
        }

    // ── The Poster design ────────────────────────────────────────────────────────────────────────

    /**
     * A widget in the Poster design: the cover, its title set in a display face on a block of the
     * palette, and a strip of flat slabs, previous, PLAY or PAUSE in a word twice the width, next.
     * The same pieces as Now Playing's Poster controls, so the widget looks like the app.
     *
     * The five Material widgets each have a shape of their own (a cookie, an arch, a record, a
     * totem). Poster has no shapes to vary, so they all take this one arrangement; the strip lays it
     * out in a row because a single cell of height has no room to stack it.
     *
     * Type is drawn into bitmaps here because a launcher will not load the app's fonts into a
     * RemoteViews TextView.
     */
    /**
     * The strip widget can be dragged to any size, and side by side only suits it while it is one
     * row tall; at its default two rows it left a column of empty colour beside a thin title. From
     * Android 12 the launcher is given both and picks by the size it is showing; below that, a
     * launcher cannot be asked, so it keeps the strip.
     */
    private fun stripOrStack(context: Context, state: WidgetState, cover: Bitmap?, ink: Ink): RemoteViews =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RemoteViews(
                mapOf(
                    SizeF(180f, 40f) to gass(context, state, cover, ink, strip = true),
                    SizeF(180f, 140f) to gass(context, state, cover, ink, strip = false),
                ),
            )
        } else {
            gass(context, state, cover, ink, strip = true)
        }

    private fun gass(context: Context, state: WidgetState, cover: Bitmap?, ink: Ink, strip: Boolean) =
        views(context, if (strip) R.layout.widget_gass_strip else R.layout.widget_gass).apply {
            setImageViewBitmap(R.id.widget_cover, shaped(context, cover, R.drawable.widget_shape_square, ink))
            setImageViewBitmap(R.id.widget_title_art, titleArt(context, state, ink, strip))
            if (strip) tint(R.id.widget_title_bg, ink.container)

            tint(R.id.widget_slab_previous_bg, ink.surface)
            setImageViewResource(R.id.widget_previous, R.drawable.ic_widget_previous)
            tint(R.id.widget_previous, ink.onSurface)
            setContentDescription(R.id.widget_slab_previous, context.getString(R.string.widget_previous))
            setOnClickPendingIntent(R.id.widget_slab_previous, NowPlayingWidget.broadcast(context, NowPlayingWidget.ACTION_PREVIOUS))

            tint(R.id.widget_slab_toggle_bg, ink.accent)
            setImageViewBitmap(
                R.id.widget_toggle,
                wordArt(context, context.getString(if (state.isPlaying) R.string.widget_pause_label else R.string.widget_play_label), ink.onAccent),
            )
            setContentDescription(R.id.widget_slab_toggle, toggleLabel(context, state))
            setOnClickPendingIntent(R.id.widget_slab_toggle, NowPlayingWidget.broadcast(context, NowPlayingWidget.ACTION_TOGGLE))

            tint(R.id.widget_slab_next_bg, ink.surface)
            setImageViewResource(R.id.widget_next, R.drawable.ic_widget_next)
            tint(R.id.widget_next, ink.onSurface)
            setContentDescription(R.id.widget_slab_next, context.getString(R.string.widget_next))
            setOnClickPendingIntent(R.id.widget_slab_next, NowPlayingWidget.broadcast(context, NowPlayingWidget.ACTION_NEXT))

            root(context, state)
        }

    /**
     * The title, in the same display face the app's Now Playing would give this song where it can,
     * on a block of the palette, with the artist under it in small capitals. A fixed-width bitmap;
     * the ImageView scales it to the widget.
     */
    private fun titleArt(context: Context, state: WidgetState, ink: Ink, strip: Boolean): Bitmap {
        // The strip's title sits in a narrow column, so its bitmap is narrower too; scaled down from
        // the full width its type came out at a few pixels high.
        val width = if (strip) 420 else 720
        val pad = 24
        val title = title(context, state)
        val face = pickTitleFace(title, TitleFonts.faces.size, avoid = null) { TitleFonts.covers(context, TitleFonts.faces[it], title) }
        val typeface = face?.let { runCatching { ResourcesCompat.getFont(context, TitleFonts.faces[it]) }.getOrNull() }
        val (block, ink1) = if (strip) ink.container to ink.onContainer else ink.tertiary to ink.onTertiary
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink1
            textSize = if (strip) 64f else 84f
            this.typeface = typeface ?: Typeface.DEFAULT_BOLD
        }
        val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, width - 2 * pad)
            .setMaxLines(2)
            .setEllipsize(TextUtils.TruncateAt.END)
            .setLineSpacing(0f, 0.92f)
            .setIncludePad(false)
            .build()
        val artist = if (state.title.isBlank()) "" else state.artist.uppercase()
        val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink1
            alpha = 210
            textSize = 26f
            letterSpacing = 0.1f
        }
        val artistHeight = if (artist.isBlank()) 0 else (artistPaint.fontSpacing + 6).toInt()
        val height = pad + titleLayout.height + artistHeight + pad
        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        // The block hugs the text rather than spanning the width: a label stuck on the cover.
        val used = (0 until titleLayout.lineCount).maxOf { titleLayout.getLineWidth(it) }
            .coerceAtLeast(artistPaint.measureText(artist)) + 2 * pad
        canvas.drawRect(0f, 0f, if (strip) width.toFloat() else used.coerceAtMost(width.toFloat()), height.toFloat(), Paint().apply { color = block })
        canvas.save()
        canvas.translate(pad.toFloat(), pad.toFloat())
        titleLayout.draw(canvas)
        canvas.restore()
        if (artist.isNotBlank()) {
            val ellipsized = TextUtils.ellipsize(artist, artistPaint, (width - 2 * pad).toFloat(), TextUtils.TruncateAt.END)
            canvas.drawText(ellipsized, 0, ellipsized.length, pad.toFloat(), pad + titleLayout.height + artistPaint.fontSpacing, artistPaint)
        }
        return out
    }

    /** A word, set in Anton, centred in a bitmap with room around it so it sits in its slab. */
    private fun wordArt(context: Context, word: String, color: Int): Bitmap {
        val out = Bitmap.createBitmap(320, 120, Bitmap.Config.ARGB_8888)
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = 62f
            textAlign = Paint.Align.CENTER
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.display_anton) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        }
        val y = out.height / 2f - (paint.descent() + paint.ascent()) / 2f
        Canvas(out).drawText(word, out.width / 2f, y, paint)
        return out
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
        ContextCompat.getDrawable(context, mask(shape))!!.mutate().apply {
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
        // The mat squares up in the Poster design, a sleeve rather than a scalloped mat. The disc
        // and its label stay round: that is a picture of a record, not an edge of the interface.
        ContextCompat.getDrawable(context, mask(R.drawable.widget_shape_scallop24))!!.mutate().apply {
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
        canvas.drawBitmap(labelArt(context, cover, ink, label), offset, offset, null)
        canvas.drawCircle(side / 2f, side / 2f, side * 0.018f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink.container })
        return out
    }

    /** The record's centre label: always round, whatever the design (see [vinyl]). */
    private fun labelArt(context: Context, cover: Bitmap?, ink: Ink, side: Int): Bitmap {
        val wasSquare = square
        square = false
        return try { shaped(context, cover, R.drawable.widget_shape_circle, ink, side) } finally { square = wasSquare }
    }

    /**
     * An even outline around a [size]-px [shape] drawn at ([offset], [offset]), [offset] px thick.
     *
     * The shape stamped at points around a circle, which fattens it by the same amount everywhere.
     * Just drawing the shape bigger behind it does not: the rim goes thin on the inside of a deep notch
     * and cuts a white line into the art, which is what the four-leaf clover did.
     */
    private fun Canvas.outline(context: Context, @DrawableRes shape: Int, offset: Int, size: Int, color: Int) {
        val stamp = ContextCompat.getDrawable(context, mask(shape))!!.mutate().apply { setTint(color) }
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
