package com.verza.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.verza.data.ArtworkRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Hilt entry point so plain composables can reach the [ArtworkRepository] without a ViewModel. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ArtworkResolverEntryPoint {
    fun artworkRepository(): ArtworkRepository
}

/**
 * Resolves the best-known cover art for a song (`title` + `artist`) via [ArtworkRepository] —
 * which queries iTunes and falls back to the YT thumbnail if no match is found. Process-wide
 * cache (in the repository) means repeated calls for the same (title, artist) are free.
 *
 * Use only for actual songs — album/playlist/artist YouTube thumbnails are already accurate
 * (real album art / channel images) and an iTunes search by playlist name produces noise.
 */
@Composable
fun rememberSongArtwork(title: String, artist: String, fallback: String?): String? {
    // Local tracks carry their own embedded album-art content:// URI — use it directly and skip
    // the iTunes lookup (which would be a wrong guess and a needless network call).
    if (fallback != null && (fallback.startsWith("content://") || fallback.startsWith("file://"))) {
        return fallback
    }
    val context = LocalContext.current.applicationContext
    val repo = remember(context) {
        EntryPointAccessors.fromApplication(context, ArtworkResolverEntryPoint::class.java).artworkRepository()
    }
    val key = remember(title, artist) { "${title.trim().lowercase()}|${artist.trim().lowercase()}" }
    var resolved by remember(key) { mutableStateOf<String?>(null) }
    LaunchedEffect(key) {
        if (title.isNotBlank()) resolved = repo.resolve(title, artist)
    }
    return resolved ?: fallback
}
