package com.keepstraight.shared.presentation.phone

import com.keepstraight.shared.domain.PostureScore
import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.presentation.CalibrationError
import com.keepstraight.shared.presentation.CalibrationPhase
import com.keepstraight.shared.presentation.CalibrationUiState

data class CalibrationReducerState(
    val ui: CalibrationUiState = CalibrationUiState.Idle,
    val goodPitch: Float? = null,
    val goodRoll: Float? = null,
)

sealed interface CalibrationReduceAction {
    data object Reset : CalibrationReduceAction

    data class Start(val connected: Boolean) : CalibrationReduceAction

    data class ContinueSlouch(val connected: Boolean) : CalibrationReduceAction

    data class TickCountdown(val seconds: Int, val phase: CalibrationPhase) : CalibrationReduceAction

    data class BeginCapture(val phase: CalibrationPhase) : CalibrationReduceAction

    data class CaptureFailed(val error: CalibrationError) : CalibrationReduceAction

    data class CaptureSucceeded(
        val result: CalibrationCaptureResult,
        val phase: CalibrationPhase,
    ) : CalibrationReduceAction

    data class ValidateSlouchCapture(val slouch: CalibrationCaptureResult) : CalibrationReduceAction

    data object PersistSucceeded : CalibrationReduceAction

    data class PersistFailed(val error: CalibrationError) : CalibrationReduceAction
}

object CalibrationReducer {
    fun reduce(
        state: CalibrationReducerState,
        action: CalibrationReduceAction,
    ): CalibrationReducerState = when (action) {
        CalibrationReduceAction.Reset -> CalibrationReducerState()

        is CalibrationReduceAction.Start -> {
            if (!action.connected) {
                state.copy(ui = CalibrationUiState.Error(CalibrationError.NOT_CONNECTED))
            } else {
                state.copy(
                    goodPitch = null,
                    goodRoll = null,
                    ui = CalibrationUiState.Countdown(COUNTDOWN_SECONDS, CalibrationPhase.GOOD),
                )
            }
        }

        is CalibrationReduceAction.ContinueSlouch -> when {
            !action.connected -> state.copy(ui = CalibrationUiState.Error(CalibrationError.NOT_CONNECTED))
            state.goodPitch == null || state.goodRoll == null ->
                state.copy(ui = CalibrationUiState.Error(CalibrationError.SAVE_FAILED))
            else -> state.copy(
                ui = CalibrationUiState.Countdown(COUNTDOWN_SECONDS, CalibrationPhase.SLOUCH),
            )
        }

        is CalibrationReduceAction.TickCountdown ->
            state.copy(ui = CalibrationUiState.Countdown(action.seconds, action.phase))

        is CalibrationReduceAction.BeginCapture ->
            state.copy(ui = CalibrationUiState.Capturing(action.phase))

        is CalibrationReduceAction.CaptureFailed ->
            state.copy(ui = CalibrationUiState.Error(action.error))

        is CalibrationReduceAction.CaptureSucceeded -> when (action.phase) {
            CalibrationPhase.GOOD -> state.copy(
                goodPitch = action.result.basePitch,
                goodRoll = action.result.baseRoll,
                ui = CalibrationUiState.PromptSlouch,
            )
            CalibrationPhase.SLOUCH -> state.copy(ui = CalibrationUiState.Capturing(CalibrationPhase.SLOUCH))
        }

        is CalibrationReduceAction.ValidateSlouchCapture -> {
            val goodP = state.goodPitch
            val goodR = state.goodRoll
            if (goodP == null || goodR == null) {
                state.copy(ui = CalibrationUiState.Error(CalibrationError.SAVE_FAILED))
            } else {
                val separation = PostureScore.distanceDeg(
                    goodP,
                    goodR,
                    action.slouch.basePitch,
                    action.slouch.baseRoll,
                )
                if (separation < PostureScore.MIN_REFERENCE_SEPARATION_DEG) {
                    state.copy(ui = CalibrationUiState.Error(CalibrationError.SLUMP_TOO_SIMILAR))
                } else {
                    state
                }
            }
        }

        CalibrationReduceAction.PersistSucceeded ->
            state.copy(
                goodPitch = null,
                goodRoll = null,
                ui = CalibrationUiState.Success,
            )

        is CalibrationReduceAction.PersistFailed ->
            state.copy(ui = CalibrationUiState.Error(action.error))
    }

    const val COUNTDOWN_SECONDS = 3
}
