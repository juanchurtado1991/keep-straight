package com.keepstraight

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.keepstraight.bridge.PhoneLanBridgeService
import com.keepstraight.presentation.calibration.CalibrationViewModel
import com.keepstraight.presentation.connection.ConnectionViewModel
import com.keepstraight.presentation.dashboard.DashboardViewModel
import com.keepstraight.presentation.onboarding.OnboardingViewModel
import com.keepstraight.presentation.pairing.WatchPairingViewModel
import com.keepstraight.presentation.settings.SettingsViewModel
import com.keepstraight.presentation.shell.AppShellViewModel
import com.keepstraight.ui.AlertSettingsScreen
import com.keepstraight.ui.CalibratePostureScreen
import com.keepstraight.ui.ConnectionFlowScreen
import com.keepstraight.ui.DashboardScreen
import com.keepstraight.presentation.pairing.DesktopPairingViewModel
import com.keepstraight.ui.DesktopQrScanScreen
import com.keepstraight.ui.HistoryScreen
import com.keepstraight.ui.SensitivityScreen
import com.keepstraight.ui.SettingsScreen
import com.keepstraight.ui.onboarding.OnboardingScreen
import com.keepstraight.ui.onboarding.STEP_PAIR
import com.keepstraight.ui.theme.KeepStraightTheme
import com.keepstraight.util.SystemIntentsHelper

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* user choice handled via settings deep link */ }

    private var dashboardViewModel: DashboardViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PhoneLanBridgeService.start(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            KeepStraightTheme {
                val shellViewModel: AppShellViewModel = viewModel()
                val navController = rememberNavController()
                val onboardingComplete by shellViewModel.onboardingComplete.collectAsState()
                val events = shellViewModel.eventsPaged.collectAsLazyPagingItems()

                val startDestination = if (onboardingComplete) {
                    Routes.DASHBOARD
                } else {
                    Routes.ONBOARDING
                }

                fun openConnection(autoStart: Boolean) {
                    navController.navigate(Routes.connection(autoStart))
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.ONBOARDING) {
                        val pairingViewModel: WatchPairingViewModel = viewModel()
                        val onboardingViewModel: OnboardingViewModel = viewModel()
                        OnboardingScreen(
                            pairingViewModel = pairingViewModel,
                            onboardingViewModel = onboardingViewModel,
                            onOpenNotificationSettings = {
                                startActivity(SystemIntentsHelper.notificationSettings(this@MainActivity))
                            },
                            onOpenBatterySettings = {
                                SystemIntentsHelper.startBatteryOptimization(this@MainActivity)
                            },
                            onOpenBatteryFallback = {
                                startActivity(SystemIntentsHelper.batteryOptimizationFallback())
                            },
                            onOpenBluetoothSettings = {
                                startActivity(SystemIntentsHelper.bluetoothSettings())
                            },
                            onOpenWearCompanion = SystemIntentsHelper.wearCompanion(this@MainActivity)?.let { intent ->
                                { startActivity(intent) }
                            },
                            onComplete = {
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(Routes.CHANGE_WATCH) {
                        val pairingViewModel: WatchPairingViewModel = viewModel()
                        val onboardingViewModel: OnboardingViewModel = viewModel()
                        OnboardingScreen(
                            pairingViewModel = pairingViewModel,
                            onboardingViewModel = onboardingViewModel,
                            onOpenNotificationSettings = {},
                            onOpenBatterySettings = {},
                            onOpenBatteryFallback = {},
                            onOpenBluetoothSettings = {
                                startActivity(SystemIntentsHelper.bluetoothSettings())
                            },
                            onOpenWearCompanion = SystemIntentsHelper.wearCompanion(this@MainActivity)?.let { intent ->
                                { startActivity(intent) }
                            },
                            onComplete = { navController.popBackStack() },
                            initialStep = STEP_PAIR,
                            pairOnly = true,
                        )
                    }

                    composable(Routes.DASHBOARD) {
                        val dashboardVm: DashboardViewModel = viewModel()
                        dashboardViewModel = dashboardVm
                        DashboardScreen(
                            viewModel = dashboardVm,
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                            onScanDesktopQr = { navController.navigate(Routes.DESKTOP_QR) },
                            onOpenBatterySettings = {
                                SystemIntentsHelper.startBatteryOptimization(this@MainActivity)
                            },
                            onFixConnection = { openConnection(autoStart = true) },
                            onConnectionStatus = { openConnection(autoStart = false) },
                        )
                    }

                    composable(
                        route = Routes.CONNECTION,
                        arguments = listOf(
                            navArgument("autoStart") { type = NavType.BoolType; defaultValue = false },
                        ),
                    ) { entry ->
                        val autoStart = entry.arguments?.getBoolean("autoStart") ?: false
                        val connectionViewModel: ConnectionViewModel = viewModel()
                        ConnectionFlowScreen(
                            viewModel = connectionViewModel,
                            autoStart = autoStart,
                            onBack = { navController.popBackStack() },
                            onChangeWatch = {
                                navController.navigate(Routes.CHANGE_WATCH) {
                                    popUpTo(Routes.CONNECTION) { inclusive = true }
                                }
                            },
                            onOpenBluetooth = {
                                startActivity(SystemIntentsHelper.bluetoothSettings())
                            },
                        )
                    }

                    composable(Routes.HISTORY) {
                        HistoryScreen(
                            events = events,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.ALERT_SETTINGS) {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        AlertSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SENSITIVITY) {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        SensitivityScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SETTINGS) {
                        val settingsViewModel: SettingsViewModel = viewModel()
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onOpenNotificationSettings = {
                                startActivity(SystemIntentsHelper.notificationSettings(this@MainActivity))
                            },
                            onOpenBatterySettings = {
                                SystemIntentsHelper.startBatteryOptimization(this@MainActivity)
                            },
                            onOpenBluetoothSettings = {
                                startActivity(SystemIntentsHelper.bluetoothSettings())
                            },
                            onOpenAppDetails = {
                                startActivity(SystemIntentsHelper.appDetailsSettings(this@MainActivity))
                            },
                            onHistory = { navController.navigate(Routes.HISTORY) },
                            onAlertSettings = { navController.navigate(Routes.ALERT_SETTINGS) },
                            onSensitivity = { navController.navigate(Routes.SENSITIVITY) },
                            onChangePairedWatch = {
                                navController.navigate(Routes.CHANGE_WATCH)
                            },
                            onScanDesktopQr = { navController.navigate(Routes.DESKTOP_QR) },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.CALIBRATE) {
                        val calibrationViewModel: CalibrationViewModel = viewModel()
                        CalibratePostureScreen(
                            viewModel = calibrationViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenConnection = { openConnection(autoStart = true) },
                        )
                    }

                    composable(Routes.DESKTOP_QR) {
                        val desktopPairingViewModel: DesktopPairingViewModel = viewModel()
                        DesktopQrScanScreen(
                            viewModel = desktopPairingViewModel,
                            onBack = { navController.popBackStack() },
                            onPaired = {
                                navController.popBackStack()
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel?.refreshBatteryBanner()
    }
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val CHANGE_WATCH = "change_watch"
    const val DASHBOARD = "dashboard"
    const val CONNECTION = "connection?autoStart={autoStart}"
    const val HISTORY = "history"
    const val ALERT_SETTINGS = "alert_settings"
    const val SENSITIVITY = "sensitivity"
    const val SETTINGS = "settings"
    const val CALIBRATE = "calibrate"
    const val DESKTOP_QR = "desktop_qr"

    fun connection(autoStart: Boolean) = "connection?autoStart=$autoStart"
}
