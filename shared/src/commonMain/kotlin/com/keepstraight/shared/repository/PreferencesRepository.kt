package com.keepstraight.shared.repository

import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.SensitivityLevel
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val pairedWatchId: Flow<String?>
    val onboardingComplete: Flow<Boolean>
    val sensitivity: Flow<SensitivityLevel>
    val monitoringEnabled: Flow<Boolean>
    val alertsEnabled: Flow<Boolean>
    val alertPreferences: Flow<AlertPreferences>
    val batteryOptimizationDismissed: Flow<Boolean>
    val calibrationPitch: Flow<Float?>
    val calibrationRoll: Flow<Float?>

    suspend fun setPairedWatchId(watchId: String?)
    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setSensitivity(level: SensitivityLevel)
    suspend fun setMonitoringEnabled(enabled: Boolean)
    suspend fun setAlertsEnabled(enabled: Boolean)
    suspend fun setAlertPreferences(preferences: AlertPreferences)
    suspend fun setBatteryOptimizationDismissed(dismissed: Boolean)
    suspend fun setCalibration(pitch: Float, roll: Float)
}
