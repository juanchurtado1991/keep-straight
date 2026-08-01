package com.keepstraight.shared.domain

enum class ActivityState {
    SITTING,
    WALKING,
    STANDING,
    NOT_WORN,
    AMBIGUOUS,
}

enum class AnalyzerResult {
    NONE,
    SLUMP_INITIAL_ALERT,
    SLUMP_REPEAT_ALERT,
    POSTURE_CORRECTED,
    STATE_RESET,
}
