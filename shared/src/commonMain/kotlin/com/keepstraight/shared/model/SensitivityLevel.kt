package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
enum class SensitivityLevel {
    STRICT,
    NORMAL,
    RELAXED,
}
