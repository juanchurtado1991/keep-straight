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
    val titleKey: StatusCopyKey,
    val bodyKey: StatusCopyKey,
    val titleArgs: List<Any> = emptyList(),
    val bodyArgs: List<Any> = emptyList(),
    /** When set, shown instead of [bodyKey] (e.g. bridge pair error detail). */
    val bodyOverride: String? = null,
    val presenceKey: StatusCopyKey? = null,
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
        statusKey: StatusCopyKey,
        statusArgs: List<Any>,
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
                titleKey = StatusCopyKey.STATUS_CALIBRATING_ERECT,
                bodyKey = StatusCopyKey.BODY_HOLD_GOOD_SITTING,
                showProgress = true,
                secondaryAction = DesktopStatusAction.STOP_SESSION,
            )
            CalibrationPhase.CAPTURE_SLUMP -> return DesktopStatusPresentation(
                tone = DesktopStatusTone.PROGRESS,
                titleKey = StatusCopyKey.STATUS_CALIBRATING_SLUMPED,
                bodyKey = StatusCopyKey.BODY_HOLD_USUAL_SLOUCH,
                showProgress = true,
            )
            else -> Unit
        }

        return when (phase) {
            DesktopSessionPhase.IDLE -> {
                if (!hasCalibration) {
                    DesktopStatusPresentation(
                        tone = DesktopStatusTone.WARNING,
                        titleKey = StatusCopyKey.STATUS_CALIBRATION_NEEDED,
                        bodyKey = StatusCopyKey.BODY_CALIBRATE_BEFORE_MONITOR,
                        primaryAction = DesktopStatusAction.CALIBRATE_ERECT,
                    )
                } else {
                    DesktopStatusPresentation(
                        tone = DesktopStatusTone.NEUTRAL,
                        titleKey = StatusCopyKey.STATUS_READY,
                        bodyKey = statusKey,
                        bodyArgs = statusArgs,
                        presenceKey = null,
                    )
                }
            }
            DesktopSessionPhase.PAUSED -> {
                val (titleKey, bodyKey) = when (presence) {
                    PresenceState.STANDING -> StatusCopyKey.STATUS_PAUSED_STANDING to StatusCopyKey.BODY_PAUSED_STANDING
                    PresenceState.AWAY -> StatusCopyKey.STATUS_PAUSED_AWAY to StatusCopyKey.BODY_PAUSED_AWAY
                    PresenceState.LOW_CONFIDENCE ->
                        StatusCopyKey.STATUS_PAUSED_FACE_CAMERA to StatusCopyKey.BODY_PAUSED_FACE_CAMERA
                    PresenceState.SITTING ->
                        StatusCopyKey.STATUS_PAUSED to
                            if (statusKey == StatusCopyKey.BODY_START_SESSION) {
                                StatusCopyKey.BODY_MONITORING_PAUSED
                            } else {
                                statusKey
                            }
                }
                DesktopStatusPresentation(
                    tone = DesktopStatusTone.WARNING,
                    titleKey = titleKey,
                    bodyKey = bodyKey,
                    bodyArgs = if (presence == PresenceState.SITTING) statusArgs else emptyList(),
                    presenceKey = presenceKey(presence),
                    primaryAction = DesktopStatusAction.STOP_SESSION,
                )
            }
            DesktopSessionPhase.RUNNING -> {
                if (isSlumped) {
                    DesktopStatusPresentation(
                        tone = DesktopStatusTone.ERROR,
                        titleKey = StatusCopyKey.STATUS_SLOUCHING,
                        bodyKey = StatusCopyKey.BODY_SLOUCHING_SCORE,
                        bodyArgs = listOf((slumpScore * 100).toInt()),
                        presenceKey = presenceKey(presence),
                    )
                } else {
                    DesktopStatusPresentation(
                        tone = DesktopStatusTone.SUCCESS,
                        titleKey = StatusCopyKey.STATUS_LOOKING_GOOD,
                        bodyKey = StatusCopyKey.BODY_MONITORING_SITTING,
                        presenceKey = presenceKey(presence),
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
                titleKey = StatusCopyKey.STATUS_CAMERA_IN_USE,
                bodyKey = StatusCopyKey.BODY_CAMERA_IN_USE,
                primaryAction = DesktopStatusAction.RETRY_CAMERA,
                secondaryAction = DesktopStatusAction.REFRESH_CAMERAS,
            )
            CameraError.PERMISSION_DENIED -> DesktopStatusPresentation(
                tone = DesktopStatusTone.ERROR,
                titleKey = StatusCopyKey.STATUS_CAMERA_PERMISSION,
                bodyKey = StatusCopyKey.BODY_CAMERA_PERMISSION,
                primaryAction = DesktopStatusAction.RETRY_CAMERA,
                secondaryAction = DesktopStatusAction.REFRESH_CAMERAS,
            )
            CameraError.NOT_FOUND -> DesktopStatusPresentation(
                tone = DesktopStatusTone.ERROR,
                titleKey = StatusCopyKey.STATUS_NO_CAMERA,
                bodyKey = StatusCopyKey.BODY_NO_CAMERA,
                primaryAction = DesktopStatusAction.REFRESH_CAMERAS,
                secondaryAction = DesktopStatusAction.RETRY_CAMERA,
            )
            CameraError.OPEN_FAILED -> DesktopStatusPresentation(
                tone = DesktopStatusTone.ERROR,
                titleKey = StatusCopyKey.STATUS_CAMERA_OPEN_FAILED,
                bodyKey = StatusCopyKey.BODY_CAMERA_OPEN_FAILED,
                primaryAction = DesktopStatusAction.RETRY_CAMERA,
                secondaryAction = DesktopStatusAction.REFRESH_CAMERAS,
            )
            CameraError.DISCONNECTED -> DesktopStatusPresentation(
                tone = DesktopStatusTone.ERROR,
                titleKey = StatusCopyKey.STATUS_CAMERA_DISCONNECTED,
                bodyKey = StatusCopyKey.BODY_CAMERA_DISCONNECTED,
                primaryAction = DesktopStatusAction.RETRY_CAMERA,
            )
        }
        DesktopIssue.ModelMissing -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            titleKey = StatusCopyKey.STATUS_POSE_MODEL_MISSING,
            bodyKey = StatusCopyKey.BODY_POSE_MODEL_MISSING,
        )
        DesktopIssue.NeedsCalibration -> DesktopStatusPresentation(
            tone = DesktopStatusTone.WARNING,
            titleKey = StatusCopyKey.STATUS_CALIBRATION_NEEDED,
            bodyKey = StatusCopyKey.BODY_CALIBRATE_BEFORE_MONITOR,
            primaryAction = DesktopStatusAction.CALIBRATE_ERECT,
        )
        DesktopIssue.CalibrationNeedsErectFirst -> DesktopStatusPresentation(
            tone = DesktopStatusTone.WARNING,
            titleKey = StatusCopyKey.STATUS_CALIBRATE_ERECT_FIRST,
            bodyKey = StatusCopyKey.BODY_CALIBRATE_ERECT_FIRST,
            primaryAction = DesktopStatusAction.CALIBRATE_ERECT,
        )
        DesktopIssue.CalibrationPosesTooSimilar -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            titleKey = StatusCopyKey.STATUS_POSES_TOO_SIMILAR,
            bodyKey = StatusCopyKey.BODY_POSES_TOO_SIMILAR,
            primaryAction = DesktopStatusAction.CALIBRATE_ERECT,
            secondaryAction = DesktopStatusAction.CALIBRATE_SLUMP,
        )
        DesktopIssue.CalibrationNoPose -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            titleKey = StatusCopyKey.STATUS_NO_POSE,
            bodyKey = StatusCopyKey.BODY_NO_POSE_CALIBRATE,
            primaryAction = DesktopStatusAction.CALIBRATE,
        )
        is DesktopIssue.BridgePairFailed -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            titleKey = StatusCopyKey.STATUS_BRIDGE_PAIR_FAILED,
            bodyKey = StatusCopyKey.BODY_BRIDGE_PAIR_FAILED_DEFAULT,
            bodyOverride = issue.detail.takeIf { it.isNotBlank() },
            primaryAction = DesktopStatusAction.REPAIR_BRIDGE,
        )
        DesktopIssue.BridgeSendFailed -> DesktopStatusPresentation(
            tone = if (bridgeState == BridgeConnectionState.FAILED) {
                DesktopStatusTone.ERROR
            } else {
                DesktopStatusTone.WARNING
            },
            titleKey = StatusCopyKey.STATUS_BRIDGE_UNREACHABLE,
            bodyKey = StatusCopyKey.BODY_BRIDGE_UNREACHABLE,
            primaryAction = DesktopStatusAction.REPAIR_BRIDGE,
            secondaryAction = DesktopStatusAction.CLEAR_BRIDGE,
        )
        DesktopIssue.BridgeUnauthorized -> DesktopStatusPresentation(
            tone = DesktopStatusTone.ERROR,
            titleKey = StatusCopyKey.STATUS_BRIDGE_EXPIRED,
            bodyKey = StatusCopyKey.BODY_BRIDGE_EXPIRED,
            primaryAction = DesktopStatusAction.REPAIR_BRIDGE,
            secondaryAction = DesktopStatusAction.CLEAR_BRIDGE,
        )
        DesktopIssue.TooDarkOrLowConfidence -> DesktopStatusPresentation(
            tone = DesktopStatusTone.WARNING,
            titleKey = StatusCopyKey.STATUS_TOO_DARK,
            bodyKey = StatusCopyKey.BODY_TOO_DARK,
        )
    }

    private fun presenceKey(presence: PresenceState): StatusCopyKey =
        when (presence) {
            PresenceState.SITTING -> StatusCopyKey.PRESENCE_SITTING
            PresenceState.STANDING -> StatusCopyKey.PRESENCE_STANDING
            PresenceState.AWAY -> StatusCopyKey.PRESENCE_AWAY
            PresenceState.LOW_CONFIDENCE -> StatusCopyKey.PRESENCE_LOW_CONFIDENCE
        }
}
