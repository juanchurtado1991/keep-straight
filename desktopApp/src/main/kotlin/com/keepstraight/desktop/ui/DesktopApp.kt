package com.keepstraight.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.keepstraight.desktop.DesktopSessionController
import com.keepstraight.desktop.navigation.DesktopRoute
import com.keepstraight.desktop.ui.home.HomeScreen
import com.keepstraight.shared.presentation.DesktopStatusAction

@Composable
fun DesktopApp(
    controller: DesktopSessionController,
    onQuit: () -> Unit,
    onHideToTray: (() -> Unit)?,
) {
    var route by remember { mutableStateOf(DesktopRoute.Home) }

    LaunchedEffect(route) { controller.clearBridgeActionMessage() }

    when (route) {
        DesktopRoute.Calibrate -> CalibrationScreen(
            controller = controller,
            onClose = { route = DesktopRoute.Home },
        )
        DesktopRoute.Settings -> DesktopSettingsScreen(
            controller = controller,
            onBack = { route = DesktopRoute.Home },
            onHideToTray = onHideToTray,
            onOpenCompanionSetup = { route = DesktopRoute.CompanionSetup },
            onQuit = onQuit,
        )
        DesktopRoute.CompanionSetup -> com.keepstraight.desktop.ui.wizard.CompanionSetupFlow(
            controller = controller,
            onFinished = { route = DesktopRoute.Home },
        )
        DesktopRoute.Home -> {
            val ui by controller.uiState.collectAsState()
            val status by controller.statusPresentation.collectAsState()
            val bridgeState by controller.bridgeState.collectAsState()
            val bridgeHost by controller.bridgeHost.collectAsState()
            val bridgeMsg by controller.bridgeActionMessage.collectAsState()
            val bridgeBusy by controller.bridgeActionBusy.collectAsState()

            HomeScreen(
                controller = controller,
                status = status,
                ui = ui,
                bridgeState = bridgeState,
                bridgeHost = bridgeHost,
                bridgeMsg = bridgeMsg,
                bridgeBusy = bridgeBusy,
                onOpenCalibrate = { route = DesktopRoute.Calibrate },
                onOpenSettings = { route = DesktopRoute.Settings },
                onOpenCompanion = { route = DesktopRoute.CompanionSetup },
                onHideToTray = onHideToTray,
                onStatusAction = { action ->
                    when (action) {
                        DesktopStatusAction.CALIBRATE,
                        DesktopStatusAction.CALIBRATE_ERECT,
                        DesktopStatusAction.CALIBRATE_SLUMP,
                        -> route = DesktopRoute.Calibrate
                        DesktopStatusAction.REPAIR_BRIDGE -> route = DesktopRoute.CompanionSetup
                        else -> controller.handleStatusAction(action)
                    }
                },
            )
        }
    }
}
