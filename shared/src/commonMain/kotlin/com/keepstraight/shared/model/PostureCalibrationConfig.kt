package com.keepstraight.shared.model

import com.ghost.serialization.annotations.GhostSerialization

@GhostSerialization
data class PostureCalibrationConfig(
    val basePitch: Float,
    val baseRoll: Float,
    val sensitivity: SensitivityLevel = SensitivityLevel.NORMAL,
    val slumpDurationThresholdMs: Long = 30_000L,
    val repeatAlertIntervalMs: Long = 5_000L,
    /** Second calibration pose: typical slouch while seated (wrist signature). */
    val hasSlumpReference: Boolean = false,
    val slumpPitch: Float = 0f,
    val slumpRoll: Float = 0f,
)
