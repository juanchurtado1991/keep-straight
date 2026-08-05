package com.keepstraight.shared.presentation.phone

import com.keepstraight.shared.model.CalibrationCaptureResult
import com.keepstraight.shared.presentation.CalibrationError
import com.keepstraight.shared.presentation.CalibrationPhase
import com.keepstraight.shared.presentation.CalibrationUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CalibrationReducerTest {

    @Test
    fun startWhenDisconnected_setsNotConnectedError() {
        val next = CalibrationReducer.reduce(
            CalibrationReducerState(),
            CalibrationReduceAction.Start(connected = false),
        )
        assertEquals(CalibrationUiState.Error(CalibrationError.NOT_CONNECTED), next.ui)
    }

    @Test
    fun startWhenConnected_beginsGoodCountdown() {
        val next = CalibrationReducer.reduce(
            CalibrationReducerState(),
            CalibrationReduceAction.Start(connected = true),
        )
        assertEquals(
            CalibrationUiState.Countdown(CalibrationReducer.COUNTDOWN_SECONDS, CalibrationPhase.GOOD),
            next.ui,
        )
        assertNull(next.goodPitch)
    }

    @Test
    fun goodCapture_movesToPromptSlouch() {
        val result = CalibrationCaptureResult(basePitch = 10f, baseRoll = 2f, capturedAt = 0L)
        val next = CalibrationReducer.reduce(
            CalibrationReducerState(ui = CalibrationUiState.Capturing(CalibrationPhase.GOOD)),
            CalibrationReduceAction.CaptureSucceeded(result, CalibrationPhase.GOOD),
        )
        assertEquals(CalibrationUiState.PromptSlouch, next.ui)
        assertEquals(10f, next.goodPitch)
        assertEquals(2f, next.goodRoll)
    }

    @Test
    fun slouchTooSimilar_setsError() {
        val state = CalibrationReducerState(
            ui = CalibrationUiState.Capturing(CalibrationPhase.SLOUCH),
            goodPitch = 10f,
            goodRoll = 2f,
        )
        val slouch = CalibrationCaptureResult(basePitch = 10.5f, baseRoll = 2.2f, capturedAt = 0L)
        val next = CalibrationReducer.reduce(
            state,
            CalibrationReduceAction.ValidateSlouchCapture(slouch),
        )
        assertEquals(CalibrationUiState.Error(CalibrationError.SLUMP_TOO_SIMILAR), next.ui)
    }

    @Test
    fun persistSucceeded_clearsReferences() {
        val next = CalibrationReducer.reduce(
            CalibrationReducerState(
                ui = CalibrationUiState.Capturing(CalibrationPhase.SLOUCH),
                goodPitch = 10f,
                goodRoll = 2f,
            ),
            CalibrationReduceAction.PersistSucceeded,
        )
        assertEquals(CalibrationUiState.Success, next.ui)
        assertNull(next.goodPitch)
        assertNull(next.goodRoll)
    }
}
