package com.keepstraight.shared.usecase.phone

import com.keepstraight.shared.domain.PostureScore
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.presentation.CalibrationError
import com.keepstraight.shared.presentation.CalibrationPhase
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

    private var goodPitch: Float? = null
    private var goodRoll: Float? = null

    suspend fun start(isConnected: Boolean) {
        goodPitch = null
        goodRoll = null
        if (!isConnected) {
            _state.value = CalibrationUiState.Error(CalibrationError.NOT_CONNECTED)
            return
        }
        runCapturePhase(CalibrationPhase.GOOD)
    }

    /** After good pose is captured, user confirms they are slouching. */
    suspend fun continueWithSlouch(isConnected: Boolean) {
        if (!isConnected) {
            _state.value = CalibrationUiState.Error(CalibrationError.NOT_CONNECTED)
            return
        }
        if (goodPitch == null || goodRoll == null) {
            _state.value = CalibrationUiState.Error(CalibrationError.SAVE_FAILED)
            return
        }
        runCapturePhase(CalibrationPhase.SLOUCH)
    }

    fun reset() {
        goodPitch = null
        goodRoll = null
        _state.value = CalibrationUiState.Idle
    }

    private suspend fun runCapturePhase(phase: CalibrationPhase) {
        for (seconds in COUNTDOWN_SECONDS downTo 1) {
            _state.value = CalibrationUiState.Countdown(seconds, phase)
            delay(COUNTDOWN_STEP_MS)
        }

        _state.value = CalibrationUiState.Capturing(phase)

        val sendResult = withTimeoutOrNull(SEND_TIMEOUT_MS) {
            deviceSyncGateway.requestCalibrationCapture()
        }
        when {
            sendResult == null -> {
                _state.value = CalibrationUiState.Error(CalibrationError.SEND_TIMEOUT)
                return
            }
            sendResult.isFailure -> {
                _state.value = CalibrationUiState.Error(CalibrationError.SEND_FAILED)
                return
            }
        }

        val result = deviceSyncGateway.awaitCalibrationResult(CAPTURE_TIMEOUT_MS)
        if (result == null) {
            _state.value = CalibrationUiState.Error(CalibrationError.WATCH_NO_RESPONSE)
            return
        }

        when (phase) {
            CalibrationPhase.GOOD -> {
                goodPitch = result.basePitch
                goodRoll = result.baseRoll
                _state.value = CalibrationUiState.PromptSlouch
            }
            CalibrationPhase.SLOUCH -> persistBothPoses(result)
        }
    }

    private suspend fun persistBothPoses(slouch: CalibrationCaptureResult) {
        val goodP = goodPitch
        val goodR = goodRoll
        if (goodP == null || goodR == null) {
            _state.value = CalibrationUiState.Error(CalibrationError.SAVE_FAILED)
            return
        }

        val separation = PostureScore.distanceDeg(
            goodP,
            goodR,
            slouch.basePitch,
            slouch.baseRoll,
        )
        if (separation < PostureScore.MIN_REFERENCE_SEPARATION_DEG) {
            _state.value = CalibrationUiState.Error(CalibrationError.SLUMP_TOO_SIMILAR)
            return
        }

        try {
            preferencesRepository.setCalibration(goodP, goodR)
            preferencesRepository.setSlumpReference(slouch.basePitch, slouch.baseRoll)
            val sensitivity = preferencesRepository.sensitivity.first()
            deviceSyncGateway.sendCalibration(
                PostureCalibrationConfig(
                    basePitch = goodP,
                    baseRoll = goodR,
                    sensitivity = sensitivity,
                    slumpDurationThresholdMs = preferencesRepository.slumpDurationThresholdMs.first(),
                    repeatAlertIntervalMs = preferencesRepository.repeatAlertIntervalMs.first(),
                    hasSlumpReference = true,
                    slumpPitch = slouch.basePitch,
                    slumpRoll = slouch.baseRoll,
                ),
            )
            goodPitch = null
            goodRoll = null
            _state.value = CalibrationUiState.Success
        } catch (_: Exception) {
            _state.value = CalibrationUiState.Error(CalibrationError.SAVE_FAILED)
        }
    }

    private companion object {
        const val COUNTDOWN_SECONDS = 3
        const val COUNTDOWN_STEP_MS = 1_000L
        const val SEND_TIMEOUT_MS = 8_000L
        const val CAPTURE_TIMEOUT_MS = 20_000L
    }
}
