package com.keepstraight.shared.presentation

enum class CalibrationError {
    NOT_CONNECTED,
    SEND_FAILED,
    SEND_TIMEOUT,
    WATCH_NO_RESPONSE,
    SAVE_FAILED,
    SLUMP_TOO_SIMILAR,
}

enum class CalibrationPhase {
    GOOD,
    SLOUCH,
}

sealed interface CalibrationUiState {
    data object Idle : CalibrationUiState
    data object PromptSlouch : CalibrationUiState
    data class Countdown(val seconds: Int, val phase: CalibrationPhase) : CalibrationUiState
    data class Capturing(val phase: CalibrationPhase) : CalibrationUiState
    data object Success : CalibrationUiState
    data class Error(val reason: CalibrationError) : CalibrationUiState
}
