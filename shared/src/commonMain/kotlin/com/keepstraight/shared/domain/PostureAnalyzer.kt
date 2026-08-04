package com.keepstraight.shared.domain

import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.util.FixedSampleBuffer

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
    private val pitchBuffer = FixedSampleBuffer(5)
    private val rollBuffer = FixedSampleBuffer(5)

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

        pitchBuffer.add(pitch)
        rollBuffer.add(roll)
        val smoothPitch = pitchBuffer.average()
        val smoothRoll = rollBuffer.average()

        if (currentTimeMs < slumpStartTimeMs) {
            slumpStartTimeMs = currentTimeMs
        }

        // Enter slump on smoothed pose; exit on raw pose so correction isn't delayed by the buffer.
        val inEpisode = slumpActive || slumpStartTimeMs >= 0L
        val isBadPosture = if (inEpisode) {
            PostureScore.isBadPosture(pitch, roll, calibration)
        } else {
            PostureScore.isBadPosture(smoothPitch, smoothRoll, calibration)
        }

        if (!isBadPosture) {
            val corrected = slumpActive || slumpStartTimeMs >= 0L
            // Keep buffers; only clear episode timers.
            slumpStartTimeMs = -1L
            lastAlertTimeMs = -1L
            slumpActive = false
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
        pitchBuffer.clear()
        rollBuffer.clear()
    }

    fun isSlumpActive(): Boolean = slumpActive

    fun slumpDurationSeconds(currentTimeMs: Long): Int {
        if (slumpStartTimeMs < 0L) return 0
        return ((currentTimeMs - slumpStartTimeMs) / 1000L).toInt()
    }
}
