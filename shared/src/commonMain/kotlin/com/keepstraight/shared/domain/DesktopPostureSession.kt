package com.keepstraight.shared.domain

import com.keepstraight.shared.model.SensitivityLevel
import com.keepstraight.shared.platform.currentTimeMillis
import com.keepstraight.shared.presentation.DesktopIssue
import com.keepstraight.shared.vision.BodyPose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DesktopSessionPhase {
    IDLE,
    RUNNING,
    PAUSED,
}

enum class CalibrationPhase {
    NONE,
    CAPTURE_ERECT,
    CAPTURE_SLUMP,
    COMPLETE,
}

data class DesktopSessionUiState(
    val phase: DesktopSessionPhase = DesktopSessionPhase.IDLE,
    val presence: PresenceState = PresenceState.AWAY,
    val slumpScore: Float = 0f,
    val isSlumped: Boolean = false,
    val slumpElapsedMs: Long = 0L,
    val calibrationPhase: CalibrationPhase = CalibrationPhase.NONE,
    val hasCalibration: Boolean = false,
    val hasErectCapture: Boolean = false,
    val lastAlertAtMs: Long = 0L,
    val statusMessage: String = "Start a session to monitor posture.",
    val sensitivity: SensitivityLevel = SensitivityLevel.NORMAL,
    val slumpDurationThresholdMs: Long = 30_000L,
    val repeatAlertIntervalMs: Long = 5_000L,
    val settingsFromPhone: Boolean = false,
    val issue: DesktopIssue? = null,
    val modelReady: Boolean = true,
)

enum class DesktopAlertEvent {
    SLUMP_INITIAL,
    SLUMP_REPEAT,
}

/**
 * Desktop posture session: landmarks → presence → dual-pose slump timers → alerts.
 */
class DesktopPostureSession {
    private val _ui = MutableStateFlow(DesktopSessionUiState())
    val uiState: StateFlow<DesktopSessionUiState> = _ui.asStateFlow()

    private var calibration: LandmarkCalibration? = null
    private var erectSamples = mutableListOf<LandmarkPostureFeatures>()
    private var slumpSamples = mutableListOf<LandmarkPostureFeatures>()
    private var calibrationStartedAt = 0L
    private var slumpStartedAt = 0L
    private var lastGoodPoseAt = 0L
    private var lastAlertAt = 0L
    private var wasSlumped = false

    private var pendingSlumpDurationMs = 30_000L
    private var pendingRepeatAlertMs = 5_000L

    private var lastWorkTickAtMs = 0L
    private var pendingSeatedMs = 0L
    private var pendingGoodPostureMs = 0L

    var onAlert: ((DesktopAlertEvent) -> Unit)? = null
    var onCalibrationSaved: ((LandmarkCalibration) -> Unit)? = null

    fun currentCalibration(): LandmarkCalibration? = calibration

    fun updateSensitivity(level: SensitivityLevel) {
        calibration = calibration?.copy(sensitivity = level)
        _ui.value = _ui.value.copy(sensitivity = level)
    }

    fun updateTimers(slumpDurationThresholdMs: Long, repeatAlertIntervalMs: Long) {
        pendingSlumpDurationMs = slumpDurationThresholdMs
        pendingRepeatAlertMs = repeatAlertIntervalMs
        calibration = calibration?.copy(
            slumpDurationThresholdMs = slumpDurationThresholdMs,
            repeatAlertIntervalMs = repeatAlertIntervalMs,
        )
        _ui.value = _ui.value.copy(
            slumpDurationThresholdMs = slumpDurationThresholdMs,
            repeatAlertIntervalMs = repeatAlertIntervalMs,
        )
    }

    /** Apply phone-authoritative sensitivity + timers (phase 2 bridge). */
    fun applyPhoneSettings(
        sensitivity: SensitivityLevel,
        slumpDurationThresholdMs: Long,
        repeatAlertIntervalMs: Long,
    ) {
        updateSensitivity(sensitivity)
        updateTimers(slumpDurationThresholdMs, repeatAlertIntervalMs)
        _ui.value = _ui.value.copy(settingsFromPhone = true)
    }

    fun clearPhoneSettingsFlag() {
        _ui.value = _ui.value.copy(settingsFromPhone = false)
    }

