package com.keepstraight.shared.repository

import com.keepstraight.shared.model.AlertPreferences
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.model.WatchControlCommand
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface DeviceSyncGateway {
    val isConnected: Flow<Boolean>
    val calibrationResult: SharedFlow<CalibrationCaptureResult>

    suspend fun refreshConnectionStatus(): Boolean
    suspend fun discoverPairedDevices(): List<PairedDevice>
    suspend fun pairDevice(deviceId: String)
    suspend fun clearPairedDevice()
    suspend fun sendCalibration(config: PostureCalibrationConfig): Result<Unit>
    suspend fun sendControl(command: WatchControlCommand): Result<Unit>
    suspend fun sendPreferences(
        sensitivity: SensitivityLevel,
        alertPreferences: AlertPreferences,
    ): Result<Unit>
    suspend fun requestSync(): Result<Unit>
    suspend fun requestCalibrationCapture(): Result<Unit>
    /** Invalidates any in-flight capture waiters (e.g. user reset/back). */
    suspend fun cancelCalibrationCapture()
    /** Waits for the in-flight capture started by [requestCalibrationCapture]. */
    suspend fun awaitCalibrationResult(timeoutMs: Long): CalibrationCaptureResult?
    suspend fun reconnect(): Result<Unit>
    suspend fun syncAllPreferences(): Result<Unit>
}
