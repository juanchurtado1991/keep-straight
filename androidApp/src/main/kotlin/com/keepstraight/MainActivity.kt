package com.keepstraight

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.androidx.compose.koinViewModel
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
import com.keepstraight.navigation.AppRoutes
import com.keepstraight.navigation.NavArguments
import com.keepstraight.ui.onboarding.OnboardingStep
import com.keepstraight.ui.theme.KeepStraightTheme
import com.keepstraight.util.SystemIntentsHelper

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* user choice handled via settings deep link */ }

    private val dashboardViewModel: DashboardViewModel by viewModel()

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
                val shellViewModel: AppShellViewModel = koinViewModel()
                val navController = rememberNavController()
                val onboardingComplete by shellViewModel.onboardingComplete.collectAsState()
                val events = shellViewModel.eventsPaged.collectAsLazyPagingItems()

                val startDestination = if (onboardingComplete) {
                    AppRoutes.DASHBOARD
                } else {
                    AppRoutes.ONBOARDING
                }

                LaunchedEffect(onboardingComplete) {
                    if (onboardingComplete &&
                        navController.currentDestination?.route == AppRoutes.ONBOARDING
                    ) {
                        navController.navigate(AppRoutes.DASHBOARD) {
                            popUpTo(AppRoutes.ONBOARDING) { inclusive = true }
                        }
                    }
                }

                fun openConnection(autoStart: Boolean) {
                    navController.navigate(AppRoutes.connection(autoStart))
                }

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(AppRoutes.ONBOARDING) {
                        val pairingViewModel: WatchPairingViewModel = koinViewModel()
                        val onboardingViewModel: OnboardingViewModel = koinViewModel()
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
                            onOpenCalibrate = { navController.navigate(AppRoutes.CALIBRATE) },
                            onComplete = {
                                navController.navigate(AppRoutes.DASHBOARD) {
                                    popUpTo(AppRoutes.ONBOARDING) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(AppRoutes.CHANGE_WATCH) {
                        val pairingViewModel: WatchPairingViewModel = koinViewModel()
                        val onboardingViewModel: OnboardingViewModel = koinViewModel()
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
                            onOpenCalibrate = { navController.navigate(AppRoutes.CALIBRATE) },
                            onComplete = { navController.popBackStack() },
                            initialStep = OnboardingStep.PAIR,
                            pairOnly = true,
                        )
                    }

                    composable(AppRoutes.DASHBOARD) {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onSettings = { navController.navigate(AppRoutes.SETTINGS) },
                            onScanDesktopQr = { navController.navigate(AppRoutes.DESKTOP_QR) },
                            onOpenBatterySettings = {
                                SystemIntentsHelper.startBatteryOptimization(this@MainActivity)
                            },
                            onFixConnection = { openConnection(autoStart = true) },
                            onConnectionStatus = { openConnection(autoStart = false) },
                        )
                    }

                    composable(
                        route = AppRoutes.CONNECTION,
                        arguments = listOf(
                            navArgument(NavArguments.AUTO_START) { type = NavType.BoolType; defaultValue = false },
                        ),
                    ) { entry ->
                        val autoStart = entry.arguments?.getBoolean(NavArguments.AUTO_START) ?: false
                        val connectionViewModel: ConnectionViewModel = koinViewModel()
                        ConnectionFlowScreen(
                            viewModel = connectionViewModel,
                            autoStart = autoStart,
                            onBack = { navController.popBackStack() },
                            onChangeWatch = {
                                navController.navigate(AppRoutes.CHANGE_WATCH) {
                                    popUpTo(AppRoutes.CONNECTION) { inclusive = true }
                                }
                            },
                            onOpenBluetooth = {
                                startActivity(SystemIntentsHelper.bluetoothSettings())
                            },
                        )
                    }

                    composable(AppRoutes.HISTORY) {
                        HistoryScreen(
                            events = events,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(AppRoutes.ALERT_SETTINGS) {
                        val settingsViewModel: SettingsViewModel = koinViewModel()
                        AlertSettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(AppRoutes.SENSITIVITY) {
                        val settingsViewModel: SettingsViewModel = koinViewModel()
                        SensitivityScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(AppRoutes.SETTINGS) {
                        val settingsViewModel: SettingsViewModel = koinViewModel()
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
                            onHistory = { navController.navigate(AppRoutes.HISTORY) },
                            onAlertSettings = { navController.navigate(AppRoutes.ALERT_SETTINGS) },
                            onSensitivity = { navController.navigate(AppRoutes.SENSITIVITY) },
                            onChangePairedWatch = {
                                navController.navigate(AppRoutes.CHANGE_WATCH)
                            },
                            onScanDesktopQr = { navController.navigate(AppRoutes.DESKTOP_QR) },
                            onOpenConnection = { openConnection(autoStart = false) },
                            onRecalibrate = { navController.navigate(AppRoutes.CALIBRATE) },
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(AppRoutes.CALIBRATE) {
                        val calibrationViewModel: CalibrationViewModel = koinViewModel()
                        CalibratePostureScreen(
                            viewModel = calibrationViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenConnection = { openConnection(autoStart = true) },
                        )
                    }

                    composable(AppRoutes.DESKTOP_QR) {
                        val desktopPairingViewModel: DesktopPairingViewModel = koinViewModel()
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
        dashboardViewModel.refreshBatteryBanner()
    }
}
