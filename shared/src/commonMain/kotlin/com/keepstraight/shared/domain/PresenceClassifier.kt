package com.keepstraight.shared.domain

import com.keepstraight.shared.vision.BodyPose

enum class PresenceState {
    /** Seated at desk — posture monitoring active. */
    SITTING,
    /** Standing / standing desk — pause alerts. */
    STANDING,
    /** No reliable pose. */
    AWAY,
    /** Pose confidence too low (dark / profile). */
    LOW_CONFIDENCE,
}

/**
 * Sitting-only product: standing and away pause slump timers.
 */
object PresenceClassifier {
    const val AWAY_TIMEOUT_MS = 4_000L
    private const val STANDING_HIP_Y_MAX = 0.62f
    private const val SITTING_HIP_Y_MIN = 0.48f
    // Must match LandmarkPostureScorer.MIN_KEYPOINT_CONFIDENCE — desk cams often sit in the
    // mid-teens; a higher gate here paused timers while scoring still worked, so alerts never fired.
    private const val MIN_CONFIDENCE = 0.12f

    fun classify(
        pose: BodyPose?,
        features: LandmarkPostureFeatures?,
        calibration: LandmarkCalibration?,
        millisSinceLastGoodPose: Long,
    ): PresenceState {
        if (pose == null || features == null) {
            return if (millisSinceLastGoodPose >= AWAY_TIMEOUT_MS) {
                PresenceState.AWAY
            } else {
                PresenceState.LOW_CONFIDENCE
            }
        }
        if (features.meanConfidence < MIN_CONFIDENCE) {
            return PresenceState.LOW_CONFIDENCE
        }

        val erectHipY = calibration?.erect?.hipY
        if (erectHipY != null) {
            // Standing: hips noticeably higher in frame than seated calibration (smaller y).
            if (features.hipY < erectHipY - 0.12f) {
                return PresenceState.STANDING
            }
        } else {
            if (features.hipY < STANDING_HIP_Y_MAX && features.shoulderY < 0.35f) {
                return PresenceState.STANDING
            }
            if (features.hipY < SITTING_HIP_Y_MIN && features.shoulderWidth < 0.15f) {
                return PresenceState.STANDING
            }
        }

        return PresenceState.SITTING
    }

    fun shouldRunSlumpTimers(state: PresenceState): Boolean =
        state == PresenceState.SITTING
}
