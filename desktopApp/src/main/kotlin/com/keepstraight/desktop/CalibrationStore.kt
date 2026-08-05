package com.keepstraight.desktop

import com.keepstraight.desktop.presentation.CalibrationPrefsKeys
import com.keepstraight.desktop.presentation.DesktopPrefsKeys
import com.keepstraight.shared.domain.LandmarkCalibration
import com.keepstraight.shared.domain.LandmarkPostureFeatures
import com.keepstraight.shared.model.SensitivityLevel
import java.util.prefs.Preferences

object CalibrationStore {
    fun load(prefs: Preferences): LandmarkCalibration? {
        if (!prefs.getBoolean(CalibrationPrefsKeys.VALID, false)) return null
        return runCatching {
            LandmarkCalibration(
                erect = features(CalibrationPrefsKeys.PREFIX_ERECT, prefs),
                slumped = features(CalibrationPrefsKeys.PREFIX_SLUMP, prefs),
                sensitivity = SensitivityLevel.valueOf(
                    prefs.get(DesktopPrefsKeys.SENSITIVITY, SensitivityLevel.NORMAL.name),
                ),
                slumpDurationThresholdMs = prefs.getLong(DesktopPrefsKeys.SLUMP_DURATION_MS, 30_000L),
                repeatAlertIntervalMs = prefs.getLong(DesktopPrefsKeys.REPEAT_ALERT_MS, 5_000L),
                calibratedAtMs = prefs.getLong(CalibrationPrefsKeys.AT_MS, 0L),
            )
        }.getOrNull()
    }

    fun save(prefs: Preferences, cal: LandmarkCalibration) {
        prefs.putBoolean(CalibrationPrefsKeys.VALID, true)
        writeFeatures(CalibrationPrefsKeys.PREFIX_ERECT, cal.erect, prefs)
        writeFeatures(CalibrationPrefsKeys.PREFIX_SLUMP, cal.slumped, prefs)
        prefs.put(DesktopPrefsKeys.SENSITIVITY, cal.sensitivity.name)
        prefs.putLong(DesktopPrefsKeys.SLUMP_DURATION_MS, cal.slumpDurationThresholdMs)
        prefs.putLong(DesktopPrefsKeys.REPEAT_ALERT_MS, cal.repeatAlertIntervalMs)
        prefs.putLong(CalibrationPrefsKeys.AT_MS, cal.calibratedAtMs)
    }

    fun clear(prefs: Preferences) {
        prefs.putBoolean(CalibrationPrefsKeys.VALID, false)
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
