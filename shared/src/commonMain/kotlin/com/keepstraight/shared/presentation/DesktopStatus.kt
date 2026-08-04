package com.keepstraight.shared.presentation

import com.keepstraight.shared.domain.CalibrationPhase
import com.keepstraight.shared.domain.DesktopSessionPhase
import com.keepstraight.shared.domain.PresenceState
import com.keepstraight.shared.vision.CameraError

/** Visual tone — mirrors phone StatusTone. */
enum class DesktopStatusTone {
    NEUTRAL,
    PROGRESS,
    SUCCESS,
    WARNING,
    ERROR,
}

/**
 * First-class desktop issues (not free-form strings).
 * UI maps these to title/body/actions like phone calibration/reconnect flows.
 */
sealed class DesktopIssue {
    data class Camera(val error: CameraError) : DesktopIssue()
    data object ModelMissing : DesktopIssue()
    data object NeedsCalibration : DesktopIssue()
    data object CalibrationNeedsErectFirst : DesktopIssue()
    data object CalibrationPosesTooSimilar : DesktopIssue()
    data object CalibrationNoPose : DesktopIssue()
    data class BridgePairFailed(val detail: String) : DesktopIssue()
    data object BridgeSendFailed : DesktopIssue()
    data object BridgeUnauthorized : DesktopIssue()
    data object TooDarkOrLowConfidence : DesktopIssue()
}

enum class BridgeConnectionState {
    NOT_CONFIGURED,
    PAIRED,
    DEGRADED,
    FAILED,
}

data class DesktopStatusPresentation(
    val tone: DesktopStatusTone,
    val title: String,
    val body: String,
    val presenceLabel: String? = null,
    val primaryAction: DesktopStatusAction? = null,
    val secondaryAction: DesktopStatusAction? = null,
    val showProgress: Boolean = false,
)

enum class DesktopStatusAction {
    RETRY_CAMERA,
    REFRESH_CAMERAS,
    CALIBRATE,
    CALIBRATE_ERECT,
    CALIBRATE_SLUMP,
    STOP_SESSION,
    CLEAR_BRIDGE,
    REPAIR_BRIDGE,
}

object DesktopStatusMapper {
    fun present(
        phase: DesktopSessionPhase,
        presence: PresenceState,
        calibrationPhase: CalibrationPhase,
        hasCalibration: Boolean,
        isSlumped: Boolean,
        slumpScore: Float,
        statusMessage: String,
        issue: DesktopIssue?,
        bridgeState: BridgeConnectionState,
        modelReady: Boolean,
    ): DesktopStatusPresentation {
        issue?.let { return fromIssue(it, bridgeState) }

        if (!modelReady) {
            return fromIssue(DesktopIssue.ModelMissing, bridgeState)
        }

        when (calibrationPhase) {
            CalibrationPhase.CAPTURE_ERECT -> return DesktopStatusPresentation(
                tone = DesktopStatusTone.PROGRESS,
                title = "Calibrating — erect",
                body = "Hold good sitting posture…",
                showProgress = true,
                secondaryAction = DesktopStatusAction.STOP_SESSION,
            )
            CalibrationPhase.CAPTURE_SLUMP -> return DesktopStatusPresentation(
                tone = DesktopStatusTone.PROGRESS,
                title = "Calibrating — slumped",
                body = "Hold your usual slouch…",
                showProgress = true,
            )
            else -> Unit
        }

        return when (phase) {
            DesktopSessionPhase.IDLE -> {
                if (!hasCalibration) {
                    DesktopStatusPresentation(
                        tone = DesktopStatusTone.WARNING,
                        title = "Calibration needed",
                        body = "Calibrate erect, then slumped, before monitoring.",
                        primaryAction = DesktopStatusAction.CALIBRATE_ERECT,
                    )
                } else {
                    DesktopStatusPresentation(
                        tone = DesktopStatusTone.NEUTRAL,
                        title = "Ready",
                        body = statusMessage.ifBlank { "Start a session to monitor posture." },
                        presenceLabel = null,
                    )
                }
            }
            DesktopSessionPhase.PAUSED -> {
                val (title, body, tone) = when (presence) {
                    PresenceState.STANDING -> Triple(
                        "Paused — Standing",
                        "Standing desk posture is not monitored in v1.",
                        DesktopStatusTone.WARNING,
                    )
                    PresenceState.AWAY -> Triple(
                        "Paused — Away",
                        "No person detected. Sit in frame facing the camera.",
                        DesktopStatusTone.WARNING,
                    )
                    PresenceState.LOW_CONFIDENCE -> Triple(
                        "Paused — Face the camera",
                        "Pose is unclear (dark room, profile, or low confidence).",
                        DesktopStatusTone.WARNING,
                    )
                    PresenceState.SITTING -> Triple(
                        "Paused",
                        statusMessage.ifBlank { "Monitoring paused." },
                        DesktopStatusTone.WARNING,
                    )
                }
                DesktopStatusPresentation(
                    tone = tone,
                    title = title,
                    body = body,
                    presenceLabel = presenceLabel(presence),
                    primaryAction = DesktopStatusAction.STOP_SESSION,
                )
            }
            DesktopSessionPhase.RUNNING -> {
                if (isSlumped) {
                    DesktopStatusPresentation(
                        tone = DesktopStatusTone.ERROR,
                        title = "Slouching",
                        body = "Sit up straight. Score ${(slumpScore * 100).toInt()}%.",
                        presenceLabel = presenceLabel(presence),
                    )
                } else {
                    DesktopStatusPresentation(
                        tone = DesktopStatusTone.SUCCESS,
                        title = "Looking good",
                        body = "Monitoring sitting posture.",
                        presenceLabel = presenceLabel(presence),
                    )
                }
            }
        }
    }

