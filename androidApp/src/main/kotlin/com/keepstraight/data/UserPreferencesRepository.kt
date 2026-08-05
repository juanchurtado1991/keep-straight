package com.keepstraight.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.model.SensitivityTimingLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(
    private val context: Context,
) : com.keepstraight.shared.repository.PreferencesRepository {
    private object Keys {
        val PAIRED_WATCH_ID = stringPreferencesKey("paired_watch_id")
        val PAIRED_AT = longPreferencesKey("paired_at")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val SENSITIVITY = stringPreferencesKey("sensitivity")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
        val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val VISUAL_ENABLED = booleanPreferencesKey("visual_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val PHONE_NOTIFICATION_ENABLED = booleanPreferencesKey("phone_notification_enabled")
        val BATTERY_OPT_DISMISSED = booleanPreferencesKey("battery_opt_dismissed")
        val CALIBRATION_PITCH = longPreferencesKey("calibration_pitch_bits")
        val CALIBRATION_ROLL = longPreferencesKey("calibration_roll_bits")
        val HAS_SLUMP_REF = booleanPreferencesKey("has_slump_reference")
        val SLUMP_PITCH = longPreferencesKey("slump_pitch_bits")
        val SLUMP_ROLL = longPreferencesKey("slump_roll_bits")
        val SLUMP_DURATION_MS = longPreferencesKey("slump_duration_threshold_ms")
        val REPEAT_ALERT_MS = longPreferencesKey("repeat_alert_interval_ms")
    }

    override val pairedWatchId: Flow<String?> = context.dataStore.data.map { it[Keys.PAIRED_WATCH_ID] }

    override val pairedAt: Flow<Long?> = context.dataStore.data.map { it[Keys.PAIRED_AT] }

    override val onboardingComplete: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.ONBOARDING_COMPLETE] ?: false
    }

    override val sensitivity: Flow<SensitivityLevel> = context.dataStore.data.map { prefs ->
        prefs[Keys.SENSITIVITY]?.let { runCatching { SensitivityLevel.valueOf(it) }.getOrNull() }
            ?: SensitivityLevel.NORMAL
    }

    override val monitoringEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.MONITORING_ENABLED] ?: true
    }

    override val alertsEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.ALERTS_ENABLED] ?: true
    }

    override val alertPreferences: Flow<AlertPreferences> = context.dataStore.data.map { prefs ->
        AlertPreferences(
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            visualEnabled = prefs[Keys.VISUAL_ENABLED] ?: true,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: false,
            phoneNotificationEnabled = prefs[Keys.PHONE_NOTIFICATION_ENABLED] ?: true,
        )
    }

    override val batteryOptimizationDismissed: Flow<Boolean> = context.dataStore.data.map {
        it[Keys.BATTERY_OPT_DISMISSED] ?: false
    }

    override val calibrationPitch: Flow<Float?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CALIBRATION_PITCH]?.let { java.lang.Float.intBitsToFloat(it.toInt()) }
    }

    override val calibrationRoll: Flow<Float?> = context.dataStore.data.map { prefs ->
        prefs[Keys.CALIBRATION_ROLL]?.let { java.lang.Float.intBitsToFloat(it.toInt()) }
    }

    override val hasSlumpReference: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_SLUMP_REF] ?: false
    }

    override val slumpReferencePitch: Flow<Float?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SLUMP_PITCH]?.let { java.lang.Float.intBitsToFloat(it.toInt()) }
    }

    override val slumpReferenceRoll: Flow<Float?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SLUMP_ROLL]?.let { java.lang.Float.intBitsToFloat(it.toInt()) }
    }

    override val slumpDurationThresholdMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.SLUMP_DURATION_MS] ?: DEFAULT_SLUMP_DURATION_MS
    }

    override val repeatAlertIntervalMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.REPEAT_ALERT_MS] ?: DEFAULT_REPEAT_ALERT_MS
    }

    override suspend fun setPairedWatchId(watchId: String?, pairedAtMs: Long?) {
        context.dataStore.edit { prefs ->
            if (watchId == null) {
                prefs.remove(Keys.PAIRED_WATCH_ID)
                prefs.remove(Keys.PAIRED_AT)
            } else {
                prefs[Keys.PAIRED_WATCH_ID] = watchId
                prefs[Keys.PAIRED_AT] = pairedAtMs ?: System.currentTimeMillis()
            }
        }
    }

    override suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    override suspend fun setSensitivity(level: SensitivityLevel) {
        context.dataStore.edit { it[Keys.SENSITIVITY] = level.name }
    }

    override suspend fun setMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
    }

    override suspend fun setAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ALERTS_ENABLED] = enabled }
    }

    override suspend fun setAlertPreferences(preferences: AlertPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTIC_ENABLED] = preferences.hapticEnabled
            prefs[Keys.VISUAL_ENABLED] = preferences.visualEnabled
            prefs[Keys.SOUND_ENABLED] = preferences.soundEnabled
            prefs[Keys.PHONE_NOTIFICATION_ENABLED] = preferences.phoneNotificationEnabled
        }
    }

    override suspend fun setBatteryOptimizationDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.BATTERY_OPT_DISMISSED] = dismissed }
    }

    override suspend fun setCalibration(pitch: Float, roll: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CALIBRATION_PITCH] = pitch.toRawBits().toLong()
            prefs[Keys.CALIBRATION_ROLL] = roll.toRawBits().toLong()
        }
    }

    override suspend fun setSlumpReference(pitch: Float, roll: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SLUMP_REF] = true
            prefs[Keys.SLUMP_PITCH] = pitch.toRawBits().toLong()
            prefs[Keys.SLUMP_ROLL] = roll.toRawBits().toLong()
        }
    }

    override suspend fun setSlumpTiming(slumpDurationThresholdMs: Long, repeatAlertIntervalMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SLUMP_DURATION_MS] = slumpDurationThresholdMs.coerceIn(MIN_SLUMP_DURATION_MS, MAX_SLUMP_DURATION_MS)
            prefs[Keys.REPEAT_ALERT_MS] = repeatAlertIntervalMs.coerceIn(MIN_REPEAT_ALERT_MS, MAX_REPEAT_ALERT_MS)
        }
    }

    companion object {
        const val DEFAULT_SLUMP_DURATION_MS = SensitivityTimingLimits.DEFAULT_SLUMP_DURATION_MS
        const val DEFAULT_REPEAT_ALERT_MS = SensitivityTimingLimits.DEFAULT_REPEAT_ALERT_MS
        const val MIN_SLUMP_DURATION_MS = SensitivityTimingLimits.MIN_SLUMP_DURATION_MS
        const val MAX_SLUMP_DURATION_MS = SensitivityTimingLimits.MAX_SLUMP_DURATION_MS
        const val MIN_REPEAT_ALERT_MS = SensitivityTimingLimits.MIN_REPEAT_ALERT_MS
        const val MAX_REPEAT_ALERT_MS = SensitivityTimingLimits.MAX_REPEAT_ALERT_MS
    }
}
