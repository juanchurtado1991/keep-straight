package com.keepstraight.shared.domain

import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.util.FixedSampleBuffer
import kotlin.math.abs
import kotlin.math.sqrt

class ActivityClassifier(
    private var config: PostureCalibrationConfig?,
) {
    private var stepBaseline: Int = -1
    private var lastStepSampleTimeMs: Long = 0L
    private var stepsInWindow: Int = 0
    private var lastObservedStepCount: Int = -1
    private var lastStepIncreaseTimeMs: Long = 0L

    private var standingCandidateStartMs: Long = -1L
    private var sittingCandidateStartMs: Long = -1L

    private var currentState: ActivityState = ActivityState.AMBIGUOUS

    private val pitchBuffer = FixedSampleBuffer(5)
    private val rollBuffer = FixedSampleBuffer(5)
    private val gravityBuffer = FixedSampleBuffer(5)

    private var isOffWrist: Boolean = false

    fun updateConfig(newConfig: PostureCalibrationConfig) {
        config = newConfig
        reset()
    }

    fun setOffWrist(offWrist: Boolean) {
        isOffWrist = offWrist
        if (offWrist) {
            standingCandidateStartMs = -1L
            sittingCandidateStartMs = -1L
            currentState = ActivityState.NOT_WORN
        }
    }

    fun classify(
        pitch: Float,
        roll: Float,
        ax: Float = 0f,
        ay: Float = 0f,
        az: Float = 0f,
        stepCount: Int,
        currentTimeMs: Long,
    ): ActivityState {
        if (isOffWrist) {
            currentState = ActivityState.NOT_WORN
            return ActivityState.NOT_WORN
        }

        val calibration = config ?: run {
            currentState = ActivityState.AMBIGUOUS
            return ActivityState.AMBIGUOUS
        }

        updateStepWindow(stepCount, currentTimeMs)

        pitchBuffer.add(pitch)
        rollBuffer.add(roll)
        val gravityMagnitude = sqrt(ax * ax + ay * ay + az * az)
        gravityBuffer.add(gravityMagnitude)

        val smoothPitch = pitchBuffer.average()
        val smoothRoll = rollBuffer.average()
        val pitchDelta = angleDelta(smoothPitch, calibration.basePitch)
        val rollDelta = angleDelta(smoothRoll, calibration.baseRoll)
        val standingPitchDelta = calibration.sensitivity.standingPitchDelta()
        val standingRollDelta = calibration.sensitivity.standingRollDelta()

        val gravityStable = isGravityStable()
        val wristVertical = isWristVertical(ax, ay, az)
        val towardSlump = PostureScore.directedSlumpScore(smoothPitch, smoothRoll, calibration)
            ?.let { it >= PostureScore.slumpScoreThreshold(calibration.sensitivity) * 0.6f }
            ?: false
        val looksStandingAngles =
            !towardSlump &&
                (pitchDelta > standingPitchDelta || rollDelta > standingRollDelta)
        // Sitting band is angle + gravity only. Wrist "vertical" must NOT veto sitting:
        // on Galaxy Watch, desk typing often has dominant |az| while still seated.
        val inSittingBand = (!looksStandingAngles || towardSlump) && gravityStable

        if (stepsInWindow >= WALK_STEP_THRESHOLD) {
            standingCandidateStartMs = -1L
            sittingCandidateStartMs = -1L
            currentState = ActivityState.WALKING
            return ActivityState.WALKING
        }

        return when (currentState) {
            ActivityState.WALKING -> classifyFromWalking(inSittingBand, currentTimeMs)
            ActivityState.STANDING -> classifyFromStanding(inSittingBand, currentTimeMs)
            ActivityState.SITTING,
            ActivityState.AMBIGUOUS,
            ActivityState.NOT_WORN,
            -> classifyTowardSittingOrStanding(
                looksStandingAngles = looksStandingAngles,
                wristVertical = wristVertical,
                gravityStable = gravityStable,
                inSittingBand = inSittingBand,
                pitchDelta = pitchDelta,
                rollDelta = rollDelta,
                standingPitchDelta = standingPitchDelta,
                standingRollDelta = standingRollDelta,
                currentTimeMs = currentTimeMs,
            )
        }
    }

    fun reset() {
        stepBaseline = -1
        lastStepSampleTimeMs = 0L
        stepsInWindow = 0
        lastObservedStepCount = -1
        lastStepIncreaseTimeMs = 0L
        standingCandidateStartMs = -1L
        sittingCandidateStartMs = -1L
        currentState = ActivityState.AMBIGUOUS
        pitchBuffer.clear()
        rollBuffer.clear()
        gravityBuffer.clear()
    }

    private fun classifyFromWalking(
        inSittingBand: Boolean,
        currentTimeMs: Long,
    ): ActivityState {
        val noStepsLongEnough =
            lastStepIncreaseTimeMs > 0L &&
                currentTimeMs - lastStepIncreaseTimeMs >= WALKING_TO_SITTING_MS

        if (noStepsLongEnough && inSittingBand) {
            if (sittingCandidateStartMs < 0L) {
                sittingCandidateStartMs = currentTimeMs
            }
            // Spec: zero steps for ≥20 s AND sitting angles restored (already in band).
            sittingCandidateStartMs = -1L
            standingCandidateStartMs = -1L
            currentState = ActivityState.SITTING
            return ActivityState.SITTING
        }

        if (!noStepsLongEnough || !inSittingBand) {
            sittingCandidateStartMs = -1L
        }
        return ActivityState.WALKING
    }

    private fun classifyFromStanding(
        inSittingBand: Boolean,
        currentTimeMs: Long,
    ): ActivityState {
        if (inSittingBand) {
            if (sittingCandidateStartMs < 0L) {
                sittingCandidateStartMs = currentTimeMs
            }
            if (currentTimeMs - sittingCandidateStartMs >= STANDING_TO_SITTING_MS) {
                sittingCandidateStartMs = -1L
                standingCandidateStartMs = -1L
                currentState = ActivityState.SITTING
                return ActivityState.SITTING
            }
            return ActivityState.STANDING
        }

        sittingCandidateStartMs = -1L
        return ActivityState.STANDING
    }

    private fun classifyTowardSittingOrStanding(
        looksStandingAngles: Boolean,
        wristVertical: Boolean,
        gravityStable: Boolean,
        inSittingBand: Boolean,
        pitchDelta: Float,
        rollDelta: Float,
        standingPitchDelta: Float,
        standingRollDelta: Float,
        currentTimeMs: Long,
    ): ActivityState {
        // Vertical wrist alone is not standing — only reinforces when angles are
        // already approaching the standing band (arm hanging away from calibrate pose).
        val borderlineAngles =
            pitchDelta > standingPitchDelta * BORDERLINE_STANDING_FACTOR ||
                rollDelta > standingRollDelta * BORDERLINE_STANDING_FACTOR
        val looksStanding =
            looksStandingAngles ||
                (wristVertical && gravityStable && borderlineAngles)

        if (looksStanding) {
            sittingCandidateStartMs = -1L
            if (standingCandidateStartMs < 0L) {
                standingCandidateStartMs = currentTimeMs
            }
            return if (currentTimeMs - standingCandidateStartMs >= STANDING_HOLD_MS) {
                currentState = ActivityState.STANDING
                ActivityState.STANDING
            } else {
                currentState = ActivityState.AMBIGUOUS
                ActivityState.AMBIGUOUS
            }
        }

        standingCandidateStartMs = -1L
        if (inSittingBand) {
            sittingCandidateStartMs = -1L
            currentState = ActivityState.SITTING
            return ActivityState.SITTING
        }

        // Near baseline but gravity briefly noisy (typing) — stay sitting if we already were.
        if (currentState == ActivityState.SITTING && !looksStandingAngles) {
            return ActivityState.SITTING
        }

        currentState = ActivityState.AMBIGUOUS
        return ActivityState.AMBIGUOUS
    }

    private fun updateStepWindow(stepCount: Int, currentTimeMs: Long) {
        if (lastObservedStepCount < 0) {
            lastObservedStepCount = stepCount
            lastStepIncreaseTimeMs = currentTimeMs
        } else if (stepCount > lastObservedStepCount) {
            lastObservedStepCount = stepCount
            lastStepIncreaseTimeMs = currentTimeMs
        } else if (stepCount < lastObservedStepCount) {
            // Counter reset (reboot) — re-baseline without treating as motion.
            lastObservedStepCount = stepCount
            lastStepIncreaseTimeMs = currentTimeMs
        }

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
    }

    private fun isGravityStable(): Boolean {
        if (gravityBuffer.size() < 3) return true
        val avg = gravityBuffer.average()
        // No usable accelerometer yet (e.g. unit tests passing angles only).
        if (avg < MIN_USABLE_MAGNITUDE) return true
        return abs(avg - GRAVITY_MS2) <= GRAVITY_STABLE_BAND &&
            gravityBuffer.variance() <= GRAVITY_VARIANCE_MAX
    }

    private fun isWristVertical(ax: Float, ay: Float, az: Float): Boolean {
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        if (magnitude < MIN_USABLE_MAGNITUDE) return false
        val absAz = abs(az)
        val horizontal = sqrt(ax * ax + ay * ay)
        return absAz / magnitude >= VERTICAL_AZ_RATIO &&
            horizontal / magnitude <= VERTICAL_HORIZONTAL_MAX_RATIO
    }

    private fun angleDelta(current: Float, baseline: Float): Float {
        val delta = abs(current - baseline)
        return minOf(delta, 360f - delta)
    }

    private companion object {
        const val STEP_WINDOW_MS = 10_000L
        const val WALK_STEP_THRESHOLD = 3
        const val STANDING_HOLD_MS = 30_000L
        const val STANDING_TO_SITTING_MS = 15_000L
        const val WALKING_TO_SITTING_MS = 20_000L
        const val GRAVITY_MS2 = 9.81f
        const val GRAVITY_STABLE_BAND = 2.5f
        const val GRAVITY_VARIANCE_MAX = 1.25f
        const val MIN_USABLE_MAGNITUDE = 1f
        const val VERTICAL_AZ_RATIO = 0.85f
        const val VERTICAL_HORIZONTAL_MAX_RATIO = 0.45f
        const val BORDERLINE_STANDING_FACTOR = 0.75f
    }
}

private fun SensitivityLevel.standingPitchDelta(): Float = when (this) {
    SensitivityLevel.STRICT -> 18f
    SensitivityLevel.NORMAL -> 22f
    SensitivityLevel.RELAXED -> 28f
}

private fun SensitivityLevel.standingRollDelta(): Float = when (this) {
    SensitivityLevel.STRICT -> 12f
    SensitivityLevel.NORMAL -> 15f
    SensitivityLevel.RELAXED -> 18f
}
