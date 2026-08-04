package com.keepstraight.shared.repository

import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.SensitivityLevel
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val pairedWatchId: Flow<String?>
    val pairedAt: Flow<Long?>
    val onboardingComplete: Flow<Boolean>
    val sensitivity: Flow<SensitivityLevel>
    val monitoringEnabled: Flow<Boolean>
    val alertsEnabled: Flow<Boolean>
    val alertPreferences: Flow<AlertPreferences>
    val batteryOptimizationDismissed: Flow<Boolean>
    val calibrationPitch: Flow<Float?>
    val calibrationRoll: Flow<Float?>
    val hasSlumpReference: Flow<Boolean>
    val slumpReferencePitch: Flow<Float?>
    val slumpReferenceRoll: Flow<Float?>
    /** Milliseconds of sustained slump before the first alert (default 5 min). */
    val slumpDurationThresholdMs: Flow<Long>
    /** Milliseconds between repeat alerts after the first (default 5 s). */
    val repeatAlertIntervalMs: Flow<Long>

    suspend fun setPairedWatchId(watchId: String?, pairedAtMs: Long? = null)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setSensitivity(level: SensitivityLevel)
    suspend fun setMonitoringEnabled(enabled: Boolean)
    suspend fun setAlertsEnabled(enabled: Boolean)
    suspend fun setAlertPreferences(preferences: AlertPreferences)
    suspend fun setBatteryOptimizationDismissed(dismissed: Boolean)
    suspend fun setCalibration(pitch: Float, roll: Float)
    suspend fun setSlumpReference(pitch: Float, roll: Float)
    suspend fun setSlumpTiming(slumpDurationThresholdMs: Long, repeatAlertIntervalMs: Long)
}
