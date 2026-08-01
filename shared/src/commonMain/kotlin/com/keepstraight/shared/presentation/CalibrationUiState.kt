package com.keepstraight.shared.presentation

sealed interface CalibrationUiState {
    data object Idle : CalibrationUiState
    data class Countdown(val seconds: Int) : CalibrationUiState
    data object Capturing : CalibrationUiState
    data object Success : CalibrationUiState
    data object Error : CalibrationUiState
}
