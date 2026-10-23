package com.verza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verza.ui.expressive.BodyStrong
import com.verza.ui.expressive.BodyText
import com.verza.ui.expressive.LocalExpressiveColors
import com.verza.ui.expressive.PillShape
import com.verza.ui.expressive.SquircleShape
import kotlinx.coroutines.launch

/** One page of the feature tour: what it is, and — crucially — where to find it. */
private data class TourPage(
    val icon: ImageVector,
    val eyebrow: String,
    val title: String,
    val body: String,
    val where: String,
)

// Every "where" here is a real path in the app as it is. When a setting moves, move it here too:
// the tour once sent people to a "Background glow" group and a "Sleeve" switch that no longer existed.
private val tourPages = listOf(
    TourPage(
        icon = Icons.Rounded.Wallpaper,
        eyebrow = "Now Playing",
        title = "It moves with the music",
        body = "The cover's colours fill Now Playing and move with the bass. The progress bar turns " +
            "into a live spectrum, and the bass can tap back as a soft vibration.",
        where = "Settings › Appearance  ·  Settings › Sound",
    ),
    TourPage(
        icon = Icons.Rounded.AutoAwesome,
        eyebrow = "Made for you",
        title = "Mixes from your listening",
        body = "Mixes made on your phone from what you play: one for the time of day, Discover weekly, " +
            "Release radar and your genres. Connect ListenBrainz and your Spotify listening joins in.",
        where = "Home  ·  Settings › Listening elsewhere",
    ),
    TourPage(
        icon = Icons.Rounded.Lyrics,
        eyebrow = "Sing along",
        title = "Lyrics in time",
        body = "Lyrics follow the song line by line, on the same background as Now Playing.",
        where = "Now Playing › Lyrics",
    ),
    TourPage(
        icon = Icons.Rounded.SelfImprovement,
        eyebrow = "Deep work",
        title = "Focus sessions",
        body = "Start a timed or open-ended focus block. The queue tops itself up so silence never " +
            "breaks your flow, and a timed block fades out when it ends.",
        where = "Now Playing › ⋯ › Focus session",
    ),
    TourPage(
        icon = Icons.Rounded.Bedtime,
        eyebrow = "Drift off",
        title = "Sleep timer & wind-down",
        body = "Fade out and pause after a set time, or wind down with a long, gradual fade across the " +
            "last minutes. With Gentle start, the volume eases back up when you resume.",
        where = "Now Playing › ⋯ › Sleep timer  ·  Settings › Playback › Gentle start",
    ),
    TourPage(
        icon = Icons.Rounded.Schedule,
        eyebrow = "Lean back",
        title = "Ambient display",
        body = "A full-screen clock with a drifting cover that keeps the screen on, for a desk or a " +
            "nightstand. Tap anywhere to leave.",
        where = "Now Playing › ⋯ › Ambient display",
    ),
    TourPage(
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        eyebrow = "Context",
        title = "Liner notes",
        body = "A short card about what's playing: the album, the year, the genre and a few words.",
        where = "Now Playing › ⋯ › Liner notes",
    ),
    TourPage(
        icon = Icons.Rounded.GraphicEq,
        eyebrow = "Sound",
        title = "Tune it to your ears",
        body = "A ten-band equaliser with presets, bass boost, and loudness that lifts quiet tracks.",
        where = "Settings › Sound › Equalizer",
    ),
    TourPage(
        icon = Icons.Rounded.Insights,
        eyebrow = "Just for you",
        title = "Your Sound",
        body = "Your top tracks, artists and genres, worked out on your phone from what you play. " +
            "It never leaves it.",
        where = "Settings › Your library › Your Sound",
    ),
    TourPage(
        icon = Icons.Rounded.LibraryMusic,
        eyebrow = "Yours to keep",
        title = "Your library, your file",
        body = "Play music stored on your phone, and save your playlists, likes and stats to a single " +
            "file you can back up or take to another device.",
        where = "Library › On device  ·  Settings › Your library › Export",
    ),
    TourPage(
        icon = Icons.Rounded.Link,
        eyebrow = "Together",
        title = "Listen along",
        body = "Share your queue as a link. A friend opens it in Verza and picks up the same songs at " +
            "the same spot. No account, no server.",
        where = "Now Playing › ⋯ › Share listening session",
    ),
    TourPage(
        icon = Icons.Rounded.TouchApp,
        eyebrow = "Shortcut",
        title = "Press and hold",
        body = "Press and hold any song, album or playlist on Home to play it next, queue it, start a " +
            "radio or like it.",
        where = "Home › press and hold any card",
    ),
)

/**
 * A swipeable tour of Verza's features: what each does and where to find it. Offered at the end of
 * onboarding and reopenable from Settings. Dressed like onboarding: the app's own ground, the
 * masthead's eyebrow and title, and the same buttons.
 */
@Composable
fun FeatureTourScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExpressiveColors.current
    val pages = remember { tourPages }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val onLast = pagerState.currentPage >= pages.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.container)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Progress as the onboarding shows it, compressed: twelve pages would not fit as dots.
                Text(
                    "${pagerState.currentPage + 1} of ${pages.size}",
                    style = BodyStrong.copy(fontSize = 13.sp),
                    color = colors.onContainerMuted,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Skip",
                    style = BodyStrong.copy(fontSize = 15.sp),
                    color = colors.onContainerMuted,
                    modifier = Modifier
                        .clip(PillShape)
                        .clickable(role = Role.Button, onClick = onFinish)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { page ->
                TourCard(pages[page])
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 20.dp)) {
                repeat(pages.size) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(PillShape)
                            .background(if (i <= pagerState.currentPage) colors.accent else colors.onContainerMuted.copy(alpha = 0.2f)),
                    )
                }
            }
            WideButton(
                text = if (onLast) "Start listening" else "Next",
                container = colors.accent,
                content = colors.onAccent,
                onClick = {
                    if (onLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
            )
        }
    }
}

@Composable
private fun TourCard(page: TourPage) {
    val colors = LocalExpressiveColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(SquircleShape(20.dp))
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(28.dp))
        GlassMasthead(page.eyebrow, page.title, titleLines = 3)
        Spacer(Modifier.height(14.dp))
        Text(page.body, style = BodyText.copy(fontSize = 17.sp, lineHeight = 25.sp), color = colors.onContainerMuted)
        Spacer(Modifier.height(28.dp))
        // "Find it": the practical part, exactly where the feature lives.
        Column(
            Modifier
                .fillMaxWidth()
                .clip(SquircleShape(16.dp))
                .background(colors.onContainer.copy(alpha = 0.05f))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text("FIND IT", style = BodyStrong.copy(fontSize = 11.sp, letterSpacing = 1.2.sp), color = colors.accent)
            Spacer(Modifier.height(4.dp))
            // One place per line: two run together on a phone read as one long path.
            page.where.split("  ·  ").forEach { place ->
                Text(place, style = BodyText.copy(fontSize = 15.sp, lineHeight = 22.sp), color = colors.onContainer)
            }
        }
    }
}
