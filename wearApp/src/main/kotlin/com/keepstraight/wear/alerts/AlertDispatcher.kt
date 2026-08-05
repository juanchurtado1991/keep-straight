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

    /**
     * Urgent multi-burst waveform — long, max-amplitude pulses so a desk slump
     * is hard to ignore on the wrist.
     * timings: delay, on, off, on, off, on, off, on, off, on
     */
    private val insistentAlertEffect: VibrationEffect = VibrationEffect.createWaveform(
        longArrayOf(
            0,
            BURST_MS, GAP_MS,
            BURST_MS, GAP_MS,
            BURST_MS, LONG_GAP_MS,
            BURST_MS, GAP_MS,
            BURST_MS, GAP_MS,
            BURST_MS,
        ),
        intArrayOf(
            0,
            HAPTIC_AMPLITUDE, 0,
            HAPTIC_AMPLITUDE, 0,
            HAPTIC_AMPLITUDE, 0,
            HAPTIC_AMPLITUDE, 0,
            HAPTIC_AMPLITUDE, 0,
            HAPTIC_AMPLITUDE,
        ),
        -1,
    )

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    fun dispatchAlert(preferences: AlertPreferences) {
        if (isDndActive()) return

        if (!preferences.soundEnabled) {
            activeRingtone?.stop()
            activeRingtone = null
        }
        if (preferences.hapticEnabled) {
            playInsistentHaptic()
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

    private fun playInsistentHaptic() {
        vibrator.cancel()
        vibrator.vibrate(insistentAlertEffect)
    }

    private fun broadcastFlashOverlay() {
        val intent = Intent(ACTION_ALERT_FLASH).setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    private fun playAlertSound() {
        activeRingtone?.stop()
        activeRingtone = null

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ringtone.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        }
        activeRingtone = ringtone
        ringtone.play()
    }

    companion object {
        const val ACTION_ALERT_FLASH = "com.keepstraight.wear.ALERT_FLASH"
        private const val BURST_MS = 280L
        private const val GAP_MS = 90L
        private const val LONG_GAP_MS = 220L
        /** Max amplitude (0–255). */
        private const val HAPTIC_AMPLITUDE = 255
    }
}
