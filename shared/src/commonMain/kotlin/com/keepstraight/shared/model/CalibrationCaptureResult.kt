package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
data class CalibrationCaptureResult(
    val basePitch: Float,
    val baseRoll: Float,
    val capturedAt: Long,
)