    fun setModelReady(ready: Boolean) {
        _ui.value = _ui.value.copy(
            modelReady = ready,
            issue = if (!ready) DesktopIssue.ModelMissing else clearModelIssue(_ui.value.issue),
        )
    }

    fun setIssue(issue: DesktopIssue?) {
        _ui.value = _ui.value.copy(issue = issue)
    }

    fun clearIssue() {
        if (_ui.value.issue != null) {
            _ui.value = _ui.value.copy(issue = null)
        }
    }

    fun setCalibration(cal: LandmarkCalibration) {
        calibration = cal.copy(
            slumpDurationThresholdMs = pendingSlumpDurationMs,
            repeatAlertIntervalMs = pendingRepeatAlertMs,
        )
        erectSamples.clear()
        slumpSamples.clear()
        _ui.value = _ui.value.copy(
            hasCalibration = LandmarkPostureScorer.hasUsableCalibration(cal),
            hasErectCapture = true,
            calibrationPhase = CalibrationPhase.COMPLETE,
            sensitivity = cal.sensitivity,
            slumpDurationThresholdMs = pendingSlumpDurationMs,
            repeatAlertIntervalMs = pendingRepeatAlertMs,
            issue = null,
            statusMessage = "Calibration loaded.",
        )
    }

    fun startSession(): Boolean {
        if (!_ui.value.modelReady) {
            _ui.value = _ui.value.copy(issue = DesktopIssue.ModelMissing)
            return false
        }
        if (!_ui.value.hasCalibration) {
            _ui.value = _ui.value.copy(issue = DesktopIssue.NeedsCalibration)
            return false
        }
        slumpStartedAt = 0L
        lastAlertAt = 0L
        wasSlumped = false
        _ui.value = _ui.value.copy(
            phase = DesktopSessionPhase.RUNNING,
            issue = null,
            statusMessage = "Monitoring posture.",
        )
        return true
    }

    fun stopSession() {
        slumpStartedAt = 0L
        wasSlumped = false
        _ui.value = _ui.value.copy(
            phase = DesktopSessionPhase.IDLE,
            isSlumped = false,
            slumpElapsedMs = 0L,
            presence = PresenceState.AWAY,
            statusMessage = "Session stopped.",
            issue = null,
        )
    }

    fun beginErectCalibration(): Boolean {
        if (!_ui.value.modelReady) {
            _ui.value = _ui.value.copy(issue = DesktopIssue.ModelMissing)
            return false
        }
        erectSamples.clear()
        slumpSamples.clear()
        // Mid dual-flow marker: UI must show "capture slumped" after erect even when an older
        // calibration is still loaded (hasCalibration stays true until both poses finish).
        // Start timer on first usable sample so frame/clock skew cannot stall capture.
        calibrationStartedAt = -1L
        _ui.value = _ui.value.copy(
            calibrationPhase = CalibrationPhase.CAPTURE_ERECT,
            hasErectCapture = false,
            statusMessage = "Hold good posture…",
            issue = null,
        )
        return true
    }

    fun beginSlumpCalibration(): Boolean {
        if (!_ui.value.modelReady) {
            _ui.value = _ui.value.copy(issue = DesktopIssue.ModelMissing)
            return false
        }
        if (erectSamples.isEmpty() && calibration == null && !_ui.value.hasErectCapture) {
            _ui.value = _ui.value.copy(issue = DesktopIssue.CalibrationNeedsErectFirst)
            return false
        }
        // Prefer freshly captured erect samples; otherwise keep previous erect from calibration.
        if (erectSamples.isEmpty() && calibration != null) {
            erectSamples += calibration!!.erect
        }
        if (erectSamples.isEmpty()) {
            _ui.value = _ui.value.copy(issue = DesktopIssue.CalibrationNeedsErectFirst)
            return false
        }
        slumpSamples.clear()
        calibrationStartedAt = -1L
        _ui.value = _ui.value.copy(
            calibrationPhase = CalibrationPhase.CAPTURE_SLUMP,
            statusMessage = "Hold your slumped posture…",
            issue = null,
        )
        return true
    }

