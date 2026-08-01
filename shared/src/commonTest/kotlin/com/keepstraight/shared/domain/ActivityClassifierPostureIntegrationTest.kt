package com.keepstraight.shared.domain

import com.keepstraight.shared.model.PostureCalibrationConfig
import com.keepstraight.shared.model.SensitivityLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityClassifierPostureIntegrationTest {

    private val config = PostureCalibrationConfig(
        basePitch = 0f,
        baseRoll = 0f,
        sensitivity = SensitivityLevel.NORMAL,
        slumpDurationThresholdMs = 300_000L,
        repeatAlertIntervalMs = 5_000L,
    )

    @Test
    fun fiveMinuteSlumpWhileSittingTriggersInitialAlert() {
        val classifier = ActivityClassifier(config)
        val analyzer = PostureAnalyzer(config)
        val slumpPitch = 12f

        var lastResult = AnalyzerResult.NONE
        var t = 0L
        while (t <= 300_000L) {
            val activity = classifier.classify(
                pitch = slumpPitch,
                roll = 0f,
                stepCount = 0,
                currentTimeMs = t,
            )
            lastResult = analyzer.processSample(
                pitch = slumpPitch,
                roll = 0f,
                activityState = activity,
                currentTimeMs = t,
            )
            t += 1_000L
        }

        assertEquals(ActivityState.SITTING, classifier.classify(slumpPitch, 0f, 0, 300_000L))
        assertEquals(AnalyzerResult.SLUMP_INITIAL_ALERT, lastResult)
        assertTrue(analyzer.isSlumpActive())
    }

    @Test
    fun walkingResetsSlumpTracking() {
        val classifier = ActivityClassifier(config)
        val analyzer = PostureAnalyzer(config)
        val slumpPitch = 12f

        repeat(301) { second ->
            val t = second * 1_000L
            val activity = classifier.classify(slumpPitch, 0f, 0, t)
            analyzer.processSample(slumpPitch, 0f, activity, t)
        }

        val walkingResult = analyzer.processSample(
            pitch = slumpPitch,
            roll = 0f,
            activityState = ActivityState.WALKING,
            currentTimeMs = 301_000L,
        )

        assertEquals(AnalyzerResult.STATE_RESET, walkingResult)
    }
}
