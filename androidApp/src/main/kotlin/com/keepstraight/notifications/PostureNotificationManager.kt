package com.keepstraight.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.keepstraight.MainActivity
import com.keepstraight.R

class PostureNotificationManager(
    private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createChannels()
    }

    fun showSlumpAlert(durationSeconds: Int) {
        if (!notificationManager.areNotificationsEnabled()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        val durationText = when {
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_POSTURE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_slump_title))
            .setContentText(context.getString(R.string.notification_slump_body, durationText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_SLUMP, notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_POSTURE,
            context.getString(R.string.notification_channel_posture),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_posture_desc)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_POSTURE = "posture_alerts"
        private const val NOTIFICATION_SLUMP = 1001
    }
}
