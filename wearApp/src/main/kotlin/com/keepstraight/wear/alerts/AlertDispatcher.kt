package com.keepstraight.wear.alerts

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.keepstraight.shared.model.AlertPreferences

class AlertDispatcher(private val context: Context) {

    private var activeRingtone: android.media.Ringtone? = null

    fun dispatchAlert(preferences: AlertPreferences) {
        if (isDndActive()) return

        if (preferences.hapticEnabled) {
            playDoublePulseHaptic()
        }
        if (preferences.visualEnabled) {
            broadcastFlashOverlay()
        }
        if (preferences.soundEnabled) {
            playAlertSound()
        }
    }

    private fun isDndActive(): Boolean {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        return notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }

    private fun playDoublePulseHaptic() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

        val timings = longArrayOf(0, PULSE_MS, GAP_MS, PULSE_MS)
        val amplitudes = intArrayOf(0, 255, 0, 255)
        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
    }

    private fun broadcastFlashOverlay() {
        val intent = Intent(ACTION_ALERT_FLASH).setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    private fun playAlertSound() {
        activeRingtone?.stop()
        activeRingtone = null

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }
        activeRingtone = ringtone
        ringtone.play()
    }

    companion object {
        const val ACTION_ALERT_FLASH = "com.keepstraight.wear.ALERT_FLASH"
        private const val PULSE_MS = 120L
        private const val GAP_MS = 80L
    }
}
