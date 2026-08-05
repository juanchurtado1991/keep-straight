package com.keepstraight.shared.presentation.phone

sealed interface CalibrationEvent {
    data object Start : CalibrationEvent
    data object ContinueSlouch : CalibrationEvent
    data object Reset : CalibrationEvent
    data object SuccessAcknowledged : CalibrationEvent
}

sealed interface CalibrationEffect {
    data object NavigateBack : CalibrationEffect
}
