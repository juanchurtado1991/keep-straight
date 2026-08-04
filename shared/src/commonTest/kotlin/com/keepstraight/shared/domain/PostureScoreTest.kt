package com.keepstraight.shared.domain

import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostureScoreTest {

    @Test
    fun directedScoreZeroAtGoodPose() {
        val config = dualConfig(goodPitch = 0f, slumpPitch = 20f)
        assertEquals(0f, PostureScore.directedSlumpScore(0f, 0f, config)!!, 0.05f)
    }

    @Test
    fun directedScoreNearOneAtSlumpPose() {
        val config = dualConfig(goodPitch = 0f, slumpPitch = 20f)
        val score = PostureScore.directedSlumpScore(20f, 0f, config)!!
        assertTrue(score in 0.9f..1.1f)
    }

    @Test
    fun badPostureWhenPastThresholdAlongSlumpAxis() {
        val config = dualConfig(goodPitch = 0f, slumpPitch = 20f)
        assertFalse(PostureScore.isBadPosture(5f, 0f, config))
        assertTrue(PostureScore.isBadPosture(12f, 0f, config))
    }

    @Test
    fun rejectsNearIdenticalReferences() {
        val config = PostureCalibrationConfig(
            basePitch = 0f,
            baseRoll = 0f,
            hasSlumpReference = true,
            slumpPitch = 3f,
            slumpRoll = 0f,
        )
        assertFalse(PostureScore.hasUsableSlumpReference(config))
    }

    private fun dualConfig(goodPitch: Float, slumpPitch: Float) = PostureCalibrationConfig(
        basePitch = goodPitch,
        baseRoll = 0f,
        sensitivity = SensitivityLevel.NORMAL,
        hasSlumpReference = true,
        slumpPitch = slumpPitch,
        slumpRoll = 0f,
    )
}
