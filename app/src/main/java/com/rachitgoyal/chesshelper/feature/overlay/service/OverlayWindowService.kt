package com.rachitgoyal.chesshelper.feature.overlay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.rachitgoyal.chesshelper.MainActivity
import com.rachitgoyal.chesshelper.R

class OverlayWindowService : LifecycleService() {
    private var overlayWindowHost: OverlayWindowHost? = null

    override fun onCreate() {
        super.onCreate()
        overlayWindowHost = OverlayWindowHost(this) {
            stopOverlayAndSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_SHOW_OVERLAY

        if (!Settings.canDrawOverlays(this)) {
            stopOverlayAndSelf()
            return START_NOT_STICKY
        }

        return when (action) {
            ACTION_SHOW_OVERLAY -> {
                startForegroundCompat()
                overlayWindowHost?.show()
                START_STICKY
            }

            ACTION_HIDE_OVERLAY -> {
                stopOverlayAndSelf()
                START_NOT_STICKY
            }

            else -> START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        overlayWindowHost?.hide()
        overlayWindowHost = null
        OverlayWindowServiceState.isRunning = false
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        overlayWindowHost?.onConfigurationChanged()
    }

    private fun startForegroundCompat() {
        createNotificationChannelIfNeeded()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopOverlayIntent = PendingIntent.getService(
            this,
            1,
            createIntent(this, ACTION_HIDE_OVERLAY),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.overlay_notification_stop_action), stopOverlayIntent)
            .build()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun stopOverlayAndSelf() {
        overlayWindowHost?.hide()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.rachitgoyal.chesshelper.action.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.rachitgoyal.chesshelper.action.HIDE_OVERLAY"

        private const val CHANNEL_ID = "chess_overlay_window"
        private const val NOTIFICATION_ID = 7

        fun createIntent(context: android.content.Context, action: String): Intent {
            return Intent(context, OverlayWindowService::class.java).setAction(action)
        }

        fun overlaySettingsIntent(packageName: String): Intent {
            return Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"),
            )
        }
    }
}
