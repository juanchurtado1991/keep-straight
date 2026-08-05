package com.keepstraight.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.keepstraight.desktop.alert.NativeDesktopNotifier
import com.keepstraight.desktop.generated.resources.Res
import com.keepstraight.desktop.generated.resources.icon
import com.keepstraight.desktop.ui.DesktopApp
import com.keepstraight.desktop.ui.KeepStraightDesktopTheme
import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.presentation.DesktopPrefsKeys
import com.keepstraight.desktop.ui.i18n.DesktopMessageJvm
import com.keepstraight.desktop.ui.i18n.DesktopStrings
import com.keepstraight.desktop.ui.wizard.FirstRunWizard
import com.keepstraight.shared.vision.WebcamBootstrap
import org.jetbrains.compose.resources.painterResource
import java.awt.SystemTray
import java.util.prefs.Preferences

fun main() {
    WebcamBootstrap.ensureInitialized()
    application {
        val prefs = remember { Preferences.userRoot().node(DesktopPrefsKeys.ROOT) }
        var consentAccepted by remember {
            mutableStateOf(prefs.getBoolean(DesktopPrefsKeys.CAMERA_CONSENT_ACCEPTED, false))
        }
        var wizardCompleted by remember {
            mutableStateOf(prefs.getBoolean(DesktopPrefsKeys.WIZARD_COMPLETED, false))
        }

        val trayLabels = DesktopSystemTray.Labels(
            tooltip = DesktopMessageJvm.text(DesktopMessageKey.TRAY_APP_NAME),
            open = DesktopMessageJvm.text(DesktopMessageKey.TRAY_OPEN),
            hide = DesktopMessageJvm.text(DesktopMessageKey.TRAY_HIDE),
            quit = DesktopMessageJvm.text(DesktopMessageKey.TRAY_QUIT),
        )
        val systemTray = remember {
            if (SystemTray.isSupported()) DesktopSystemTray() else null
        }
        val trayInstalled = remember { systemTray?.install(trayLabels) == true }

        var windowVisible by remember {
            val startHidden = prefs.getBoolean(DesktopPrefsKeys.START_HIDDEN_IN_TRAY, false)
            mutableStateOf(
                when {
                    !wizardCompleted -> true
                    startHidden && trayInstalled -> false
                    else -> true
                },
            )
        }

        val controller = remember { DesktopSessionController(prefs) }
        val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
        val appIcon = painterResource(Res.drawable.icon)

        val quitApp: () -> Unit = {
            NativeDesktopNotifier.bindTray(null)
            systemTray?.dispose()
            controller.shutdown()
            exitApplication()
        }

        systemTray?.setCallbacks(
            open = { windowVisible = true },
            hide = {
                controller.noteHiddenToTray()
                windowVisible = false
            },
            quit = quitApp,
        )

        if (trayInstalled) {
            NativeDesktopNotifier.bindTray(
                NativeDesktopNotifier.TrayNotifier { title, body ->
                    systemTray?.showNotification(title, body) == true
                },
            )
        } else {
            NativeDesktopNotifier.bindTray(null)
        }

        val hideToTray: (() -> Unit)? = if (trayInstalled) {
            {
                controller.noteHiddenToTray()
                windowVisible = false
            }
        } else {
            null
        }

        Window(
            onCloseRequest = {
                if (wizardCompleted && trayInstalled) {
                    windowVisible = false
                } else {
                    quitApp()
                }
            },
            visible = windowVisible,
            title = DesktopStrings.appName(),
            state = windowState,
            icon = appIcon,
        ) {
            KeepStraightDesktopTheme {
                if (!wizardCompleted) {
                    FirstRunWizard(
                        controller = controller,
                        prefsAcceptedCamera = consentAccepted,
                        onAcceptCamera = {
                            prefs.putBoolean(DesktopPrefsKeys.CAMERA_CONSENT_ACCEPTED, true)
                            consentAccepted = true
                            controller.onCameraConsentGranted()
                        },
                        onDeclineCamera = quitApp,
                        onFinished = {
                            prefs.putBoolean(DesktopPrefsKeys.WIZARD_COMPLETED, true)
                            wizardCompleted = true
                        },
                    )
                } else {
                    DesktopApp(
                        controller = controller,
                        onQuit = quitApp,
                        onHideToTray = hideToTray,
                    )
                }
            }
        }
    }
}
