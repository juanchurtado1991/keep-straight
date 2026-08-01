package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
data class PostureCalibrationConfig(
    val basePitch: Float,
    val baseRoll: Float,
    val sensitivity: SensitivityLevel = SensitivityLevel.NORMAL,
    val slumpDurationThresholdMs: Long = 300_000L,
    val repeatAlertIntervalMs: Long = 5_000L,
)
