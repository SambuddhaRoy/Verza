package com.lstn.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lstn.data.PreferencesRepository
import com.lstn.innertube.AudioQuality
import com.lstn.ui.theme.LstnTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
) : ViewModel() {

    val theme: StateFlow<LstnTheme> = prefs.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, LstnTheme.NOIR)

    val isSignedIn: StateFlow<Boolean> = prefs.cookieFlow
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val audioQuality: StateFlow<AudioQuality> = prefs.audioQualityFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AudioQuality.HIGH)

    fun setTheme(theme: LstnTheme) {
        viewModelScope.launch { prefs.setTheme(theme) }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch { prefs.setAudioQuality(quality) }
    }

    fun onSignedIn(cookie: String) {
        viewModelScope.launch { prefs.setCookie(cookie) }
    }

    fun signOut() {
        viewModelScope.launch { prefs.setCookie(null) }
    }
}
