package com.keepstraight.shared.domain

import com.keepstraight.shared.presentation.DesktopIssue
import com.keepstraight.shared.vision.BodyPose
import com.keepstraight.shared.vision.Landmark
import com.keepstraight.shared.vision.PoseLandmark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopPostureSessionTest {

    @Test
    fun pausesAndResetsTimer_whenAway() {
        val session = DesktopPostureSession()
        val erectPose = seatedPose(shoulderY = 0.40f, hipY = 0.75f, noseY = 0.32f)
        val slumpPose = seatedPose(shoulderY = 0.48f, hipY = 0.76f, noseY = 0.42f)

        val erectFeatures = LandmarkPostureScorer.extractFeatures(erectPose)!!
        val slumpFeatures = LandmarkPostureScorer.extractFeatures(slumpPose)!!
        assertTrue(
            LandmarkPostureScorer.featureDistance(erectFeatures, slumpFeatures) >=
                LandmarkPostureScorer.MIN_FEATURE_SEPARATION,
        )

        session.setCalibration(
            LandmarkCalibration(
                erect = erectFeatures,
                slumped = slumpFeatures,
                slumpDurationThresholdMs = 1_000L,
                repeatAlertIntervalMs = 500L,
            ),
        )
        assertTrue(session.startSession())

        session.onPose(erectPose, nowMs = 1_000L)
        session.onPose(slumpPose, nowMs = 1_200L)
        assertTrue(session.uiState.value.slumpScore > 0.3f)

        session.onPose(null, nowMs = 1_200L + PresenceClassifier.AWAY_TIMEOUT_MS + 100)
        assertEquals(PresenceState.AWAY, session.uiState.value.presence)
        assertEquals(0L, session.uiState.value.slumpElapsedMs)

        val alert = session.onPose(slumpPose, nowMs = 10_000L)
        assertNull(alert)
        assertTrue(session.uiState.value.slumpElapsedMs < 1_000L)
    }

    @Test
    fun erectCalibration_completesWithoutHips() {
        val session = DesktopPostureSession()
        fun lm(x: Float, y: Float) = Landmark(x, y, 0.9f)
        val pose = BodyPose(
            landmarks = mapOf(
                PoseLandmark.NOSE to lm(0.52f, 0.32f),
                PoseLandmark.LEFT_SHOULDER to lm(0.38f, 0.40f),
                PoseLandmark.RIGHT_SHOULDER to lm(0.62f, 0.40f),
            ),
            timestampMs = 0L,
        )
        assertTrue(session.beginErectCalibration())
        session.onPose(pose, nowMs = 1_000L)
        session.onPose(pose, nowMs = 1_400L)
        session.onPose(pose, nowMs = 1_800L)
        session.onPose(pose, nowMs = 2_300L)
        assertTrue(session.uiState.value.hasErectCapture)
        assertEquals(CalibrationPhase.NONE, session.uiState.value.calibrationPhase)
    }

    @Test
    fun startSession_blockedWithoutCalibration() {
        val session = DesktopPostureSession()
        assertFalse(session.startSession())
        assertTrue(session.uiState.value.issue is DesktopIssue.NeedsCalibration)
    }

    @Test
    fun slumpCalibration_requiresErectFirst() {
        val session = DesktopPostureSession()
        assertFalse(session.beginSlumpCalibration())
        assertTrue(session.uiState.value.issue is DesktopIssue.CalibrationNeedsErectFirst)
    }

    @Test
    fun modelMissing_blocksStart() {
        val session = DesktopPostureSession()
        session.setModelReady(false)
        assertFalse(session.startSession())
        assertTrue(session.uiState.value.issue is DesktopIssue.ModelMissing)
    }

    @Test
    fun recalibrate_requiresSlumpAfterErect() {
        val session = DesktopPostureSession()
        val erectPose = seatedPose(shoulderY = 0.40f, hipY = 0.75f, noseY = 0.32f)
        val slumpPose = seatedPose(shoulderY = 0.52f, hipY = 0.76f, noseY = 0.48f)
        val erectFeatures = LandmarkPostureScorer.extractFeatures(erectPose)!!
        val slumpFeatures = LandmarkPostureScorer.extractFeatures(slumpPose)!!
        session.setCalibration(
            LandmarkCalibration(erect = erectFeatures, slumped = slumpFeatures),
        )
        assertEquals(CalibrationPhase.COMPLETE, session.uiState.value.calibrationPhase)
        assertTrue(session.uiState.value.hasCalibration)

        assertTrue(session.beginErectCalibration())
        assertFalse(session.uiState.value.hasErectCapture)
        assertEquals(CalibrationPhase.CAPTURE_ERECT, session.uiState.value.calibrationPhase)

        session.onPose(erectPose, nowMs = 1_000L)
        session.onPose(erectPose, nowMs = 1_400L)
        session.onPose(erectPose, nowMs = 1_800L)
        session.onPose(erectPose, nowMs = 2_300L)

        // Old calibration still loaded, but dual flow is mid-way — UI must offer slumped next.
        assertTrue(session.uiState.value.hasErectCapture)
        assertTrue(session.uiState.value.hasCalibration)
        assertEquals(CalibrationPhase.NONE, session.uiState.value.calibrationPhase)
        assertTrue(session.beginSlumpCalibration())
        assertEquals(CalibrationPhase.CAPTURE_SLUMP, session.uiState.value.calibrationPhase)

        session.onPose(slumpPose, nowMs = 3_000L)
        session.onPose(slumpPose, nowMs = 3_400L)
        session.onPose(slumpPose, nowMs = 3_800L)
        session.onPose(slumpPose, nowMs = 4_300L)

        assertEquals(CalibrationPhase.COMPLETE, session.uiState.value.calibrationPhase)
        assertTrue(session.uiState.value.hasCalibration)
    }

    private fun seatedPose(shoulderY: Float, hipY: Float, noseY: Float): BodyPose {
        fun lm(x: Float, y: Float) = Landmark(x, y, 0.95f)
        return BodyPose(
            landmarks = mapOf(
                PoseLandmark.NOSE to lm(0.52f, noseY),
                PoseLandmark.LEFT_EAR to lm(0.45f, noseY + 0.02f),
                PoseLandmark.RIGHT_EAR to lm(0.55f, noseY + 0.02f),
                PoseLandmark.LEFT_SHOULDER to lm(0.38f, shoulderY),
                PoseLandmark.RIGHT_SHOULDER to lm(0.62f, shoulderY),
                PoseLandmark.LEFT_HIP to lm(0.42f, hipY),
                PoseLandmark.RIGHT_HIP to lm(0.58f, hipY),
            ),
            timestampMs = 0L,
        )
    }
}
