package com.keepstraight.shared.presentation

enum class ReconnectError {
    NO_PAIRED_WATCH,
    WATCH_UNREACHABLE,
    SEND_FAILED,
    SEND_TIMEOUT,
}

sealed interface ReconnectUiState {
    data object Idle : ReconnectUiState
    data object InProgress : ReconnectUiState
    data object Success : ReconnectUiState
    data class Failed(val reason: ReconnectError) : ReconnectUiState
}
