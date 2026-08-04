package com.keepstraight.shared.domain

import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.vision.BodyPose
import com.keepstraight.shared.vision.Landmark
import com.keepstraight.shared.vision.PoseLandmark
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Features extracted from upper-body landmarks for dual-pose (erect → slump) scoring.
 * Desk webcam shots often hide hips — shoulders + head are enough for slump scoring.
 */
data class LandmarkPostureFeatures(
    /** Mid-shoulder y relative to mid-hip y (or estimated hip). */
    val torsoLean: Float,
    /** Nose x offset from mid-shoulder, normalized by shoulder width. */
    val headForward: Float,
    /** Ear–shoulder vertical drop, normalized by shoulder width. */
    val neckDrop: Float,
    /** Nose y relative to mid-shoulder (forward/down head in slump). */
    val headDrop: Float,
    /** Mid-hip y in frame (estimated if hips are not visible). */
    val hipY: Float,
    /** Mid-shoulder y in frame. */
    val shoulderY: Float,
    val shoulderWidth: Float,
    val meanConfidence: Float,
)

data class LandmarkCalibration(
    val erect: LandmarkPostureFeatures,
    val slumped: LandmarkPostureFeatures,
    val sensitivity: SensitivityLevel = SensitivityLevel.NORMAL,
    val slumpDurationThresholdMs: Long = 30_000L,
    val repeatAlertIntervalMs: Long = 5_000L,
    val calibratedAtMs: Long = 0L,
)

object LandmarkPostureScorer {
    const val MIN_FEATURE_SEPARATION = 0.08f
    /** Desk webcams often score shoulders/nose in the mid-teens; keep usable. */
    const val MIN_KEYPOINT_CONFIDENCE = 0.12f
    private const val MIN_CORE_CONFIDENCE = 0.12f

    fun slumpScoreThreshold(level: SensitivityLevel): Float = when (level) {
        SensitivityLevel.STRICT -> 0.35f
        SensitivityLevel.NORMAL -> 0.45f
        SensitivityLevel.RELAXED -> 0.55f
    }

    fun extractFeatures(pose: BodyPose): LandmarkPostureFeatures? {
        val lShoulder = pose.get(PoseLandmark.LEFT_SHOULDER) ?: return null
        val rShoulder = pose.get(PoseLandmark.RIGHT_SHOULDER) ?: return null
        val nose = pose.get(PoseLandmark.NOSE) ?: return null

        // Core upper-body points must be usable. Hips are optional at a desk.
        if (!reliable(lShoulder, rShoulder, nose)) return null

        val midShoulderX = (lShoulder.x + rShoulder.x) / 2f
        val midShoulderY = (lShoulder.y + rShoulder.y) / 2f
        val shoulderWidth = abs(rShoulder.x - lShoulder.x).coerceAtLeast(0.05f)

        val lHip = pose.get(PoseLandmark.LEFT_HIP)
        val rHip = pose.get(PoseLandmark.RIGHT_HIP)
        val midHipY = averageY(lHip, rHip)
            // Typical seated torso length ≈ 1.4× shoulder width below shoulders.
            ?: (midShoulderY + shoulderWidth * 1.4f).coerceIn(0f, 1f)

        val earY = averageY(pose.get(PoseLandmark.LEFT_EAR), pose.get(PoseLandmark.RIGHT_EAR))
            ?: nose.y

        val torsoLean = (midShoulderY - midHipY) / shoulderWidth
        val headForward = (nose.x - midShoulderX) / shoulderWidth
        val neckDrop = (earY - midShoulderY) / shoulderWidth
        val headDrop = (nose.y - midShoulderY) / shoulderWidth

        val coreConf = listOf(lShoulder.confidence, rShoulder.confidence, nose.confidence).average().toFloat()
        if (coreConf < MIN_CORE_CONFIDENCE) return null

        return LandmarkPostureFeatures(
            torsoLean = torsoLean,
            headForward = headForward,
            neckDrop = neckDrop,
            headDrop = headDrop,
            hipY = midHipY,
            shoulderY = midShoulderY,
            shoulderWidth = shoulderWidth,
            meanConfidence = coreConf,
        )
    }

    fun featureDistance(a: LandmarkPostureFeatures, b: LandmarkPostureFeatures): Float {
        val dt = a.torsoLean - b.torsoLean
        val dh = a.headForward - b.headForward
        val dn = a.neckDrop - b.neckDrop
        val dd = a.headDrop - b.headDrop
        return sqrt(dt * dt + dh * dh + dn * dn + dd * dd)
    }

    fun hasUsableCalibration(cal: LandmarkCalibration): Boolean =
        featureDistance(cal.erect, cal.slumped) >= MIN_FEATURE_SEPARATION

    /**
     * Directed score along erect → slumped. 0 ≈ erect, 1 ≈ slumped reference.
     */
    fun directedSlumpScore(features: LandmarkPostureFeatures, cal: LandmarkCalibration): Float? {
        if (!hasUsableCalibration(cal)) return null
        val vx = cal.slumped.torsoLean - cal.erect.torsoLean
        val vy = cal.slumped.headForward - cal.erect.headForward
        val vz = cal.slumped.neckDrop - cal.erect.neckDrop
        val vw = cal.slumped.headDrop - cal.erect.headDrop
        val mag2 = vx * vx + vy * vy + vz * vz + vw * vw
        if (mag2 < 1e-6f) return null
        val cx = features.torsoLean - cal.erect.torsoLean
        val cy = features.headForward - cal.erect.headForward
        val cz = features.neckDrop - cal.erect.neckDrop
        val cw = features.headDrop - cal.erect.headDrop
        return ((cx * vx + cy * vy + cz * vz + cw * vw) / mag2).coerceAtLeast(0f)
    }

    fun isSlumped(features: LandmarkPostureFeatures, cal: LandmarkCalibration): Boolean {
        val score = directedSlumpScore(features, cal) ?: return false
        return score >= slumpScoreThreshold(cal.sensitivity)
    }

    private fun reliable(vararg points: Landmark): Boolean =
        points.all { it.confidence >= MIN_KEYPOINT_CONFIDENCE }

    private fun averageY(a: Landmark?, b: Landmark?): Float? {
        return when {
            a != null && b != null &&
                a.confidence >= MIN_KEYPOINT_CONFIDENCE &&
                b.confidence >= MIN_KEYPOINT_CONFIDENCE -> (a.y + b.y) / 2f
            a != null && a.confidence >= MIN_KEYPOINT_CONFIDENCE -> a.y
            b != null && b.confidence >= MIN_KEYPOINT_CONFIDENCE -> b.y
            else -> null
        }
    }
}