    fun cancelCalibration() {
        val wasCapturing = _ui.value.calibrationPhase == CalibrationPhase.CAPTURE_ERECT ||
            _ui.value.calibrationPhase == CalibrationPhase.CAPTURE_SLUMP
        _ui.value = _ui.value.copy(
            calibrationPhase = if (_ui.value.hasCalibration) {
                CalibrationPhase.COMPLETE
            } else {
                CalibrationPhase.NONE
            },
            statusMessage = when {
                !wasCapturing -> _ui.value.statusMessage
                _ui.value.hasCalibration -> "Monitoring ready."
                else -> "Calibration cancelled."
            },
            issue = null,
        )
    }

    /**
     * Feed a pose sample. Returns an alert event when thresholds fire.
     */
    fun onPose(pose: BodyPose?, nowMs: Long = currentTimeMillis()): DesktopAlertEvent? {
        val features = pose?.let { LandmarkPostureScorer.extractFeatures(it) }
        if (features != null) {
            lastGoodPoseAt = nowMs
            handleCalibrationSample(features, nowMs)
        } else if (
            _ui.value.calibrationPhase == CalibrationPhase.CAPTURE_ERECT ||
            _ui.value.calibrationPhase == CalibrationPhase.CAPTURE_SLUMP
        ) {
            if (calibrationStartedAt < 0L) {
                calibrationStartedAt = nowMs
            }
            // After a few seconds with no usable upper-body pose, leave capture
            // mode so the UI does not spin on "Hold still…" forever.
            if (nowMs - calibrationStartedAt >= 4_000L) {
                _ui.value = _ui.value.copy(
                    calibrationPhase = CalibrationPhase.NONE,
                    issue = DesktopIssue.CalibrationNoPose,
                    statusMessage = "No pose detected. Face the camera with good light.",
                )
            }
        }

        val millisSinceGood = if (lastGoodPoseAt == 0L) Long.MAX_VALUE else nowMs - lastGoodPoseAt
        val presence = PresenceClassifier.classify(
            pose = pose,
            features = features,
            calibration = calibration,
            millisSinceLastGoodPose = millisSinceGood,
        )

        val cal = calibration
        val score = if (features != null && cal != null) {
            LandmarkPostureScorer.directedSlumpScore(features, cal) ?: 0f
        } else {
            0f
        }

        val running = _ui.value.phase == DesktopSessionPhase.RUNNING ||
            _ui.value.phase == DesktopSessionPhase.PAUSED
        if (!running) {
            _ui.value = _ui.value.copy(
                presence = presence,
                slumpScore = score,
            )
            return null
        }

        // Camera/model hard issues stay sticky until cleared.
        val stickyIssue = _ui.value.issue
        if (stickyIssue is DesktopIssue.Camera || stickyIssue is DesktopIssue.ModelMissing) {
            return null
        }

        if (!PresenceClassifier.shouldRunSlumpTimers(presence) || cal == null || features == null) {
            // Standing / away clear the timer. Brief low-confidence frames only freeze it so a
            // mid-slump dark flicker doesn't restart the whole delay.
            if (presence != PresenceState.LOW_CONFIDENCE) {
                slumpStartedAt = 0L
                wasSlumped = false
            }
            val pauseMsg = when (presence) {
                PresenceState.STANDING -> "Paused — Standing"
                PresenceState.AWAY -> "Paused — Away"
                PresenceState.LOW_CONFIDENCE -> "Paused — Face the camera"
                PresenceState.SITTING -> "Calibrate erect and slumped poses first."
            }
            val presenceIssue = when (presence) {
                PresenceState.LOW_CONFIDENCE -> DesktopIssue.TooDarkOrLowConfidence
                else -> null
            }
            val elapsed = if (slumpStartedAt > 0L && presence == PresenceState.LOW_CONFIDENCE) {
                nowMs - slumpStartedAt
            } else {
                0L
            }
            _ui.value = _ui.value.copy(
                phase = DesktopSessionPhase.PAUSED,
                presence = presence,
                slumpScore = score,
                isSlumped = presence == PresenceState.LOW_CONFIDENCE && wasSlumped,
                slumpElapsedMs = elapsed,
                statusMessage = pauseMsg,
                issue = presenceIssue,
            )
            return null
        }

        val slumped = LandmarkPostureScorer.isSlumped(features, cal)
        var alert: DesktopAlertEvent? = null

        if (slumped) {
            if (slumpStartedAt == 0L) slumpStartedAt = nowMs
            val elapsed = nowMs - slumpStartedAt
            val threshold = cal.slumpDurationThresholdMs
            val repeat = cal.repeatAlertIntervalMs
            if (elapsed >= threshold) {
                if (!wasSlumped || lastAlertAt == 0L) {
                    alert = DesktopAlertEvent.SLUMP_INITIAL
                    lastAlertAt = nowMs
                    wasSlumped = true
                } else if (nowMs - lastAlertAt >= repeat) {
                    alert = DesktopAlertEvent.SLUMP_REPEAT
                    lastAlertAt = nowMs
                }
            }
            accumulateWorkSample(nowMs = nowMs, seated = true, goodPosture = false)
            _ui.value = _ui.value.copy(
                phase = DesktopSessionPhase.RUNNING,
                presence = presence,
                slumpScore = score,
                isSlumped = true,
                slumpElapsedMs = elapsed,
                lastAlertAtMs = lastAlertAt,
                statusMessage = if (elapsed >= threshold) "Slouching — sit up" else "Monitoring posture.",
                hasCalibration = true,
                issue = null,
            )
        } else {
            slumpStartedAt = 0L
            wasSlumped = false
            accumulateWorkSample(nowMs = nowMs, seated = true, goodPosture = true)
            _ui.value = _ui.value.copy(
                phase = DesktopSessionPhase.RUNNING,
                presence = presence,
                slumpScore = score,
                isSlumped = false,
                slumpElapsedMs = 0L,
                statusMessage = "Looking good.",
                hasCalibration = true,
                issue = null,
            )
        }

        if (alert != null) onAlert?.invoke(alert)
        return alert
    }

