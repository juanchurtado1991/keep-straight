package com.keepstraight.desktop.ui.i18n

import androidx.compose.runtime.Composable
import com.keepstraight.desktop.generated.resources.Res
import com.keepstraight.desktop.generated.resources.body_bridge_expired
import com.keepstraight.desktop.generated.resources.body_bridge_pair_failed_default
import com.keepstraight.desktop.generated.resources.body_bridge_unreachable
import com.keepstraight.desktop.generated.resources.body_calibrate_before_monitor
import com.keepstraight.desktop.generated.resources.body_calibrate_erect_first
import com.keepstraight.desktop.generated.resources.body_camera_disconnected
import com.keepstraight.desktop.generated.resources.body_camera_in_use
import com.keepstraight.desktop.generated.resources.body_camera_open_failed
import com.keepstraight.desktop.generated.resources.body_camera_permission
import com.keepstraight.desktop.generated.resources.body_hold_good_sitting
import com.keepstraight.desktop.generated.resources.body_hold_usual_slouch
import com.keepstraight.desktop.generated.resources.body_monitoring_paused
import com.keepstraight.desktop.generated.resources.body_monitoring_sitting
import com.keepstraight.desktop.generated.resources.body_no_camera
import com.keepstraight.desktop.generated.resources.body_no_pose_calibrate
import com.keepstraight.desktop.generated.resources.body_paused_away
import com.keepstraight.desktop.generated.resources.body_paused_face_camera
import com.keepstraight.desktop.generated.resources.body_paused_standing
import com.keepstraight.desktop.generated.resources.body_pose_model_missing
import com.keepstraight.desktop.generated.resources.body_poses_too_similar
import com.keepstraight.desktop.generated.resources.body_slouching_score
import com.keepstraight.desktop.generated.resources.body_start_session
import com.keepstraight.desktop.generated.resources.body_too_dark
import com.keepstraight.desktop.generated.resources.presence_away
import com.keepstraight.desktop.generated.resources.presence_low_confidence
import com.keepstraight.desktop.generated.resources.presence_sitting
import com.keepstraight.desktop.generated.resources.presence_standing
import com.keepstraight.desktop.generated.resources.session_calibrate_erect_first
import com.keepstraight.desktop.generated.resources.session_calibration_cancelled
import com.keepstraight.desktop.generated.resources.session_calibration_complete
import com.keepstraight.desktop.generated.resources.session_calibration_loaded
import com.keepstraight.desktop.generated.resources.session_camera_error
import com.keepstraight.desktop.generated.resources.session_camera_ready
import com.keepstraight.desktop.generated.resources.session_erect_captured
import com.keepstraight.desktop.generated.resources.session_hold_good_posture
import com.keepstraight.desktop.generated.resources.session_hold_slumped_posture
import com.keepstraight.desktop.generated.resources.session_hold_still_progress
import com.keepstraight.desktop.generated.resources.session_looking_good
import com.keepstraight.desktop.generated.resources.session_monitoring_posture
import com.keepstraight.desktop.generated.resources.session_monitoring_ready
import com.keepstraight.desktop.generated.resources.session_no_pose_detected
import com.keepstraight.desktop.generated.resources.session_poses_too_similar
import com.keepstraight.desktop.generated.resources.session_slouching_sit_up
import com.keepstraight.desktop.generated.resources.session_stopped
import com.keepstraight.desktop.generated.resources.status_bridge_expired
import com.keepstraight.desktop.generated.resources.status_bridge_pair_failed
import com.keepstraight.desktop.generated.resources.status_bridge_unreachable
import com.keepstraight.desktop.generated.resources.status_calibrate_erect_first
import com.keepstraight.desktop.generated.resources.status_calibrating_erect
import com.keepstraight.desktop.generated.resources.status_calibrating_slumped
import com.keepstraight.desktop.generated.resources.status_calibration_needed
import com.keepstraight.desktop.generated.resources.status_camera_disconnected
import com.keepstraight.desktop.generated.resources.status_camera_in_use
import com.keepstraight.desktop.generated.resources.status_camera_open_failed
import com.keepstraight.desktop.generated.resources.status_camera_permission
import com.keepstraight.desktop.generated.resources.status_looking_good
import com.keepstraight.desktop.generated.resources.status_no_camera
import com.keepstraight.desktop.generated.resources.status_no_pose
import com.keepstraight.desktop.generated.resources.status_paused
import com.keepstraight.desktop.generated.resources.status_paused_away
import com.keepstraight.desktop.generated.resources.status_paused_face_camera
import com.keepstraight.desktop.generated.resources.status_paused_standing
import com.keepstraight.desktop.generated.resources.status_pose_model_missing
import com.keepstraight.desktop.generated.resources.status_poses_too_similar
import com.keepstraight.desktop.generated.resources.status_ready
import com.keepstraight.desktop.generated.resources.status_slouching
import com.keepstraight.desktop.generated.resources.status_too_dark
import com.keepstraight.shared.presentation.StatusCopyKey
import org.jetbrains.compose.resources.stringResource

