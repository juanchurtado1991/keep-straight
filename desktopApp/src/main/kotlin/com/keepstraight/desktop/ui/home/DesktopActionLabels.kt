package com.keepstraight.desktop.ui.home

import com.keepstraight.desktop.ui.i18n.DesktopStrings
import androidx.compose.runtime.Composable
import com.keepstraight.shared.presentation.DesktopStatusAction

@Composable
fun desktopActionLabel(action: DesktopStatusAction): String = when (action) {
    DesktopStatusAction.RETRY_CAMERA -> DesktopStrings.actionRetryCamera()
    DesktopStatusAction.REFRESH_CAMERAS -> DesktopStrings.actionRefreshCameras()
    DesktopStatusAction.CALIBRATE,
    DesktopStatusAction.CALIBRATE_ERECT,
    DesktopStatusAction.CALIBRATE_SLUMP,
    -> DesktopStrings.actionCalibrate()
    DesktopStatusAction.STOP_SESSION -> DesktopStrings.actionStop()
    DesktopStatusAction.CLEAR_BRIDGE -> DesktopStrings.actionUnlinkPhone()
    DesktopStatusAction.REPAIR_BRIDGE -> DesktopStrings.actionFixPhoneLink()
}
