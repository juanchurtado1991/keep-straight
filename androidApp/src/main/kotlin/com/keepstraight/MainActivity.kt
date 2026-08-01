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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.keepstraight.ui.AlertSettingsScreen
import com.keepstraight.ui.CalibratePostureScreen
import com.keepstraight.ui.DashboardScreen
import com.keepstraight.ui.HistoryScreen
import com.keepstraight.ui.SensitivityScreen
import com.keepstraight.ui.SettingsScreen
import com.keepstraight.ui.onboarding.OnboardingScreen
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

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(
                            viewModel = viewModel,
                            onOpenNotificationSettings = {
                                startActivity(SystemIntentsHelper.notificationSettings(this@MainActivity))
                            },
                            onOpenBatterySettings = {
                                startActivity(SystemIntentsHelper.batteryOptimizationSettings(this@MainActivity))
                            },
                            onOpenBluetoothSettings = {
                                startActivity(SystemIntentsHelper.bluetoothSettings())
                            },
                            onCalibrate = {
                                navController.navigate(Routes.CALIBRATE)
                            },
                            onComplete = {
                                navController.navigate(Routes.DASHBOARD) {
                                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                                }
                            },
                        )
                    }

                    composable(Routes.DASHBOARD) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onRecalibrate = { navController.navigate(Routes.CALIBRATE) },
                            onHistory = { navController.navigate(Routes.HISTORY) },
                            onAlertSettings = { navController.navigate(Routes.ALERT_SETTINGS) },
                            onSensitivity = { navController.navigate(Routes.SENSITIVITY) },
                            onSettings = { navController.navigate(Routes.SETTINGS) },
                            onOpenBatterySettings = {
                                startActivity(SystemIntentsHelper.batteryOptimizationSettings(this@MainActivity))
                            },
                        )
                    }

                    composable(Routes.CALIBRATE) {
                        CalibratePostureScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
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
                                startActivity(SystemIntentsHelper.batteryOptimizationSettings(this@MainActivity))
                            },
                            onOpenBluetoothSettings = {
                                startActivity(SystemIntentsHelper.bluetoothSettings())
                            },
                            onOpenAppDetails = {
                                startActivity(SystemIntentsHelper.appDetailsSettings(this@MainActivity))
                            },
                            onBack = { navController.popBackStack() },
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
    const val DASHBOARD = "dashboard"
    const val CALIBRATE = "calibrate"
    const val HISTORY = "history"
    const val ALERT_SETTINGS = "alert_settings"
    const val SENSITIVITY = "sensitivity"
    const val SETTINGS = "settings"
}
