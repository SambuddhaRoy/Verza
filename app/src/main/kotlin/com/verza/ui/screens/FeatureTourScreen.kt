package com.verza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.ui.theme.CaptionItalic
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.LocalVerzaExtendedColors
import kotlinx.coroutines.launch

/** One page of the feature tour: what it is, and — crucially — where to find it. */
private data class TourPage(
    val icon: ImageVector,
    val eyebrow: String,
    val title: String,
    val body: String,
    val where: String,
)

private val tourPages = listOf(
    TourPage(
        icon = Icons.Filled.Wallpaper,
        eyebrow = "The look",
        title = "An album as a poster",
        body = "Sleeve mode recolours the whole app from the cover art and turns Now Playing into a " +
            "full-bleed poster, with the queue a tap away underneath.",
        where = "Settings › Appearance › Sleeve",
    ),
    TourPage(
        icon = Icons.Filled.GraphicEq,
        eyebrow = "Atmosphere",
        title = "A glow that listens",
        body = "A soft glow drifts behind the app and takes on each song's colours. Turn on sound-" +
            "reactivity and it pulses with the beat — and the bass can tap back as haptics.",
        where = "Settings › Background glow  ·  “React to the music” / “Feel the beat”",
    ),
    TourPage(
        icon = Icons.Filled.SelfImprovement,
        eyebrow = "Deep work",
        title = "Focus sessions",
        body = "Start a timed (or open-ended) focus block. The queue tops itself up so silence never " +
            "breaks your flow, and a timed block fades out with a gentle “you focused for N minutes”.",
        where = "Now Playing › ⋯  ·  Sleeve › ⋯ More › Focus session",
    ),
    TourPage(
        icon = Icons.Filled.Bedtime,
        eyebrow = "Drift off",
        title = "Sleep timer & wind-down",
        body = "Fade out and pause after a set time, or pick “wind down” for a long, gradual fade across " +
            "the final minutes. Turn on Gentle start and the volume eases back up when you resume.",
        where = "Now Playing › ⋯ › Sleep timer  ·  Settings › Playback › Gentle start",
    ),
    TourPage(
        icon = Icons.Filled.Schedule,
        eyebrow = "Lean back",
        title = "Ambient display",
        body = "A full-screen, screen-on clock with a drifting cover — for your desk or nightstand. Tap " +
            "anywhere to exit.",
        where = "Now Playing › ⋯ › Ambient display",
    ),
    TourPage(
        icon = Icons.Filled.MenuBook,
        eyebrow = "Context",
        title = "Liner notes",
        body = "A little editorial card about what's playing — album, year, genre, and a few words — " +
            "pulled together on the fly.",
        where = "Sleeve: tap the dateline under the title  ·  Now Playing › ⋯ › Liner notes",
    ),
    TourPage(
        icon = Icons.Filled.Tune,
        eyebrow = "Sound",
        title = "Tune it to your ears",
        body = "A graphic equalizer with per-band gain, a bass-boost slider, and volume leveling that " +
            "lifts quiet tracks. Gapless playback is automatic.",
        where = "Settings › Sound › Equalizer",
    ),
    TourPage(
        icon = Icons.Filled.Insights,
        eyebrow = "Just for you",
        title = "Your Sound",
        body = "Always-on listening insights — your week, the songs you keep coming back to, and when " +
            "you listen most. It's computed on your phone and never leaves it.",
        where = "Settings › Your Sound",
    ),
    TourPage(
        icon = Icons.Filled.LibraryMusic,
        eyebrow = "Yours to keep",
        title = "Your library, your file",
        body = "Play music stored on your device, build playlists, and export everything — likes, " +
            "playlists, history — to a single file you can back up or move. Import it anywhere.",
        where = "Library (on-device songs)  ·  Settings › Data › Export / Import",
    ),
    TourPage(
        icon = Icons.Filled.Link,
        eyebrow = "Together",
        title = "Listen along",
        body = "Share your current queue as a link. A friend opens it in Verza and picks up the exact " +
            "same set, at the same spot. No account, no server.",
        where = "Now Playing › ⋯ › Share listening session",
    ),
    TourPage(
        icon = Icons.Filled.TouchApp,
        eyebrow = "Shortcut",
        title = "Press and hold",
        body = "Long-press any song, album, or playlist on Home for quick actions — play, play next, " +
            "add to queue, start a radio, or like it.",
        where = "Home › press and hold any card",
    ),
)

/**
 * A swipeable guided tour of Verza's features — what each one does and, importantly, where to find
 * it. Shown (optionally) at the end of onboarding and re-openable any time from Settings.
 */
@Composable
fun FeatureTourScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    val pages = remember { tourPages }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val onLast = pagerState.currentPage >= pages.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        // ── Top bar: title + skip ──────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "The tour",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                color = colors.primary,
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onFinish) {
                Text("Skip", style = CaptionItalic, color = ext.muted)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            TourCard(pages[page])
        }

        // ── Dots + advance ─────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pages.size) { i ->
                    val active = i == pagerState.currentPage
                    Box(
                        Modifier
                            .height(2.dp)
                            .width(if (active) 20.dp else 10.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (active) colors.primary else ext.muted.copy(alpha = 0.35f)),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (onLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier.height(48.dp),
            ) {
                Text(
                    if (onLast) "Start listening" else "Next",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun TourCard(page: TourPage) {
    val colors = MaterialTheme.colorScheme
    val ext = LocalVerzaExtendedColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            page.eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
            color = colors.primary,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            page.title,
            style = MaterialTheme.typography.displaySmall,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = ext.muted,
        )
        Spacer(Modifier.height(28.dp))
        // "Find it" — the practical part: exactly where the feature lives.
        Column {
            Text(
                "FIND IT",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                color = colors.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                page.where,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontMono),
                color = colors.onBackground,
            )
        }
    }
}
