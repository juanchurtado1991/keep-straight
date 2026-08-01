package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
enum class PostureEventType {
    SLUMP_DETECTED,
    CALIBRATED,
    MONITORING_PAUSED,
    MONITORING_RESUMED,
}
