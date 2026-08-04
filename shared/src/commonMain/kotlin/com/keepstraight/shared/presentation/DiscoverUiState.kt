package com.keepstraight.shared.presentation

import com.keepstraight.shared.repository.PairedDevice

enum class DiscoverError {
    TIMEOUT,
    FAILED,
}

sealed interface DiscoverUiState {
    data object Idle : DiscoverUiState
    data object Loading : DiscoverUiState
    data class Ready(val nodes: List<PairedDevice>) : DiscoverUiState
    data class Failed(val reason: DiscoverError) : DiscoverUiState
}
