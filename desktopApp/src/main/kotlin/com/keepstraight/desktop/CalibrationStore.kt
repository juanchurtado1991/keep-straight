package com.keepstraight.desktop

import com.keepstraight.shared.domain.LandmarkCalibration
import com.keepstraight.shared.domain.LandmarkPostureFeatures
import com.keepstraight.shared.model.SensitivityLevel
import java.util.prefs.Preferences

object CalibrationStore {
    fun load(prefs: Preferences): LandmarkCalibration? {
        if (!prefs.getBoolean("cal_valid", false)) return null
        return runCatching {
            LandmarkCalibration(
                erect = features("erect", prefs),
                slumped = features("slump", prefs),
                sensitivity = SensitivityLevel.valueOf(
                    prefs.get("sensitivity", SensitivityLevel.NORMAL.name),
                ),
                slumpDurationThresholdMs = prefs.getLong("slump_duration_ms", 30_000L),
                repeatAlertIntervalMs = prefs.getLong("repeat_alert_ms", 5_000L),
                calibratedAtMs = prefs.getLong("cal_at", 0L),
            )
        }.getOrNull()
    }

    fun save(prefs: Preferences, cal: LandmarkCalibration) {
        prefs.putBoolean("cal_valid", true)
        writeFeatures("erect", cal.erect, prefs)
        writeFeatures("slump", cal.slumped, prefs)
        prefs.put("sensitivity", cal.sensitivity.name)
        prefs.putLong("slump_duration_ms", cal.slumpDurationThresholdMs)
        prefs.putLong("repeat_alert_ms", cal.repeatAlertIntervalMs)
        prefs.putLong("cal_at", cal.calibratedAtMs)
    }

    fun clear(prefs: Preferences) {
        prefs.putBoolean("cal_valid", false)
    }

    private fun features(prefix: String, prefs: Preferences) = LandmarkPostureFeatures(
        torsoLean = prefs.getFloat("${prefix}_torso", 0f),
        headForward = prefs.getFloat("${prefix}_head", 0f),
        neckDrop = prefs.getFloat("${prefix}_neck", 0f),
        headDrop = prefs.getFloat("${prefix}_headDrop", 0f),
        hipY = prefs.getFloat("${prefix}_hipY", 0f),
        shoulderY = prefs.getFloat("${prefix}_shoulderY", 0f),
        shoulderWidth = prefs.getFloat("${prefix}_sw", 0.25f),
        meanConfidence = prefs.getFloat("${prefix}_conf", 0.9f),
    )

    private fun writeFeatures(prefix: String, f: LandmarkPostureFeatures, prefs: Preferences) {
        prefs.putFloat("${prefix}_torso", f.torsoLean)
        prefs.putFloat("${prefix}_head", f.headForward)
        prefs.putFloat("${prefix}_neck", f.neckDrop)
        prefs.putFloat("${prefix}_headDrop", f.headDrop)
        prefs.putFloat("${prefix}_hipY", f.hipY)
        prefs.putFloat("${prefix}_shoulderY", f.shoulderY)
        prefs.putFloat("${prefix}_sw", f.shoulderWidth)
        prefs.putFloat("${prefix}_conf", f.meanConfidence)
    }
}
