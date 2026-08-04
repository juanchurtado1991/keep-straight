package com.keepstraight.shared.domain

import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Wrist-IMU posture scoring.
 *
 * A watch cannot measure the spine. Reliable detection needs a **good** baseline and a
 * **slouch** reference captured on the same wrist so we learn that user's slouch signature.
 */
object PostureScore {
    /** Minimum angular separation (°) between good and slouch references. */
    const val MIN_REFERENCE_SEPARATION_DEG = 8f

    /** Projection along good→slouch at/above this counts as slumped. */
    fun slumpScoreThreshold(level: SensitivityLevel): Float = when (level) {
        SensitivityLevel.STRICT -> 0.35f
        SensitivityLevel.NORMAL -> 0.45f
        SensitivityLevel.RELAXED -> 0.55f
    }

    fun angleDelta(a: Float, b: Float): Float {
        val delta = abs(a - b)
        return minOf(delta, 360f - delta)
    }

    fun distanceDeg(pitch1: Float, roll1: Float, pitch2: Float, roll2: Float): Float {
        val dp = angleDelta(pitch1, pitch2)
        val dr = angleDelta(roll1, roll2)
        return sqrt(dp * dp + dr * dr)
    }

    fun hasUsableSlumpReference(config: PostureCalibrationConfig): Boolean {
        if (!config.hasSlumpReference) return false
        return distanceDeg(
            config.basePitch,
            config.baseRoll,
            config.slumpPitch,
            config.slumpRoll,
        ) >= MIN_REFERENCE_SEPARATION_DEG
    }

    /**
     * 0 ≈ good posture, 1 ≈ at slouch reference, >1 past the slouch pose.
     * Returns null when dual-pose scoring is unavailable (caller should use absolute delta).
     */
    fun directedSlumpScore(pitch: Float, roll: Float, config: PostureCalibrationConfig): Float? {
        if (!hasUsableSlumpReference(config)) return null
        val vx = signedDelta(config.slumpPitch, config.basePitch)
        val vy = signedDelta(config.slumpRoll, config.baseRoll)
        val mag2 = vx * vx + vy * vy
        if (mag2 < 1f) return null
        val cx = signedDelta(pitch, config.basePitch)
        val cy = signedDelta(roll, config.baseRoll)
        return ((cx * vx + cy * vy) / mag2).coerceAtLeast(0f)
    }

    fun isBadPosture(pitch: Float, roll: Float, config: PostureCalibrationConfig): Boolean {
        val directed = directedSlumpScore(pitch, roll, config)
        if (directed != null) {
            return directed >= slumpScoreThreshold(config.sensitivity)
        }
        val tolerance = SensitivityTolerances.slumpTolerance(config.sensitivity)
        return angleDelta(pitch, config.basePitch) > tolerance ||
            angleDelta(roll, config.baseRoll) > tolerance
    }

    /** Max absolute deviation from good baseline (for live UI). */
    fun deviationFromGood(pitch: Float, roll: Float, config: PostureCalibrationConfig): Float =
        maxOf(
            angleDelta(pitch, config.basePitch),
            angleDelta(roll, config.baseRoll),
        )

    private fun signedDelta(current: Float, baseline: Float): Float {
        var d = current - baseline
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return d
    }
}
