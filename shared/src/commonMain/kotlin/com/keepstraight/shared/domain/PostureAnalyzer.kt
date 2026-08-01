package com.keepstraight.shared.domain

import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import kotlin.math.abs

object SensitivityTolerances {
    fun slumpTolerance(level: SensitivityLevel): Float = when (level) {
        SensitivityLevel.STRICT -> 7f
        SensitivityLevel.NORMAL -> 10f
        SensitivityLevel.RELAXED -> 15f
    }
}

class PostureAnalyzer(
    private var config: PostureCalibrationConfig?,
) {
    private var slumpStartTimeMs: Long = -1L
    private var lastAlertTimeMs: Long = -1L
    private var slumpActive: Boolean = false

    fun updateConfig(newConfig: PostureCalibrationConfig) {
        config = newConfig
        resetState()
    }

    fun processSample(
        pitch: Float,
        roll: Float,
        activityState: ActivityState,
        currentTimeMs: Long,
    ): AnalyzerResult {
        val calibration = config ?: return AnalyzerResult.NONE

        if (activityState != ActivityState.SITTING) {
            val wasTracking = slumpStartTimeMs >= 0L || slumpActive
            resetState()
            return if (wasTracking) AnalyzerResult.STATE_RESET else AnalyzerResult.NONE
        }

        if (currentTimeMs < slumpStartTimeMs) {
            slumpStartTimeMs = currentTimeMs
        }

        val tolerance = SensitivityTolerances.slumpTolerance(calibration.sensitivity)
        val pitchDelta = angleDelta(pitch, calibration.basePitch)
        val rollDelta = angleDelta(roll, calibration.baseRoll)
        val isBadPosture = pitchDelta > tolerance || rollDelta > tolerance

        if (!isBadPosture) {
            val corrected = slumpActive || slumpStartTimeMs >= 0L
            resetState()
            return if (corrected) AnalyzerResult.POSTURE_CORRECTED else AnalyzerResult.NONE
        }

        if (slumpStartTimeMs < 0L) {
            slumpStartTimeMs = currentTimeMs
            return AnalyzerResult.NONE
        }

        val slumpDuration = currentTimeMs - slumpStartTimeMs
        if (slumpDuration < calibration.slumpDurationThresholdMs) {
            return AnalyzerResult.NONE
        }

        if (!slumpActive) {
            slumpActive = true
            lastAlertTimeMs = currentTimeMs
            return AnalyzerResult.SLUMP_INITIAL_ALERT
        }

        if (currentTimeMs - lastAlertTimeMs >= calibration.repeatAlertIntervalMs) {
            lastAlertTimeMs = currentTimeMs
            return AnalyzerResult.SLUMP_REPEAT_ALERT
        }

        return AnalyzerResult.NONE
    }

    fun resetState() {
        slumpStartTimeMs = -1L
        lastAlertTimeMs = -1L
        slumpActive = false
    }

    fun isSlumpActive(): Boolean = slumpActive

    fun slumpDurationSeconds(currentTimeMs: Long): Int {
        if (slumpStartTimeMs < 0L) return 0
        return ((currentTimeMs - slumpStartTimeMs) / 1000L).toInt()
    }

    private fun angleDelta(current: Float, baseline: Float): Float {
        val delta = abs(current - baseline)
        return minOf(delta, 360f - delta)
    }
}
