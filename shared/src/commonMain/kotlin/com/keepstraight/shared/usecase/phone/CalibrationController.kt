package com.keepstraight.shared.usecase.phone

import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.presentation.CalibrationUiState
import com.keepstraight.shared.repository.DeviceSyncGateway
import com.keepstraight.shared.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class CalibrationController(
    private val preferencesRepository: PreferencesRepository,
    private val deviceSyncGateway: DeviceSyncGateway,
) {
    private val _state = MutableStateFlow<CalibrationUiState>(CalibrationUiState.Idle)
    val state: StateFlow<CalibrationUiState> = _state.asStateFlow()

    val calibrationResult = deviceSyncGateway.calibrationResult

    suspend fun start(isConnected: Boolean) {
        if (!isConnected) {
            _state.value = CalibrationUiState.Error
            return
        }

        _state.value = CalibrationUiState.Countdown(COUNTDOWN_SECONDS)
        for (seconds in COUNTDOWN_SECONDS downTo 1) {
            _state.value = CalibrationUiState.Countdown(seconds)
            delay(COUNTDOWN_STEP_MS)
        }

        _state.value = CalibrationUiState.Capturing
        val captureResult = withTimeoutOrNull(CALIBRATION_TIMEOUT_MS) {
            deviceSyncGateway.requestCalibrationCapture()
        }
        if (captureResult == null || captureResult.isFailure) {
            _state.value = CalibrationUiState.Error
        }
    }

    suspend fun onResult(result: CalibrationCaptureResult) {
        preferencesRepository.setCalibration(result.basePitch, result.baseRoll)
        val sensitivity = preferencesRepository.sensitivity.first()
        deviceSyncGateway.sendCalibration(
            PostureCalibrationConfig(
                basePitch = result.basePitch,
                baseRoll = result.baseRoll,
                sensitivity = sensitivity,
            ),
        )
        _state.value = CalibrationUiState.Success
    }

    fun reset() {
        _state.value = CalibrationUiState.Idle
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 3
        const val COUNTDOWN_STEP_MS = 1_000L
        const val CALIBRATION_TIMEOUT_MS = 15_000L
    }
}
