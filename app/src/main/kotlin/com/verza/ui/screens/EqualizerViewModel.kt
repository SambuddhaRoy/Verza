package com.verza.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verza.audio.AudioEffectsController
import com.verza.audio.EqMetadata
import com.verza.audio.EqPreset
import com.verza.data.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Snapshot the Equalizer screen renders from. */
data class EqUiState(
    val metadata: EqMetadata,
    val enabled: Boolean,
    /** Per-band gains in millibels, always sized to [EqMetadata.bandCount]. */
    val bandLevelsMb: List<Int>,
    val bassStrength: Int,
    val loudnessEnabled: Boolean,
    /** The active preset, or null when bands were hand-tuned ("Custom"). */
    val activePreset: EqPreset? = null,
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val effects: AudioEffectsController,
) : ViewModel() {

    init {
        // Read the device band layout so the screen shows real bands even before playback starts.
        effects.ensureMetadata()
    }

    val state: StateFlow<EqUiState> = combine(
        effects.metadata,
        prefs.eqEnabledFlow,
        prefs.eqBandsFlow,
        prefs.bassStrengthFlow,
        prefs.loudnessEnabledFlow,
    ) { metadata, enabled, savedBands, bass, loudness ->
        EqUiState(
            metadata = metadata,
            enabled = enabled,
            bandLevelsMb = fitBands(savedBands, metadata.bandCount),
            bassStrength = bass,
            loudnessEnabled = loudness,
        )
    }.combine(prefs.eqPresetFlow) { partial, presetName ->
        partial.copy(activePreset = EqPreset.byName(presetName))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EqUiState(EqMetadata.Default, false, List(EqMetadata.Default.bandCount) { 0 }, 0, false),
    )

    /** Pads/truncates saved levels to the device band count; missing bands read as flat (0 mB). */
    private fun fitBands(saved: List<Int>, count: Int): List<Int> =
        List(count) { i -> saved.getOrElse(i) { 0 } }

    fun setEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setEqEnabled(enabled) }

    fun setBand(index: Int, levelMb: Int) {
        viewModelScope.launch {
            val md = effects.metadata.value
            val current = state.value.bandLevelsMb.toMutableList()
            if (index in current.indices) {
                current[index] = levelMb.coerceIn(md.minLevelMb, md.maxLevelMb)
                prefs.setEqBands(current)
                prefs.setEqPreset(null)   // hand-tuning a band makes it "Custom"
            }
        }
    }

    /** Resets every band to 0 dB (flat) — the Flat preset. */
    fun resetBands() {
        viewModelScope.launch {
            prefs.setEqBands(List(effects.metadata.value.bandCount) { 0 })
            prefs.setEqPreset(EqPreset.FLAT.name)
        }
    }

    /** Applies a preset's curve to the device bands and turns the equaliser on. */
    fun applyPreset(preset: EqPreset) {
        viewModelScope.launch {
            prefs.setEqBands(preset.levelsFor(effects.metadata.value))
            prefs.setEqEnabled(true)
            prefs.setEqPreset(preset.name)
        }
    }

    fun setBassStrength(strength: Int) = viewModelScope.launch { prefs.setBassStrength(strength) }

    fun setLoudnessEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setLoudnessEnabled(enabled) }
}
