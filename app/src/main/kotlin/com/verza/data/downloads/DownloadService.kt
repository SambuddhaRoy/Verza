package com.verza.data.downloads

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Keeps the process in the foreground while [DownloadManager] has anything in flight. A Soulseek
 * download can sit in a queue for a minute, and a cached app has its network cut.
 *
 * Stopped by an intent rather than stopService: stopping before onStartCommand has called
 * startForeground crashes the app, and intents arrive in order.
 */
class DownloadService : Service() {

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW))
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading music")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        // Refused from the background on 12+ (a batch that outlived the app's last screen): carry on without
        val foreground = runCatching { ServiceCompat.startForeground(this, ID, notification, type) }.isSuccess
        if (!foreground || intent?.action == STOP) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    /** Android 15 allows dataSync six hours a day. */
    override fun onTimeout(startId: Int, fgsType: Int) = stopSelf()

    companion object {
        private const val CHANNEL = "downloads"
        private const val ID = 7301
        private const val STOP = "stop"

        fun set(context: Context, running: Boolean) {
            val intent = Intent(context, DownloadService::class.java)
            runCatching {
                // A running foreground service counts as foreground, so the stop is allowed from the background
                if (running) ContextCompat.startForegroundService(context, intent)
                else context.startService(intent.setAction(STOP))
            }
        }
    }
}