object StatusCopyResolver {
    @Composable
    fun text(key: StatusCopyKey, vararg args: Any): String = when (key) {
        StatusCopyKey.STATUS_CALIBRATING_ERECT -> stringResource(Res.string.status_calibrating_erect)
        StatusCopyKey.STATUS_CALIBRATING_SLUMPED -> stringResource(Res.string.status_calibrating_slumped)
        StatusCopyKey.STATUS_CALIBRATION_NEEDED -> stringResource(Res.string.status_calibration_needed)
        StatusCopyKey.STATUS_READY -> stringResource(Res.string.status_ready)
        StatusCopyKey.STATUS_PAUSED_STANDING -> stringResource(Res.string.status_paused_standing)
        StatusCopyKey.STATUS_PAUSED_AWAY -> stringResource(Res.string.status_paused_away)
        StatusCopyKey.STATUS_PAUSED_FACE_CAMERA -> stringResource(Res.string.status_paused_face_camera)
        StatusCopyKey.STATUS_PAUSED -> stringResource(Res.string.status_paused)
        StatusCopyKey.STATUS_SLOUCHING -> stringResource(Res.string.status_slouching)
        StatusCopyKey.STATUS_LOOKING_GOOD -> stringResource(Res.string.status_looking_good)
        StatusCopyKey.STATUS_CAMERA_IN_USE -> stringResource(Res.string.status_camera_in_use)
        StatusCopyKey.STATUS_CAMERA_PERMISSION -> stringResource(Res.string.status_camera_permission)
        StatusCopyKey.STATUS_NO_CAMERA -> stringResource(Res.string.status_no_camera)
        StatusCopyKey.STATUS_CAMERA_OPEN_FAILED -> stringResource(Res.string.status_camera_open_failed)
        StatusCopyKey.STATUS_CAMERA_DISCONNECTED -> stringResource(Res.string.status_camera_disconnected)
        StatusCopyKey.STATUS_POSE_MODEL_MISSING -> stringResource(Res.string.status_pose_model_missing)
        StatusCopyKey.STATUS_CALIBRATE_ERECT_FIRST -> stringResource(Res.string.status_calibrate_erect_first)
        StatusCopyKey.STATUS_POSES_TOO_SIMILAR -> stringResource(Res.string.status_poses_too_similar)
        StatusCopyKey.STATUS_NO_POSE -> stringResource(Res.string.status_no_pose)
        StatusCopyKey.STATUS_BRIDGE_PAIR_FAILED -> stringResource(Res.string.status_bridge_pair_failed)
        StatusCopyKey.STATUS_BRIDGE_UNREACHABLE -> stringResource(Res.string.status_bridge_unreachable)
        StatusCopyKey.STATUS_BRIDGE_EXPIRED -> stringResource(Res.string.status_bridge_expired)
        StatusCopyKey.STATUS_TOO_DARK -> stringResource(Res.string.status_too_dark)
        StatusCopyKey.BODY_HOLD_GOOD_SITTING -> stringResource(Res.string.body_hold_good_sitting)
        StatusCopyKey.BODY_HOLD_USUAL_SLOUCH -> stringResource(Res.string.body_hold_usual_slouch)
        StatusCopyKey.BODY_CALIBRATE_BEFORE_MONITOR -> stringResource(Res.string.body_calibrate_before_monitor)
        StatusCopyKey.BODY_START_SESSION -> stringResource(Res.string.body_start_session)
        StatusCopyKey.BODY_PAUSED_STANDING -> stringResource(Res.string.body_paused_standing)
        StatusCopyKey.BODY_PAUSED_AWAY -> stringResource(Res.string.body_paused_away)
        StatusCopyKey.BODY_PAUSED_FACE_CAMERA -> stringResource(Res.string.body_paused_face_camera)
        StatusCopyKey.BODY_MONITORING_PAUSED -> stringResource(Res.string.body_monitoring_paused)
        StatusCopyKey.BODY_SLOUCHING_SCORE -> stringResource(Res.string.body_slouching_score, args[0] as Int)
        StatusCopyKey.BODY_MONITORING_SITTING -> stringResource(Res.string.body_monitoring_sitting)
        StatusCopyKey.BODY_CAMERA_IN_USE -> stringResource(Res.string.body_camera_in_use)
        StatusCopyKey.BODY_CAMERA_PERMISSION -> stringResource(Res.string.body_camera_permission)
        StatusCopyKey.BODY_NO_CAMERA -> stringResource(Res.string.body_no_camera)
        StatusCopyKey.BODY_CAMERA_OPEN_FAILED -> stringResource(Res.string.body_camera_open_failed)
        StatusCopyKey.BODY_CAMERA_DISCONNECTED -> stringResource(Res.string.body_camera_disconnected)
        StatusCopyKey.BODY_POSE_MODEL_MISSING -> stringResource(Res.string.body_pose_model_missing)
        StatusCopyKey.BODY_CALIBRATE_ERECT_FIRST -> stringResource(Res.string.body_calibrate_erect_first)
        StatusCopyKey.BODY_POSES_TOO_SIMILAR -> stringResource(Res.string.body_poses_too_similar)
        StatusCopyKey.BODY_NO_POSE_CALIBRATE -> stringResource(Res.string.body_no_pose_calibrate)
        StatusCopyKey.BODY_BRIDGE_PAIR_FAILED_DEFAULT -> stringResource(Res.string.body_bridge_pair_failed_default)
        StatusCopyKey.BODY_BRIDGE_UNREACHABLE -> stringResource(Res.string.body_bridge_unreachable)
        StatusCopyKey.BODY_BRIDGE_EXPIRED -> stringResource(Res.string.body_bridge_expired)
        StatusCopyKey.BODY_TOO_DARK -> stringResource(Res.string.body_too_dark)
        StatusCopyKey.PRESENCE_SITTING -> stringResource(Res.string.presence_sitting)
        StatusCopyKey.PRESENCE_STANDING -> stringResource(Res.string.presence_standing)
        StatusCopyKey.PRESENCE_AWAY -> stringResource(Res.string.presence_away)
        StatusCopyKey.PRESENCE_LOW_CONFIDENCE -> stringResource(Res.string.presence_low_confidence)
        StatusCopyKey.SESSION_CALIBRATION_LOADED -> stringResource(Res.string.session_calibration_loaded)
        StatusCopyKey.SESSION_MONITORING_POSTURE -> stringResource(Res.string.session_monitoring_posture)
        StatusCopyKey.SESSION_STOPPED -> stringResource(Res.string.session_stopped)
        StatusCopyKey.SESSION_HOLD_GOOD_POSTURE -> stringResource(Res.string.session_hold_good_posture)
        StatusCopyKey.SESSION_HOLD_SLUMPED_POSTURE -> stringResource(Res.string.session_hold_slumped_posture)
        StatusCopyKey.SESSION_MONITORING_READY -> stringResource(Res.string.session_monitoring_ready)
        StatusCopyKey.SESSION_CALIBRATION_CANCELLED -> stringResource(Res.string.session_calibration_cancelled)
        StatusCopyKey.SESSION_NO_POSE_DETECTED -> stringResource(Res.string.session_no_pose_detected)
        StatusCopyKey.SESSION_CALIBRATE_ERECT_FIRST -> stringResource(Res.string.session_calibrate_erect_first)
        StatusCopyKey.SESSION_SLOUCHING_SIT_UP -> stringResource(Res.string.session_slouching_sit_up)
        StatusCopyKey.SESSION_LOOKING_GOOD -> stringResource(Res.string.session_looking_good)
        StatusCopyKey.SESSION_CAMERA_ERROR -> stringResource(Res.string.session_camera_error)
        StatusCopyKey.SESSION_CAMERA_READY -> stringResource(Res.string.session_camera_ready)
        StatusCopyKey.SESSION_HOLD_STILL_PROGRESS -> stringResource(Res.string.session_hold_still_progress, args[0] as Int, args[1] as Int)
        StatusCopyKey.SESSION_ERECT_CAPTURED -> stringResource(Res.string.session_erect_captured)
        StatusCopyKey.SESSION_POSES_TOO_SIMILAR -> stringResource(Res.string.session_poses_too_similar)
        StatusCopyKey.SESSION_CALIBRATION_COMPLETE -> stringResource(Res.string.session_calibration_complete)
    }

    @Composable
    fun presentationTitle(presentation: com.keepstraight.shared.presentation.DesktopStatusPresentation): String =
        text(presentation.titleKey, *presentation.titleArgs.toTypedArray())

    @Composable
    fun presentationBody(presentation: com.keepstraight.shared.presentation.DesktopStatusPresentation): String =
        presentation.bodyOverride ?: text(presentation.bodyKey, *presentation.bodyArgs.toTypedArray())

    @Composable
    fun presentationPresence(presentation: com.keepstraight.shared.presentation.DesktopStatusPresentation): String? =
        presentation.presenceKey?.let { text(it) }
}
