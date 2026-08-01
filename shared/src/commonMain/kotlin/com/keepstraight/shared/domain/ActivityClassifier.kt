package com.keepstraight.shared.domain

import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.util.FixedSampleBuffer
import kotlin.math.abs

class ActivityClassifier(
    private var config: PostureCalibrationConfig?,
) {
    private var stepBaseline: Int = -1
    private var lastStepSampleTimeMs: Long = 0L
    private var stepsInWindow: Int = 0

    private var standingCandidateStartMs: Long = -1L

    private val pitchBuffer = FixedSampleBuffer(5)
    private val rollBuffer = FixedSampleBuffer(5)

    private var isOffWrist: Boolean = false

    fun updateConfig(newConfig: PostureCalibrationConfig) {
        config = newConfig
        reset()
    }

    fun setOffWrist(offWrist: Boolean) {
        isOffWrist = offWrist
        if (offWrist) {
            standingCandidateStartMs = -1L
        }
    }

    fun classify(
        pitch: Float,
        roll: Float,
        stepCount: Int,
        currentTimeMs: Long,
    ): ActivityState {
        if (isOffWrist) return ActivityState.NOT_WORN

        val calibration = config ?: return ActivityState.AMBIGUOUS

        if (stepCount < stepBaseline) {
            stepBaseline = stepCount
        }

        if (stepBaseline < 0) {
            stepBaseline = stepCount
            lastStepSampleTimeMs = currentTimeMs
        }

        if (currentTimeMs - lastStepSampleTimeMs >= STEP_WINDOW_MS) {
            stepsInWindow = (stepCount - stepBaseline).coerceAtLeast(0)
            stepBaseline = stepCount
            lastStepSampleTimeMs = currentTimeMs
        }

        pitchBuffer.add(pitch)
        rollBuffer.add(roll)
        val smoothPitch = pitchBuffer.average()
        val smoothRoll = rollBuffer.average()

        if (stepsInWindow >= WALK_STEP_THRESHOLD) {
            standingCandidateStartMs = -1L
            return ActivityState.WALKING
        }

        val pitchDelta = angleDelta(smoothPitch, calibration.basePitch)
        val rollDelta = angleDelta(smoothRoll, calibration.baseRoll)
        val standingPitchDelta = calibration.sensitivity.standingPitchDelta()
        val standingRollDelta = calibration.sensitivity.standingRollDelta()

        val looksStanding = pitchDelta > standingPitchDelta || rollDelta > standingRollDelta
        return if (looksStanding) {
            if (standingCandidateStartMs < 0L) {
                standingCandidateStartMs = currentTimeMs
            }
            if (currentTimeMs - standingCandidateStartMs >= STANDING_HOLD_MS) {
                ActivityState.STANDING
            } else {
                ActivityState.AMBIGUOUS
            }
        } else {
            standingCandidateStartMs = -1L
            ActivityState.SITTING
        }
    }

    fun reset() {
        stepBaseline = -1
        lastStepSampleTimeMs = 0L
        stepsInWindow = 0
        standingCandidateStartMs = -1L
        pitchBuffer.clear()
        rollBuffer.clear()
    }

    private fun angleDelta(current: Float, baseline: Float): Float {
        val delta = abs(current - baseline)
        return minOf(delta, 360f - delta)
    }

    private companion object {
        const val STEP_WINDOW_MS = 10_000L
        const val WALK_STEP_THRESHOLD = 3
        const val STANDING_HOLD_MS = 30_000L
    }
}

private fun SensitivityLevel.standingPitchDelta(): Float = when (this) {
    SensitivityLevel.STRICT -> 15f
    SensitivityLevel.NORMAL -> 18f
    SensitivityLevel.RELAXED -> 22f
}

private fun SensitivityLevel.standingRollDelta(): Float = when (this) {
    SensitivityLevel.STRICT -> 10f
    SensitivityLevel.NORMAL -> 12f
    SensitivityLevel.RELAXED -> 15f
}
