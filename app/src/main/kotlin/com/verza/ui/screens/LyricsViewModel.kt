package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.data.LyricLine
import com.verza.data.LyricsRepository
import com.verza.data.parseLrc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data class Synced(val lines: List<LyricLine>) : LyricsUiState
    data class Plain(val text: String) : LyricsUiState
    data object None : LyricsUiState
    data class Error(val message: String) : LyricsUiState
}

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val repository: LyricsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LyricsUiState>(LyricsUiState.Idle)
    val state: StateFlow<LyricsUiState> = _state.asStateFlow()

    // Cache the last (title, artist, duration_seconds) so we don't re-fetch while position updates.
    private var lastKey: String? = null

    fun load(title: String, artist: String, durationMs: Long) {
        if (title.isBlank()) { _state.value = LyricsUiState.None; return }
        val key = "$title|$artist|${durationMs / 1000}"
        if (key == lastKey) return
        lastKey = key

        viewModelScope.launch {
            _state.value = LyricsUiState.Loading
            repository.fetch(title, artist, durationMs).onSuccess { result ->
                val synced = result.syncedLyrics?.takeIf { it.isNotBlank() }?.let { parseLrc(it) }
                _state.value = when {
                    !synced.isNullOrEmpty() -> LyricsUiState.Synced(synced)
                    !result.plainLyrics.isNullOrBlank() -> LyricsUiState.Plain(result.plainLyrics)
                    else -> LyricsUiState.None
                }
            }.onFailure { _state.value = LyricsUiState.None } // 404 from LRCLIB → just show "no lyrics"
        }
    }
}
