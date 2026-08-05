package com.keepstraight.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.keepstraight.desktop.alert.NativeDesktopNotifier
import com.keepstraight.desktop.ui.DesktopApp
import com.keepstraight.desktop.ui.KeepStraightDesktopTheme
import com.keepstraight.desktop.ui.wizard.FirstRunWizard
import com.keepstraight.shared.vision.WebcamBootstrap
import java.awt.SystemTray
import java.util.prefs.Preferences

fun main() {
    WebcamBootstrap.ensureInitialized()
    application {
        val prefs = remember { Preferences.userRoot().node("com.keepstraight.desktop") }
        var consentAccepted by remember {
            mutableStateOf(prefs.getBoolean("camera_consent_accepted", false))
        }
        var wizardCompleted by remember {
            mutableStateOf(prefs.getBoolean("wizard_completed", false))
        }

        val systemTray = remember {
            if (SystemTray.isSupported()) DesktopSystemTray() else null
        }
        val trayInstalled = remember { systemTray?.install() == true }

        var windowVisible by remember {
            val startHidden = prefs.getBoolean("start_hidden_in_tray", false)
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
        val appIcon = painterResource("icons/icon.png")

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
            title = "KeepStraight",
            state = windowState,
            icon = appIcon,
        ) {
            KeepStraightDesktopTheme {
                if (!wizardCompleted) {
                    FirstRunWizard(
                        controller = controller,
                        prefsAcceptedCamera = consentAccepted,
                        onAcceptCamera = {
                            prefs.putBoolean("camera_consent_accepted", true)
                            consentAccepted = true
                            controller.onCameraConsentGranted()
                        },
                        onDeclineCamera = quitApp,
                        onFinished = {
                            prefs.putBoolean("wizard_completed", true)
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
