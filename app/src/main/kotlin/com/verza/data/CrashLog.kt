package com.verza.data

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Keeps the stack trace of the last crash so it can be read back on the next launch.
 *
 * A crash on someone else's phone is otherwise invisible: there is no logcat to read, and "it closed
 * itself" is not something anyone can act on. This is the same trick the desktop app uses, where JS
 * errors are mirrored into a log file for exactly the same reason.
 *
 * ponytail: one crash, overwritten each time. A ring of the last few would be better if the reports
 * ever turn out to be different crashes rather than the same one repeating.
 */
object CrashLog {

    private const val FILE_NAME = "crash.log"

    /**
     * Install the handler. Call from `Application.onCreate`.
     *
     * Chains to whatever was already registered — Android's default handler is what actually shows
     * the "app has stopped" dialog and kills the process, so dropping it would leave the app frozen
     * on a dead thread rather than closing.
     */
    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            // Written synchronously. The process is already going down; handing this to a coroutine
            // or an executor would mean racing the kill, and losing that race is the whole point of
            // failure here.
            runCatching { file(app).writeText(format(thread, error)) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** The last recorded crash, or null if there has not been one. */
    fun read(context: Context): String? =
        file(context.applicationContext).takeIf { it.exists() }?.runCatching { readText() }?.getOrNull()

    /** Forget it — after the user has sent it on. */
    fun clear(context: Context) {
        runCatching { file(context.applicationContext).delete() }
    }

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    private fun format(thread: Thread, error: Throwable): String {
        val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }
        val at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return buildString {
            appendLine("Verza ${com.verza.BuildConfig.VERSION_NAME} (${com.verza.BuildConfig.VERSION_CODE})")
            appendLine("Android ${android.os.Build.VERSION.RELEASE} · ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("$at · thread ${thread.name}")
            appendLine()
            append(stack)
        }
    }
}
