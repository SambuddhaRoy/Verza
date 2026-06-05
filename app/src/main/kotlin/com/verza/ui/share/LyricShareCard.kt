package com.verza.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.verza.ui.theme.VerzaShape

/** Preview + share for a single lyric line, via the shared [ShareCardOverlay]. */
@Composable
fun LyricShareOverlay(
    line: String,
    prevLine: String?,
    nextLine: String?,
    title: String,
    artist: String,
    artworkUrl: String?,
    onDismiss: () -> Unit,
) {
    ShareCardOverlay(
        fileName = "verza-lyric-${System.currentTimeMillis()}.png",
        chooserTitle = "Share lyric",
        onDismiss = onDismiss,
    ) { modifier ->
        LyricCard(
            modifier = modifier,
            line = line,
            prevLine = prevLine,
            nextLine = nextLine,
            title = title,
            artist = artist,
            artworkUrl = artworkUrl,
        )
    }
}

/**
 * The editorial lyric card itself — a moody, cover-coloured poster with the quoted line in
 * Newsreader, the surrounding lines faded for context, and a small now-playing header + wordmark.
 * Drawn purely from [LocalCoverColors] so it looks the same regardless of the active theme.
 */
@Composable
fun LyricCard(
    modifier: Modifier,
    line: String,
    prevLine: String?,
    nextLine: String?,
    title: String,
    artist: String,
    artworkUrl: String?,
) {
    val cover = LocalCoverColors.current
    Box(
        modifier = modifier
            .background(cover.bg)
            .moodyBackdrop(cover)
            .grain(0.06f),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(26.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Header — small cover + now-playing identity.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (artworkUrl != null) {
                    Box(modifier = Modifier.size(46.dp).clip(VerzaShape)) {
                        AsyncImage(
                            model = artworkUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 20.sp),
                        color = cover.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artist.uppercase(),
                        style = TextStyle(fontFamily = FontMono, fontSize = 10.sp, letterSpacing = 0.12.em),
                        color = cover.sub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // The quote.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!prevLine.isNullOrBlank()) {
                    Text(
                        text = prevLine,
                        style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 22.sp),
                        color = cover.faint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "“$line”",
                    style = TextStyle(
                        fontFamily = FontSleeve,
                        fontWeight = FontWeight.Normal,
                        fontSize = 32.sp,
                        lineHeight = 37.sp,
                        letterSpacing = (-0.01).em,
                    ),
                    color = cover.ink,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!nextLine.isNullOrBlank()) {
                    Text(
                        text = nextLine,
                        style = TextStyle(fontFamily = FontSleeve, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 22.sp),
                        color = cover.faint,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Footer — hairline + wordmark.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
}
