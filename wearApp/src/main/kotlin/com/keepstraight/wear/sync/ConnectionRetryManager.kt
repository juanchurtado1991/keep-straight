package com.keepstraight.wear.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.keepstraight.shared.sync.ConnectionRetryPolicy
import com.keepstraight.wear.KeepStraightWearApp

class ConnectionRetryManager(
    private val context: Context,
    private val onRetry: () -> Unit,
    private val onRetryExhausted: () -> Unit,
) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun startRetryCycle() {
        val startedAt = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_RETRY_STARTED_AT, startedAt)
            .putInt(KEY_RETRY_COUNT, 0)
            .apply()
        scheduleNextRetry()
    }

    fun cancelRetryCycle() {
        cancelRetryCycle(context)
    }

    fun handleRetryAlarm() {
        val startedAt = prefs.getLong(KEY_RETRY_STARTED_AT, 0L)
        if (startedAt == 0L) return

        val elapsed = System.currentTimeMillis() - startedAt
        if (elapsed >= ConnectionRetryPolicy.MAX_RETRY_DURATION_MS) {
            cancelRetryCycle()
            onRetryExhausted()
            return
        }

        val retryCount = prefs.getInt(KEY_RETRY_COUNT, 0) + 1
        prefs.edit().putInt(KEY_RETRY_COUNT, retryCount).apply()
        onRetry()
        scheduleNextRetry()
    }

    fun isRetryActive(): Boolean = isRetryActive(context)

    private fun scheduleNextRetry() {
        val triggerAt = SystemClock.elapsedRealtime() + ConnectionRetryPolicy.RETRY_INTERVAL_MS
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAt,
            retryPendingIntent(),
        )
    }

    private fun retryPendingIntent(): PendingIntent = retryPendingIntent(context)

    companion object {
        const val PREFS_NAME = "connection_retry"
        const val KEY_RETRY_STARTED_AT = "retry_started_at"
        const val KEY_RETRY_COUNT = "retry_count"
        private const val REQUEST_CODE = 1001

        fun isRetryActive(context: Context): Boolean =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .contains(KEY_RETRY_STARTED_AT)

        fun cancelRetryCycle(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            alarmManager.cancel(retryPendingIntent(context))
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .remove(KEY_RETRY_STARTED_AT)
                .remove(KEY_RETRY_COUNT)
                .apply()
        }

        private fun retryPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, ConnectionRetryReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

class ConnectionRetryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext as KeepStraightWearApp
        app.monitoringSession.handleConnectionRetry()
    }
}
