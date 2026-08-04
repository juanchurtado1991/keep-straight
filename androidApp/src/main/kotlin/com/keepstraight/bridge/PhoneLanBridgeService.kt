package com.keepstraight.bridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.keepstraight.KeepStraightApp
import com.keepstraight.MainActivity
import com.keepstraight.R

/**
 * Keeps [PhoneLanIngestServer] alive after the UI is closed so desktop can still
 * push slump events and poll settings.
 */
class PhoneLanBridgeService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = buildNotification()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )

        (application as KeepStraightApp).lanIngestServer.start()
        return START_STICKY
    }

    override fun onDestroy() {
        (application as? KeepStraightApp)?.lanIngestServer?.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_desktop_bridge),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desktop_bridge_desc)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val paired = (application as KeepStraightApp).lanIngestServer.isPairedWithDesktop()
        val body = if (paired) {
            getString(R.string.notification_desktop_bridge_paired)
        } else {
            getString(R.string.notification_desktop_bridge_ready)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_desktop_bridge_title))
            .setContentText(body)
            .setContentIntent(launchIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "desktop_bridge"
        private const val NOTIFICATION_ID = 2001
        private const val TAG = "PhoneLanBridge"

        fun start(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, PhoneLanBridgeService::class.java)
            try {
                appContext.startForegroundService(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start desktop bridge FGS: ${e.message}")
            }
        }

        fun stop(context: Context) {
            context.applicationContext.stopService(
                Intent(context.applicationContext, PhoneLanBridgeService::class.java),
            )
        }

        /** Restart after reboot only when a desktop token already exists. */
        fun startIfPaired(context: Context) {
            val prefs = context.applicationContext
                .getSharedPreferences("desktop_bridge", Context.MODE_PRIVATE)
            if (!prefs.getString("token", null).isNullOrBlank()) {
                start(context)
            }
        }
    }
}