    private fun fromIssue(
        issue: DesktopIssue,
        bridgeState: BridgeConnectionState,
    ): DesktopStatusPresentation = when (issue) {
        is DesktopIssue.Camera -> when (issue.error) {
            CameraError.IN_USE -> DesktopStatusPresentation(
                tone = DesktopStatusTone.ERROR,
                title = "Camera in use",
                body = "Another app (Zoom/Meet/Teams) is using the camera. Close it, then retry.",
                primaryAction = DesktopStatusAction.RETRY_CAMERA,
                secondaryAction = DesktopStatusAction.REFRESH_CAMERAS,
            )
            CameraError.PERMISSION_DENIED -> DesktopStatusPresentation(
                tone = DesktopStatusTone.ERROR,
                title = "Camera permission needed",
                body = "Allow KeepStraight (or Terminal / your IDE) to use the camera, then quit and reopen:\n" +
                    "• Mac: System Settings → Privacy & Security → Camera\n" +
                    "• Windows: Settings → Privacy & security → Camera → let desktop apps access your camera\n" +
                    "• Linux: make sure your user can read /dev/video* (often the “video” group)",
                primaryAction = DesktopStatusAction.RETRY_CAMERA,
                secondaryAction = DesktopStatusAction.REFRESH_CAMERAS,
            )
            CameraError.NOT_FOUND -> DesktopStatusPresentation(
                tone = DesktopStatusTone.ERROR,
                title = "No camera found",
                body = "Plug in a webcam or pick another device, then refresh.\n" +
                    "• Mac / Windows: also check the OS camera privacy toggle\n" +
                    "• Linux: confirm a V4L2 device exists (`ls /dev/video*`)",
                primaryAction = DesktopStatusAction.REFRESH_CAMERAS,
                secondaryAction = DesktopStatusAction.RETRY_CAMERA,
            )
            CameraError.OPEN_FAILED -> DesktopStatusPresentation(
                tone = DesktopStatusTone.ERROR,
                title = "Could not open camera",
                body = "The camera failed to open. Try another device or restart the app.\n" +
                    "On Linux ARM (aarch64), native webcam drivers aren’t bundled yet — use an x86_64 build if you can.",
                primaryAction = DesktopStatusAction.RETRY_CAMERA,
                secondaryAction = DesktopStatusAction.REFRESH_CAMERAS,
            )
            CameraError.DISCONNECTED -> DesktopStatusPresentation(
                tone = DesktopStatusTone.ERROR,
                title = "Camera disconnected",
                body = "The webcam was disconnected. Reconnect it and retry.",
                primaryAction = DesktopStatusAction.RETRY_CAMERA,
            )
        }
        DesktopIssue.ModelMissing -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            title = "Pose model missing",
            body = "Run desktopApp/scripts/download-movenet.sh, then restart KeepStraight.",
        )
        DesktopIssue.NeedsCalibration -> DesktopStatusPresentation(
            tone = DesktopStatusTone.WARNING,
            title = "Calibration needed",
            body = "Calibrate erect, then slumped, before monitoring.",
            primaryAction = DesktopStatusAction.CALIBRATE_ERECT,
        )
        DesktopIssue.CalibrationNeedsErectFirst -> DesktopStatusPresentation(
            tone = DesktopStatusTone.WARNING,
            title = "Calibrate erect first",
            body = "Capture your good sitting posture before the slumped pose.",
            primaryAction = DesktopStatusAction.CALIBRATE_ERECT,
        )
        DesktopIssue.CalibrationPosesTooSimilar -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            title = "Poses too similar",
            body = "Sit up clearly, then slouch more (round shoulders / lean forward) and retry.",
            primaryAction = DesktopStatusAction.CALIBRATE_ERECT,
            secondaryAction = DesktopStatusAction.CALIBRATE_SLUMP,
        )
        DesktopIssue.CalibrationNoPose -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            title = "No pose detected",
            body = "Sit facing the camera with good light, then try calibration again.",
            primaryAction = DesktopStatusAction.CALIBRATE,
        )
        is DesktopIssue.BridgePairFailed -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            title = "Phone pairing failed",
            body = issue.detail.ifBlank { "Check Wi‑Fi, phone IP, and the one-time code." },
            primaryAction = DesktopStatusAction.REPAIR_BRIDGE,
        )
        DesktopIssue.BridgeSendFailed -> DesktopStatusPresentation(
            tone = if (bridgeState == BridgeConnectionState.FAILED) {
                DesktopStatusTone.ERROR
            } else {
                DesktopStatusTone.WARNING
            },
            title = "Phone bridge unreachable",
            body = "Desktop alerts still work. Watch/history sync failed — check Wi‑Fi or re-pair.",
            primaryAction = DesktopStatusAction.REPAIR_BRIDGE,
            secondaryAction = DesktopStatusAction.CLEAR_BRIDGE,
        )
        DesktopIssue.BridgeUnauthorized -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            title = "Phone bridge expired",
            body = "Generate a new code on the phone and pair again.",
            primaryAction = DesktopStatusAction.REPAIR_BRIDGE,
            secondaryAction = DesktopStatusAction.CLEAR_BRIDGE,
        )
        DesktopIssue.TooDarkOrLowConfidence -> DesktopStatusPresentation(
            tone = DesktopStatusTone.WARNING,
            title = "Too dark / unclear",
            body = "Improve lighting or face the camera. Alerts are paused.",
        )
    }

    private fun presenceLabel(presence: PresenceState): String =
        when (presence) {
            PresenceState.SITTING -> "Sitting"
            PresenceState.STANDING -> "Standing"
            PresenceState.AWAY -> "Away"
            PresenceState.LOW_CONFIDENCE -> "Low confidence"
        }
}
