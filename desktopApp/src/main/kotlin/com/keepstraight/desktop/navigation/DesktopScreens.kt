package com.keepstraight.desktop.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.keepstraight.desktop.DesktopSessionController
import com.keepstraight.sharedui.i18n.SharedStrings
import com.keepstraight.sharedui.navigation.SensitivityScreenRoute
import com.keepstraight.desktop.ui.CalibrationScreen
import com.keepstraight.desktop.ui.DesktopSettingsScreen
import com.keepstraight.desktop.ui.home.HomeScreen
import com.keepstraight.desktop.ui.wizard.CompanionSetupFlow
import com.keepstraight.shared.presentation.DesktopStatusAction
import org.koin.compose.koinInject

data class HomeScreenRoute(
    private val onHideToTray: (() -> Unit)?,
    private val onQuit: () -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val controller = koinInject<DesktopSessionController>()
        LaunchedEffect(Unit) { controller.clearBridgeActionMessage() }

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
            onOpenCalibrate = { navigator.push(CalibrateScreenRoute) },
            onOpenSettings = { navigator.push(SettingsScreenRoute(onHideToTray, onQuit)) },
            onOpenCompanion = { navigator.push(CompanionSetupScreenRoute) },
            onHideToTray = onHideToTray,
            onStatusAction = { action ->
                when (action) {
                    DesktopStatusAction.CALIBRATE,
                    DesktopStatusAction.CALIBRATE_ERECT,
                    DesktopStatusAction.CALIBRATE_SLUMP,
                    -> navigator.push(CalibrateScreenRoute)
                    DesktopStatusAction.REPAIR_BRIDGE -> navigator.push(CompanionSetupScreenRoute)
                    else -> controller.handleStatusAction(action)
                }
            },
        )
    }
}

data object CalibrateScreenRoute : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val controller = koinInject<DesktopSessionController>()
        LaunchedEffect(Unit) { controller.clearBridgeActionMessage() }
        CalibrationScreen(
            controller = controller,
            onClose = { navigator.pop() },
        )
    }
}

data class SettingsScreenRoute(
    private val onHideToTray: (() -> Unit)?,
    private val onQuit: () -> Unit,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val controller = koinInject<DesktopSessionController>()
        LaunchedEffect(Unit) { controller.clearBridgeActionMessage() }
        DesktopSettingsScreen(
            controller = controller,
            onBack = { navigator.pop() },
            onHideToTray = onHideToTray,
            onOpenCompanionSetup = { navigator.push(CompanionSetupScreenRoute) },
            onOpenFullSensitivity = {
                if (!controller.uiState.value.settingsFromPhone) {
                    navigator.push(buildDesktopSensitivityRoute(controller) { navigator.pop() })
                }
            },
            onQuit = onQuit,
        )
    }
}

data object CompanionSetupScreenRoute : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val controller = koinInject<DesktopSessionController>()
        LaunchedEffect(Unit) { controller.clearBridgeActionMessage() }
        CompanionSetupFlow(
            controller = controller,
            onFinished = { navigator.pop() },
        )
    }
}

private fun buildDesktopSensitivityRoute(
    controller: DesktopSessionController,
    onBack: () -> Unit,
): SensitivityScreenRoute {
    val ui = controller.uiState.value
    return SensitivityScreenRoute(
        sensitivity = ui.sensitivity,
        slumpDurationMs = ui.slumpDurationThresholdMs,
        repeatAlertMs = ui.repeatAlertIntervalMs,
        showTimingSliders = true,
        settingsFromPhone = ui.settingsFromPhone,
        sensitivityEnabled = !ui.settingsFromPhone,
        onSensitivityChange = controller::setSensitivity,
        onSlumpTimingChange = { slumpMs, repeatMs ->
            controller.setSlumpDurationMs(slumpMs)
            controller.setRepeatAlertMs(repeatMs)
        },
        header = {
            DesktopSensitivityHeader(onBack = onBack)
        },
    )
}

@Composable
private fun DesktopSensitivityHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            SharedStrings.sensitivityTitle(),
            style = MaterialTheme.typography.headlineLarge,
        )
        TextButton(onClick = onBack) {
            Text(SharedStrings.actionDone())
        }
    }
}
