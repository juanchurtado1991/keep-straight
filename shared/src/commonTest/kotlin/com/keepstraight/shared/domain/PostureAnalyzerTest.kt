package com.keepstraight.shared.domain

import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PostureAnalyzerTest {
    private val config = PostureCalibrationConfig(
        basePitch = 0f,
        baseRoll = 0f,
        sensitivity = SensitivityLevel.NORMAL,
        slumpDurationThresholdMs = 300_000L,
        repeatAlertIntervalMs = 5_000L,
    )

    private fun advanceBadPosture(analyzer: PostureAnalyzer, untilMs: Long): AnalyzerResult {
        var result = AnalyzerResult.NONE
        var t = 0L
        while (t <= untilMs) {
            result = analyzer.processSample(
                pitch = 20f,
                roll = 0f,
                activityState = ActivityState.SITTING,
                currentTimeMs = t,
            )
            t += 1_000L
        }
        return result
    }

    @Test
    fun noAlertBeforeThreshold() {
        val analyzer = PostureAnalyzer(config)
        val result = advanceBadPosture(analyzer, untilMs = 299_000L)
        assertEquals(AnalyzerResult.NONE, result)
    }

    @Test
    fun initialAlertAfterFiveMinutes() {
        val analyzer = PostureAnalyzer(config)
        val result = advanceBadPosture(analyzer, untilMs = 300_000L)
        assertEquals(AnalyzerResult.SLUMP_INITIAL_ALERT, result)
    }

    @Test
    fun repeatAlertEveryFiveSecondsWhileSlumping() {
        val analyzer = PostureAnalyzer(config)
        advanceBadPosture(analyzer, untilMs = 300_000L)

        val second = analyzer.processSample(20f, 0f, ActivityState.SITTING, 304_999L)
        assertEquals(AnalyzerResult.NONE, second)

        val third = analyzer.processSample(20f, 0f, ActivityState.SITTING, 305_000L)
        assertEquals(AnalyzerResult.SLUMP_REPEAT_ALERT, third)
    }

    @Test
    fun resetWhenWalking() {
        val analyzer = PostureAnalyzer(config)
        advanceBadPosture(analyzer, untilMs = 300_000L)
        assertTrue(analyzer.isSlumpActive())

        val reset = analyzer.processSample(20f, 0f, ActivityState.WALKING, 301_000L)
        assertEquals(AnalyzerResult.STATE_RESET, reset)
        assertFalse(analyzer.isSlumpActive())
    }

    @Test
    fun correctedPostureResetsEpisode() {
        val analyzer = PostureAnalyzer(config)
        advanceBadPosture(analyzer, untilMs = 300_000L)

        val corrected = analyzer.processSample(0f, 0f, ActivityState.SITTING, 301_000L)
        assertEquals(AnalyzerResult.POSTURE_CORRECTED, corrected)
        assertFalse(analyzer.isSlumpActive())
    }
}
