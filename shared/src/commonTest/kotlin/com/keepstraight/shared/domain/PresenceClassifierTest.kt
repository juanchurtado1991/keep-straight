package com.keepstraight.shared.domain

import com.keepstraight.shared.vision.BodyPose
import com.keepstraight.shared.vision.Landmark
import com.keepstraight.shared.vision.PoseLandmark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PresenceClassifierTest {

    @Test
    fun away_whenNoPoseLongEnough() {
        val state = PresenceClassifier.classify(
            pose = null,
            features = null,
            calibration = null,
            millisSinceLastGoodPose = PresenceClassifier.AWAY_TIMEOUT_MS + 1,
        )
        assertEquals(PresenceState.AWAY, state)
        assertFalse(PresenceClassifier.shouldRunSlumpTimers(state))
    }

    @Test
    fun standing_whenHipsMuchHigherThanCalibration() {
        val erect = LandmarkPostureFeatures(
            torsoLean = 0.1f,
            headForward = 0f,
            neckDrop = 0.05f,
            headDrop = 0.04f,
            hipY = 0.75f,
            shoulderY = 0.45f,
            shoulderWidth = 0.25f,
            meanConfidence = 0.9f,
        )
        val standing = erect.copy(hipY = 0.50f, shoulderY = 0.25f)
        val pose = BodyPose(emptyMap(), 0L)
        val state = PresenceClassifier.classify(
            pose = pose,
            features = standing,
            calibration = LandmarkCalibration(erect, erect.copy(torsoLean = 0.4f)),
            millisSinceLastGoodPose = 0L,
        )
        assertEquals(PresenceState.STANDING, state)
        assertFalse(PresenceClassifier.shouldRunSlumpTimers(state))
    }

    @Test
    fun sitting_runsTimers_atDeskWebcamConfidence() {
        val features = LandmarkPostureFeatures(
            torsoLean = 0.1f,
            headForward = 0f,
            neckDrop = 0.05f,
            headDrop = 0.04f,
            hipY = 0.72f,
            shoulderY = 0.42f,
            shoulderWidth = 0.25f,
            meanConfidence = 0.15f,
        )
        val pose = BodyPose(
            mapOf(PoseLandmark.NOSE to Landmark(0.5f, 0.3f, 0.15f)),
            0L,
        )
        val state = PresenceClassifier.classify(
            pose = pose,
            features = features,
            calibration = null,
            millisSinceLastGoodPose = 0L,
        )
        assertEquals(PresenceState.SITTING, state)
        assertTrue(PresenceClassifier.shouldRunSlumpTimers(state))
    }
}
