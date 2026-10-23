package com.verza.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.verza.audio.VisualizerSignal
import kotlinx.coroutines.flow.StateFlow

/*
 * What is left of the background glow: its renderer and its colour, intensity and style settings
 * are gone (the canvas is a flat colour from the cover, painted at the root). The audio signal it
 * rode stayed, because the seek bar, the haptics and Glass's flowing background all read it.
 */

/**
 * The live audio-reactive signal, app-wide. MainActivity provides the visualizer's band flow here
 * whenever reactivity is running (permission granted + playback active), so any composable — the
 * play button, the mini-player art, the Now Playing cover — can move with the music. Null when
 * the visualizer is off; consumers should treat that as a still signal.
 */
val LocalAudioSignal = staticCompositionLocalOf<StateFlow<VisualizerSignal>?> { null }

/**
 * Which of the signal's readers are switched on. The visualizer runs when any of them is, so a
 * reader must check its own switch: the seek bar used to draw the spectrum whenever the signal was
 * there, so turning on the vibration brought back a spectrum that had been switched off.
 */
val LocalSpectrumOn = staticCompositionLocalOf { true }
val LocalWashBeatOn = staticCompositionLocalOf { true }
