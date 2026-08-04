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
import com.keepstraight.ui.AlertSettingsScreen
import com.keepstraight.ui.ConnectionFlowScreen
import com.keepstraight.ui.DashboardScreen
import com.keepstraight.ui.DesktopQrScanScreen
import com.keepstraight.ui.HistoryScreen
import com.keepstraight.ui.SensitivityScreen
import com.keepstraight.ui.SettingsScreen
import com.keepstraight.ui.onboarding.OnboardingScreen
import com.keepstraight.ui.onboarding.STEP_PAIR
import com.keepstraight.ui.theme.KeepStraightTheme
import com.keepstraight.util.SystemIntentsHelper
import com.keepstraight.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* user choice handled via settings deep link */ }

    private var mainViewModel: MainViewModel? = null

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
                val viewModel: MainViewModel = viewModel()
                mainViewModel = viewModel
                val navController = rememberNavController()
                val onboardingComplete by viewModel.onboardingComplete.collectAsState()
                val events = viewModel.eventsPaged.collectAsLazyPagingItems()

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
                        OnboardingScreen(
                            viewModel = viewModel,
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
                        OnboardingScreen(
                            viewModel = viewModel,
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
                        DashboardScreen(
                            viewModel = viewModel,
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
                        ConnectionFlowScreen(
                            viewModel = viewModel,
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
                        AlertSettingsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SENSITIVITY) {
                        SensitivityScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            viewModel = viewModel,
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

                    composable(Routes.DESKTOP_QR) {
                        DesktopQrScanScreen(
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
        mainViewModel?.refreshBatteryBanner()
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
    const val DESKTOP_QR = "desktop_qr"

    fun connection(autoStart: Boolean) = "connection?autoStart=$autoStart"
}