    /**
     * Drain accumulated seated / good-posture time (seconds) since the last drain.
     * Returns null when there is nothing meaningful to send.
     */
    fun drainWorkSample(): Pair<Int, Int>? {
        val seatedSec = (pendingSeatedMs / 1000L).toInt()
        val goodSec = (pendingGoodPostureMs / 1000L).toInt()
        if (seatedSec <= 0 && goodSec <= 0) return null
        pendingSeatedMs -= seatedSec * 1000L
        pendingGoodPostureMs -= goodSec * 1000L
        return seatedSec to goodSec.coerceAtMost(seatedSec)
    }

    /** Put seconds back after a failed network send. */
    fun restoreWorkSample(seatedSec: Int, goodPostureSec: Int) {
        if (seatedSec > 0) pendingSeatedMs += seatedSec * 1000L
        if (goodPostureSec > 0) pendingGoodPostureMs += goodPostureSec * 1000L
    }

    private fun accumulateWorkSample(nowMs: Long, seated: Boolean, goodPosture: Boolean) {
        if (!seated) {
            lastWorkTickAtMs = nowMs
            return
        }
        if (lastWorkTickAtMs > 0L) {
            val dt = nowMs - lastWorkTickAtMs
            if (dt in 1..5_000L) {
                pendingSeatedMs += dt
                if (goodPosture) pendingGoodPostureMs += dt
            }
        }
        lastWorkTickAtMs = nowMs
    }

    fun onCameraError(error: com.keepstraight.shared.vision.CameraError) {
        slumpStartedAt = 0L
        val capturing = _ui.value.calibrationPhase == CalibrationPhase.CAPTURE_ERECT ||
            _ui.value.calibrationPhase == CalibrationPhase.CAPTURE_SLUMP
        _ui.value = _ui.value.copy(
            phase = if (_ui.value.phase == DesktopSessionPhase.IDLE || capturing) {
                DesktopSessionPhase.IDLE
            } else {
                DesktopSessionPhase.PAUSED
            },
            // Leave capture mode so Cancel doesn't stick when the camera fails.
            calibrationPhase = if (capturing) {
                if (_ui.value.hasCalibration) CalibrationPhase.COMPLETE else CalibrationPhase.NONE
            } else {
                _ui.value.calibrationPhase
            },
            issue = DesktopIssue.Camera(error),
            statusMessage = "Camera error",
            isSlumped = false,
            slumpElapsedMs = 0L,
        )
    }

    fun onCameraRecovered() {
        if (_ui.value.issue is DesktopIssue.Camera) {
            _ui.value = _ui.value.copy(
                issue = null,
                statusMessage = when (_ui.value.phase) {
                    DesktopSessionPhase.RUNNING -> "Monitoring posture."
                    DesktopSessionPhase.PAUSED -> "Camera ready."
                    DesktopSessionPhase.IDLE -> "Camera ready."
                },
            )
        }
    }

