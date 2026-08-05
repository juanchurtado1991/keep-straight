package com.keepstraight.shared.application.phone

import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.presentation.CalibrationError
import com.keepstraight.shared.presentation.CalibrationPhase
import com.keepstraight.shared.presentation.CalibrationUiState
import com.keepstraight.shared.presentation.phone.CalibrationReduceAction
import com.keepstraight.shared.presentation.phone.CalibrationReducer
import com.keepstraight.shared.presentation.phone.CalibrationReducerState
import com.keepstraight.shared.repository.DeviceSyncGateway
import com.keepstraight.shared.repository.PreferencesRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

class CalibrationController(
    private val preferencesRepository: PreferencesRepository,
    private val deviceSyncGateway: DeviceSyncGateway,
) {
    private val _state = MutableStateFlow<CalibrationUiState>(CalibrationUiState.Idle)
    val state: StateFlow<CalibrationUiState> = _state.asStateFlow()

    val calibrationResult = deviceSyncGateway.calibrationResult

    private var reducerState = CalibrationReducerState()

    private fun apply(action: CalibrationReduceAction) {
        reducerState = CalibrationReducer.reduce(reducerState, action)
        _state.value = reducerState.ui
    }

    suspend fun start(isConnected: Boolean) {
        apply(CalibrationReduceAction.Start(isConnected))
        if (!isConnected) return
        runCapturePhase(CalibrationPhase.GOOD)
    }

    /** After good pose is captured, user confirms they are slouching. */
    suspend fun continueWithSlouch(isConnected: Boolean) {
        apply(CalibrationReduceAction.ContinueSlouch(isConnected))
        if (!isConnected) return
        if (reducerState.goodPitch == null || reducerState.goodRoll == null) return
        runCapturePhase(CalibrationPhase.SLOUCH)
    }

    fun reset() {
        apply(CalibrationReduceAction.Reset)
    }

    private suspend fun runCapturePhase(phase: CalibrationPhase) {
        for (seconds in CalibrationReducer.COUNTDOWN_SECONDS downTo 1) {
            apply(CalibrationReduceAction.TickCountdown(seconds, phase))
            delay(COUNTDOWN_STEP_MS.milliseconds)
        }

        apply(CalibrationReduceAction.BeginCapture(phase))

        val sendResult = withTimeoutOrNull(SEND_TIMEOUT_MS.milliseconds) {
            deviceSyncGateway.requestCalibrationCapture()
        }
        when {
            sendResult == null -> {
                apply(CalibrationReduceAction.CaptureFailed(CalibrationError.SEND_TIMEOUT))
                return
            }
            sendResult.isFailure -> {
                apply(CalibrationReduceAction.CaptureFailed(CalibrationError.SEND_FAILED))
                return
            }
        }

        val result = deviceSyncGateway.awaitCalibrationResult(CAPTURE_TIMEOUT_MS)
        if (result == null) {
            apply(CalibrationReduceAction.CaptureFailed(CalibrationError.WATCH_NO_RESPONSE))
            return
        }

        when (phase) {
            CalibrationPhase.GOOD -> apply(
                CalibrationReduceAction.CaptureSucceeded(result, CalibrationPhase.GOOD),
            )
            CalibrationPhase.SLOUCH -> persistBothPoses(result)
        }
    }

    private suspend fun persistBothPoses(slouch: CalibrationCaptureResult) {
        apply(CalibrationReduceAction.ValidateSlouchCapture(slouch))
        if (reducerState.ui is CalibrationUiState.Error) return

        val goodP = reducerState.goodPitch
        val goodR = reducerState.goodRoll
        if (goodP == null || goodR == null) {
            apply(CalibrationReduceAction.PersistFailed(CalibrationError.SAVE_FAILED))
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
            apply(CalibrationReduceAction.PersistSucceeded)
        } catch (_: Exception) {
            apply(CalibrationReduceAction.PersistFailed(CalibrationError.SAVE_FAILED))
        }
    }

    private companion object {
        const val COUNTDOWN_STEP_MS = 1_000L
        const val SEND_TIMEOUT_MS = 8_000L
        const val CAPTURE_TIMEOUT_MS = 20_000L
    }
}
