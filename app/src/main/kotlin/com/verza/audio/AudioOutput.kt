package com.verza.audio

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.getSystemService

/** Broad families of output, enough to choose an icon and a sensible label. */
enum class OutputKind { SPEAKER, EARPIECE, WIRED, BLUETOOTH, USB, HEARING_AID, REMOTE }

/** Where the music is coming out right now. */
data class AudioOutput(val kind: OutputKind, val name: String) {
    companion object {
        val Unknown = AudioOutput(OutputKind.SPEAKER, "Phone speaker")
    }
}

/**
 * Which output Android is actually using, and a way to change it.
 *
 * Deliberately not a device picker of our own. Routing on Android is not a list of speakers — it is
 * A2DP and LE Audio and hearing aids and USB DACs and cast targets, with priority rules that change
 * between versions and OEMs, and a picker that only knew about Bluetooth would be wrong in a way
 * that looks like a bug. So this reports the current route and hands the switching to the system
 * panel, which is the same sheet the volume keys and Quick Settings open.
 */
object AudioOutputs {

    /**
     * The system media output panel, from Android 10. Referenced as a string rather than through
     * Settings.Panel, which does not exist below API 29 and would need guarding just to name.
     */
    private const val ACTION_MEDIA_OUTPUT_PANEL = "android.settings.panel.action.MEDIA_OUTPUT"

    /**
     * The device audio is routed to, chosen by the same precedence the platform uses: anything
     * plugged in or paired wins over the built-in speaker, and the speaker is the floor.
     *
     * Read rather than subscribed-to, because [AudioDeviceInfo] carries no "is selected" flag below
     * API 31 and the ranking is what the platform does anyway.
     */
    fun current(context: Context): AudioOutput {
        val am = context.getSystemService<AudioManager>() ?: return AudioOutput.Unknown
        val devices = runCatching { am.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }
            .getOrNull()
            ?.toList()
            .orEmpty()

        val best = devices.maxByOrNull { rank(it.type) } ?: return AudioOutput.Unknown
        if (rank(best.type) == 0) return AudioOutput.Unknown
        return AudioOutput(kindOf(best.type), nameOf(best))
    }

    /**
     * Open the system's output switcher.
     *
     * The media output panel exists from Android 10; below that the only thing worth offering is
     * Bluetooth settings, since a wired headset needs no picking and there is nothing else to move
     * audio to. Falls back rather than throwing if an OEM build has neither.
     */
    fun openSwitcher(context: Context) {
        val intents = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(
                    Intent(ACTION_MEDIA_OUTPUT_PANEL).putExtra(
                        // Scopes the panel to our audio rather than whatever played last.
                        "com.android.settings.panel.extra.PACKAGE_NAME",
                        context.packageName,
                    ),
                )
            }
            add(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        for (intent in intents) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent); true }.getOrDefault(false)) return
        }
    }

    /** Higher wins. 0 means "not a real output", so an empty list falls back to the speaker. */
    private fun rank(type: Int): Int = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> 60
        AudioDeviceInfo.TYPE_HEARING_AID -> 60
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> 50
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> 40
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> 30
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 10
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> 5
        else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET) 60 else 0
    }

    private fun kindOf(type: Int): OutputKind = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> OutputKind.BLUETOOTH
        AudioDeviceInfo.TYPE_HEARING_AID -> OutputKind.HEARING_AID
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> OutputKind.USB
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> OutputKind.WIRED
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> OutputKind.EARPIECE
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> OutputKind.SPEAKER
        else ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET) {
                OutputKind.BLUETOOTH
            } else {
                OutputKind.REMOTE
            }
    }

    /**
     * The name to show. `productName` is the paired device's own name ("Pixel Buds Pro") for
     * anything external, and the phone's model for the built-in speaker — which is not what anyone
     * means by "where is this playing", so the built-ins get a plain word instead.
     */
    private fun nameOf(device: AudioDeviceInfo): String = when (device.type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Headphones"
        else -> device.productName?.toString()?.trim()?.takeIf { it.isNotBlank() } ?: "External"
    }
}

/**
 * The current output, kept up to date as things are plugged in and paired.
 *
 * [AudioDeviceCallback] fires on connect and disconnect, which is every moment the answer can
 * change without the user having been in the system panel — and the panel itself produces a
 * disconnect/connect pair too, so switching there updates this without any polling.
 */
@Composable
fun rememberAudioOutput(): State<AudioOutput> {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state = remember { mutableStateOf(AudioOutputs.current(context)) }

    DisposableEffect(context) {
        val am = context.getSystemService<AudioManager>()
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) {
                state.value = AudioOutputs.current(context)
            }

            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) {
                state.value = AudioOutputs.current(context)
            }
        }
        am?.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        state.value = AudioOutputs.current(context)
        onDispose { am?.unregisterAudioDeviceCallback(callback) }
    }

    return state
}