    private fun handleCalibrationSample(features: LandmarkPostureFeatures, nowMs: Long) {
        val phase = _ui.value.calibrationPhase
        val captureMs = 1_200L
        val minSamples = 3
        when (phase) {
            CalibrationPhase.CAPTURE_ERECT -> {
                if (calibrationStartedAt < 0L) calibrationStartedAt = nowMs
                if (_ui.value.issue is DesktopIssue.CalibrationNoPose) {
                    _ui.value = _ui.value.copy(issue = null)
                }
                erectSamples += features
                val elapsed = nowMs - calibrationStartedAt
                _ui.value = _ui.value.copy(
                    statusMessage = "Hold still… ${erectSamples.size.coerceAtMost(minSamples)}/$minSamples",
                )
                if (elapsed >= captureMs && erectSamples.size >= minSamples) {
                    _ui.value = _ui.value.copy(
                        calibrationPhase = CalibrationPhase.NONE,
                        hasErectCapture = true,
                        statusMessage = "Erect pose captured. Next: calibrate slumped.",
                        issue = null,
                    )
                }
            }
            CalibrationPhase.CAPTURE_SLUMP -> {
                if (calibrationStartedAt < 0L) calibrationStartedAt = nowMs
                if (_ui.value.issue is DesktopIssue.CalibrationNoPose) {
                    _ui.value = _ui.value.copy(issue = null)
                }
                slumpSamples += features
                val elapsed = nowMs - calibrationStartedAt
                _ui.value = _ui.value.copy(
                    statusMessage = "Hold still… ${slumpSamples.size.coerceAtMost(minSamples)}/$minSamples",
                )
                if (elapsed >= captureMs && slumpSamples.size >= minSamples) {
                    finishDualCalibration(nowMs)
                }
            }
            else -> Unit
        }
    }

    private fun finishDualCalibration(nowMs: Long) {
        val erect = averageFeatures(erectSamples)
        val slumped = averageFeatures(slumpSamples)
        if (erect == null) {
            _ui.value = _ui.value.copy(
                calibrationPhase = CalibrationPhase.NONE,
                issue = DesktopIssue.CalibrationNeedsErectFirst,
            )
            return
        }
        if (slumped == null) {
            _ui.value = _ui.value.copy(
                calibrationPhase = CalibrationPhase.NONE,
                issue = DesktopIssue.CalibrationNoPose,
            )
            return
        }
        val cal = LandmarkCalibration(
            erect = erect,
            slumped = slumped,
            sensitivity = _ui.value.sensitivity,
            slumpDurationThresholdMs = pendingSlumpDurationMs,
            repeatAlertIntervalMs = pendingRepeatAlertMs,
            calibratedAtMs = nowMs,
        )
        if (!LandmarkPostureScorer.hasUsableCalibration(cal)) {
            _ui.value = _ui.value.copy(
                calibrationPhase = CalibrationPhase.NONE,
                issue = DesktopIssue.CalibrationPosesTooSimilar,
                statusMessage = "Poses too similar.",
            )
            return
        }
        calibration = cal
        onCalibrationSaved?.invoke(cal)
        _ui.value = _ui.value.copy(
            calibrationPhase = CalibrationPhase.COMPLETE,
            hasCalibration = true,
            hasErectCapture = true,
            statusMessage = "Calibration complete.",
            issue = null,
        )
    }

    private fun averageFeatures(samples: List<LandmarkPostureFeatures>): LandmarkPostureFeatures? {
        if (samples.isEmpty()) return null
        fun avg(sel: (LandmarkPostureFeatures) -> Float): Float =
            samples.map(sel).average().toFloat()
        return LandmarkPostureFeatures(
            torsoLean = avg { it.torsoLean },
            headForward = avg { it.headForward },
            neckDrop = avg { it.neckDrop },
            headDrop = avg { it.headDrop },
            hipY = avg { it.hipY },
            shoulderY = avg { it.shoulderY },
            shoulderWidth = avg { it.shoulderWidth },
            meanConfidence = avg { it.meanConfidence },
        )
    }

    private fun clearModelIssue(issue: DesktopIssue?): DesktopIssue? =
        if (issue is DesktopIssue.ModelMissing) null else issue
}
