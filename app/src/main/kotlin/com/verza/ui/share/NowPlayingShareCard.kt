package com.verza.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.verza.ui.sleeve.grain
import com.verza.ui.sleeve.moodyBackdrop
import com.verza.ui.theme.FontMono
import com.verza.ui.theme.FontSleeve
import com.verza.ui.theme.LocalCoverColors

/** Preview + share for the current track as an editorial poster, via the shared [ShareCardOverlay]. */
@Composable
fun NowPlayingShareOverlay(
    title: String,
    artist: String,
    artworkUrl: String?,
    onDismiss: () -> Unit,
) {
    ShareCardOverlay(
        fileName = "verza-track-${System.currentTimeMillis()}.png",
        chooserTitle = "Share track",
        onDismiss = onDismiss,
    ) { modifier ->
        NowPlayingCard(modifier = modifier, title = title, artist = artist, artworkUrl = artworkUrl)
    }
}

/**
 * The flagship "share the song as art" poster — a full-bleed cover, grained and graded down into
 * the cover-coloured canvas, with the editorial masthead (artist · "title") and the VERZA wordmark
 * at the foot. Drawn from [LocalCoverColors] so it's identical regardless of the active theme.
 */
@Composable
fun NowPlayingCard(
    modifier: Modifier,
    title: String,
    artist: String,
    artworkUrl: String?,
) {
    val cover = LocalCoverColors.current
    Box(modifier = modifier.background(cover.bg)) {
        // Cover photograph (grain lives here so the masthead stays crisp).
        Box(Modifier.fillMaxSize().grain(0.07f)) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize().moodyBackdrop(cover))
            }
        }
        // Grade the lower half into the canvas so the masthead reads.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.30f to Color.Transparent,
                    0.62f to cover.bg.copy(alpha = 0.55f),
                    1.0f to cover.bg,
                ),
            ),
        )

        // Masthead + wordmark at the foot.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = artist.uppercase(),
                    style = TextStyle(fontFamily = FontMono, fontSize = 11.sp, letterSpacing = 0.14.em),
                    color = cover.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "“$title”",
                    style = TextStyle(
                        fontFamily = FontSleeve,
                        fontWeight = FontWeight.Normal,
                        fontSize = 34.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.02).em,
                    ),
                    color = cover.ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(cover.line))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "VERZA",
                    style = TextStyle(fontFamily = FontMono, fontSize = 11.sp, letterSpacing = 0.30.em),
                    color = cover.sub,
                )
                Box(Modifier.size(8.dp).clip(CircleShape).background(cover.accent))
            }
        }
    }
}
