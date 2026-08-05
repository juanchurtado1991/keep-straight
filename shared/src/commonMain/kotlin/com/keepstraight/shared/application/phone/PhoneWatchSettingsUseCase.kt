package com.keepstraight.shared.usecase.phone

import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.model.WatchControlCommand
import com.keepstraight.shared.repository.DeviceSyncGateway
import com.keepstraight.shared.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

class PhoneWatchSettingsUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val deviceSyncGateway: DeviceSyncGateway,
) {
    suspend fun setSensitivity(level: SensitivityLevel) {
        preferencesRepository.setSensitivity(level)
        val alertPrefs = preferencesRepository.alertPreferences.first()
        deviceSyncGateway.sendPreferences(level, alertPrefs)
    }

    suspend fun setSlumpTiming(slumpDurationThresholdMs: Long, repeatAlertIntervalMs: Long) {
        preferencesRepository.setSlumpTiming(slumpDurationThresholdMs, repeatAlertIntervalMs)
        val sensitivity = preferencesRepository.sensitivity.first()
        val alertPrefs = preferencesRepository.alertPreferences.first()
        deviceSyncGateway.sendPreferences(sensitivity, alertPrefs)
    }

    suspend fun setMonitoringEnabled(enabled: Boolean) {
        preferencesRepository.setMonitoringEnabled(enabled)
        val command = if (enabled) {
            WatchControlCommand.START_ALGORITHM
        } else {
            WatchControlCommand.STOP_ALGORITHM
        }
        deviceSyncGateway.sendControl(command)
    }

    suspend fun setAlertsEnabled(enabled: Boolean) {
        preferencesRepository.setAlertsEnabled(enabled)
        val command = if (enabled) {
            WatchControlCommand.RESUME_ALERTS
        } else {
            WatchControlCommand.PAUSE_ALERTS
        }
        deviceSyncGateway.sendControl(command)
    }

    suspend fun updateAlertPreferences(preferences: AlertPreferences) {
        preferencesRepository.setAlertPreferences(preferences)
        val sensitivity = preferencesRepository.sensitivity.first()
        deviceSyncGateway.sendPreferences(sensitivity, preferences)
    }

    suspend fun reconnectWatch() {
        deviceSyncGateway.reconnect()
    }

    suspend fun requestSync() {
        deviceSyncGateway.requestSync()
    }

    suspend fun dismissBatteryOptimizationBanner() {
        preferencesRepository.setBatteryOptimizationDismissed(true)
    }
}
