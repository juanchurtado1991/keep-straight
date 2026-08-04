package com.keepstraight.shared.domain

import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ActivityClassifierTest {

    private val config = PostureCalibrationConfig(
        basePitch = 0f,
        baseRoll = 0f,
        sensitivity = SensitivityLevel.NORMAL,
    )

    private fun classifier() = ActivityClassifier(config)

    private fun ActivityClassifier.sit(
        t: Long,
        pitch: Float = 0f,
        roll: Float = 0f,
        steps: Int = 0,
        ax: Float = 0f,
        ay: Float = 9.81f,
        az: Float = 0f,
    ): ActivityState = classify(
        pitch = pitch,
        roll = roll,
        ax = ax,
        ay = ay,
        az = az,
        stepCount = steps,
        currentTimeMs = t,
    )

    private fun ActivityClassifier.advance(
        fromMs: Long,
        toMs: Long,
        pitch: Float = 0f,
        roll: Float = 0f,
        steps: Int = 0,
        ax: Float = 0f,
        ay: Float = 9.81f,
        az: Float = 0f,
        stepMs: Long = 500L,
    ): ActivityState {
        var state = ActivityState.AMBIGUOUS
        var t = fromMs
        while (t <= toMs) {
            state = sit(t, pitch, roll, steps, ax, ay, az)
            t += stepMs
        }
        return state
    }

    @Test
    fun sittingWhenNearBaseline() {
        val c = classifier()
        val state = c.advance(0L, 5_000L)
        assertEquals(ActivityState.SITTING, state)
    }

    @Test
    fun walkingWhenStepDeltaAtLeastThreeInWindow() {
        val c = classifier()
        c.sit(0L, steps = 0)
        val walking = c.sit(10_000L, steps = 3)
        assertEquals(ActivityState.WALKING, walking)
    }

    @Test
    fun standingRequiresThirtySecondHold() {
        val c = classifier()
        c.advance(0L, 2_000L, pitch = 0f)

        // Standing-looking angles need the 5-sample buffer to settle above threshold.
        assertEquals(ActivityState.AMBIGUOUS, c.advance(2_500L, 10_000L, pitch = 30f))
        assertEquals(ActivityState.AMBIGUOUS, c.advance(10_500L, 32_000L, pitch = 30f))
        assertEquals(ActivityState.STANDING, c.advance(32_500L, 35_000L, pitch = 30f))
    }

    @Test
    fun standingToSittingRequiresFifteenSecondHysteresis() {
        val c = classifier()
        c.advance(0L, 2_000L, pitch = 0f)
        assertEquals(ActivityState.STANDING, c.advance(2_500L, 35_000L, pitch = 30f))

        assertEquals(ActivityState.STANDING, c.advance(35_500L, 49_000L, pitch = 0f))
        assertEquals(ActivityState.SITTING, c.advance(49_500L, 52_000L, pitch = 0f))
    }

    @Test
    fun walkingToSittingRequiresTwentySecondsZeroStepsAndSittingBand() {
        val c = classifier()
        c.sit(0L, steps = 0)
        assertEquals(ActivityState.WALKING, c.sit(10_000L, steps = 3))

        assertEquals(ActivityState.WALKING, c.advance(10_500L, 29_000L, pitch = 0f, steps = 3))
        assertEquals(ActivityState.SITTING, c.advance(29_500L, 31_000L, pitch = 0f, steps = 3))
    }

    @Test
    fun deskPoseWithDominantAzStillSitting() {
        val c = classifier()
        // Galaxy Watch at a desk often has gravity mostly on Z while seated.
        val state = c.advance(0L, 5_000L, ax = 0f, ay = 0f, az = 9.81f)
        assertEquals(ActivityState.SITTING, state)
    }

    @Test
    fun verticalWristAloneDoesNotForceStanding() {
        val c = classifier()
        c.advance(0L, 2_000L, ax = 0f, ay = 9.81f, az = 0f)

        assertEquals(
            ActivityState.SITTING,
            c.advance(2_500L, 35_000L, ax = 0f, ay = 0f, az = 9.81f),
        )
    }

    @Test
    fun verticalWristPlusStandingAnglesBecomesStanding() {
        val c = classifier()
        c.advance(0L, 2_000L, pitch = 0f, ax = 0f, ay = 9.81f, az = 0f)

        assertEquals(
            ActivityState.AMBIGUOUS,
            c.advance(2_500L, 10_000L, pitch = 30f, ax = 0f, ay = 0f, az = 9.81f),
        )
        assertEquals(
            ActivityState.STANDING,
            c.advance(10_500L, 35_000L, pitch = 30f, ax = 0f, ay = 0f, az = 9.81f),
        )
    }

    @Test
    fun offWristForcesNotWorn() {
        val c = classifier()
        c.setOffWrist(true)
        assertEquals(ActivityState.NOT_WORN, c.sit(1_000L))
    }

    @Test
    fun slumpAnglesStillCountAsSitting() {
        val c = classifier()
        // Normal sensitivity standing pitch delta is 22°; slump at 12° stays sitting.
        val state = c.advance(0L, 5_000L, pitch = 12f)
        assertEquals(ActivityState.SITTING, state)
        assertNotEquals(ActivityState.STANDING, state)
    }
}
