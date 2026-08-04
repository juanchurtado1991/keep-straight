package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
enum class WatchControlCommand {
    PAUSE_ALERTS,
    RESUME_ALERTS,
    STOP_ALGORITHM,
    START_ALGORITHM,
    CALIBRATE_CAPTURE,
    RESUME_CONNECTION,
    SYNC_PREFERENCES,
    /** Phase 2: phone asks watch to fire haptic after desktop slump event. */
    TRIGGER_ALERT,
}
