package com.verza.innertube

/** Preferred audio stream quality used when resolving a track's stream. */
enum class AudioQuality {
    LOW,     // smallest stream (data saver)
    MEDIUM,  // ~128 kbps
    HIGH,    // highest available bitrate
}
