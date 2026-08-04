package com.keepstraight.shared.domain

import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.vision.BodyPose
import com.keepstraight.shared.vision.Landmark
import com.keepstraight.shared.vision.PoseLandmark
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LandmarkPostureScorerTest {

    @Test
    fun directedScore_increasesTowardSlump() {
        val erect = features(torso = 0.1f, head = 0f, neck = 0.05f, hipY = 0.7f)
        val slumped = features(torso = 0.4f, head = 0.2f, neck = 0.25f, hipY = 0.72f)
        val cal = LandmarkCalibration(erect = erect, slumped = slumped)
        assertTrue(LandmarkPostureScorer.hasUsableCalibration(cal))

        val mid = features(torso = 0.25f, head = 0.1f, neck = 0.15f, hipY = 0.71f)
        val scoreErect = LandmarkPostureScorer.directedSlumpScore(erect, cal)
        val scoreMid = LandmarkPostureScorer.directedSlumpScore(mid, cal)
        val scoreSlump = LandmarkPostureScorer.directedSlumpScore(slumped, cal)
        assertNotNull(scoreErect)
        assertNotNull(scoreMid)
        assertNotNull(scoreSlump)
        assertTrue(scoreErect!! < scoreMid!!)
        assertTrue(scoreMid < scoreSlump!!)
        assertTrue(scoreSlump >= 0.9f)
    }

    @Test
    fun extractFeatures_fromSyntheticPose() {
        val pose = syntheticPose(shoulderY = 0.4f, hipY = 0.75f, noseOffsetX = 0.02f)
        val features = LandmarkPostureScorer.extractFeatures(pose)
        assertNotNull(features)
        assertTrue(features!!.meanConfidence > 0.5f)
    }

    @Test
    fun isSlumped_respectsSensitivity() {
        val erect = features(torso = 0.1f, head = 0f, neck = 0.05f, hipY = 0.7f)
        val slumped = features(torso = 0.5f, head = 0.25f, neck = 0.3f, hipY = 0.72f)
        val strict = LandmarkCalibration(erect, slumped, SensitivityLevel.STRICT)
        val relaxed = LandmarkCalibration(erect, slumped, SensitivityLevel.RELAXED)
        val mild = features(torso = 0.28f, head = 0.1f, neck = 0.15f, hipY = 0.71f)
        assertTrue(LandmarkPostureScorer.isSlumped(mild, strict))
        assertFalse(LandmarkPostureScorer.isSlumped(mild, relaxed))
    }

    @Test
    fun extractFeatures_worksWithoutHips() {
        fun lm(x: Float, y: Float) = Landmark(x, y, 0.9f)
        val pose = BodyPose(
            landmarks = mapOf(
                PoseLandmark.NOSE to lm(0.52f, 0.32f),
                PoseLandmark.LEFT_SHOULDER to lm(0.38f, 0.40f),
                PoseLandmark.RIGHT_SHOULDER to lm(0.62f, 0.40f),
            ),
            timestampMs = 0L,
        )
        val features = LandmarkPostureScorer.extractFeatures(pose)
        assertNotNull(features)
    }

    private fun features(
        torso: Float,
        head: Float,
        neck: Float,
        hipY: Float,
    ) = LandmarkPostureFeatures(
        torsoLean = torso,
        headForward = head,
        neckDrop = neck,
        headDrop = neck * 0.8f,
        hipY = hipY,
        shoulderY = hipY - 0.3f,
        shoulderWidth = 0.25f,
        meanConfidence = 0.9f,
    )

    private fun syntheticPose(
        shoulderY: Float,
        hipY: Float,
        noseOffsetX: Float,
    ): BodyPose {
        fun lm(x: Float, y: Float) = Landmark(x, y, 0.95f)
        return BodyPose(
            landmarks = mapOf(
                PoseLandmark.NOSE to lm(0.5f + noseOffsetX, shoulderY - 0.08f),
                PoseLandmark.LEFT_EAR to lm(0.45f, shoulderY - 0.06f),
                PoseLandmark.RIGHT_EAR to lm(0.55f, shoulderY - 0.06f),
                PoseLandmark.LEFT_SHOULDER to lm(0.38f, shoulderY),
                PoseLandmark.RIGHT_SHOULDER to lm(0.62f, shoulderY),
                PoseLandmark.LEFT_HIP to lm(0.42f, hipY),
                PoseLandmark.RIGHT_HIP to lm(0.58f, hipY),
            ),
            timestampMs = 0L,
        )
    }
}
