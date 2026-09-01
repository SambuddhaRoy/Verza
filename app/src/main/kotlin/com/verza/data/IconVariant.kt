package com.verza.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import com.verza.ui.expressive.hsv
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min

/**
 * The launcher icon, following the app's colour.
 *
 * Android bakes the launcher icon into the manifest — there is no API to tint it at runtime. The
 * only supported way to change it is to declare several `<activity-alias>` entries, each with its
 * own icon, and enable exactly one. So the cover's hue is quantised into eight buckets and there is
 * one alias per bucket.
 *
 * That mechanism has visible costs, which is why this is opt-in rather than on by default: switching
 * aliases makes the icon briefly disappear and reappear, some launchers announce it, and a few drop
 * the shortcut from the home screen entirely. Changing it on every track would be unusable, so the
 * switch only happens when the hue moves into a different bucket, and at most once every few minutes.
 */
@Singleton
class IconVariant @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Bucket centres, in degrees. Index matches [ALIASES]. */
    private val hues = floatArrayOf(0f, 40f, 80f, 140f, 180f, 220f, 265f, 300f)

    /** The default entry. Enabled whenever the option is off, so something always launches. */
    private val defaultAlias = "com.verza.IconDefault"

    private val aliases = listOf(
        "com.verza.IconRed", "com.verza.IconAmber", "com.verza.IconLime", "com.verza.IconGreen",
        "com.verza.IconTeal", "com.verza.IconBlue", "com.verza.IconIndigo", "com.verza.IconViolet",
    )

    /**
     * Which bucket is live right now, or -1 for the default.
     *
     * Read back off the system rather than remembered in the composition. The caller used to hold
     * it in `rememberSaveable`, which is empty after the process is killed — so the first cover of
     * every cold start looked like a change and re-applied an icon that was already correct.
     */
    suspend fun currentBucket(): Int = withContext(Dispatchers.IO) {
        runCatching {
            val pm = context.packageManager
            aliases.indexOfFirst {
                pm.getComponentEnabledSetting(ComponentName(context.packageName, it)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
        }.getOrDefault(-1)
    }

    /** Which bucket a colour falls into, by shortest distance around the wheel. */
    fun bucketFor(accent: Color): Int {
        val h = hsv(accent)[0]
        var best = 0
        var bestDist = Float.MAX_VALUE
        for (i in hues.indices) {
            val d = abs(h - hues[i]).let { min(it, 360f - it) }
            if (d < bestDist) { bestDist = d; best = i }
        }
        return best
    }

    /**
     * Enable the alias for [bucket] and disable the rest.
     *
     * DONT_KILL_APP matters: without it Android restarts the process the moment the component state
     * changes, which from the listener's side looks like the app crashing while music is playing.
     */
    suspend fun apply(bucket: Int) = withContext(Dispatchers.IO) {
        runCatching {
            val pm = context.packageManager
            // Enable first, disable second — the same order reset() uses, and for the same reason.
            // Disabling the default up front left a window with no enabled launcher component at
            // all, and launchers that re-read the component list during it drop the app from the
            // home screen for good.
            aliases.getOrNull(bucket)?.let {
                setState(pm, it, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            } ?: return@runCatching
            setState(pm, defaultAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
            aliases.forEachIndexed { i, alias ->
                if (i != bucket) {
                    setState(pm, alias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
                }
            }
        }
    }

    /** Put the icon back to the default and disable every variant alias. */
    suspend fun reset() = withContext(Dispatchers.IO) {
        runCatching {
            val pm = context.packageManager
            // Enable the default first: if every alias were disabled at once the app would have no
            // launcher entry at all, and it would vanish from the home screen.
            setState(pm, defaultAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
            aliases.forEach { setState(pm, it, PackageManager.COMPONENT_ENABLED_STATE_DISABLED) }
        }
    }

    private fun setState(pm: PackageManager, alias: String, state: Int) {
        val component = ComponentName(context.packageName, alias)
        if (pm.getComponentEnabledSetting(component) != state) {
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }
    }

    companion object {
        /** Minimum gap between icon switches. A launcher icon that flickers per track is a bug. */
        const val MIN_INTERVAL_MS = 3 * 60 * 1000L
    }
}
