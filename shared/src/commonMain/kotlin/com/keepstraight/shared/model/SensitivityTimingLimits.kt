package com.keepstraight.shared.model

object SensitivityTimingLimits {
    const val DEFAULT_SLUMP_DURATION_MS = 30_000L
    const val DEFAULT_REPEAT_ALERT_MS = 5_000L
    const val MIN_SLUMP_DURATION_MS = 5_000L
    const val MAX_SLUMP_DURATION_MS = 300_000L
    const val MIN_REPEAT_ALERT_MS = 2_000L
    const val MAX_REPEAT_ALERT_MS = 30_000L
}
