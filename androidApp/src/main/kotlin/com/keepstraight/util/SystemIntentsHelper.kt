package com.keepstraight.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

object SystemIntentsHelper {

    fun batteryOptimizationSettings(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun batteryOptimizationFallback(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** Prefer direct REQUEST; fall back to the system battery-opt list if OEM blocks it. */
    fun openBatteryOptimization(context: Context): Intent {
        val primary = batteryOptimizationSettings(context)
        return if (primary.resolveActivity(context.packageManager) != null) {
            primary
        } else {
            batteryOptimizationFallback()
        }
    }

    fun startBatteryOptimization(context: Context) {
        try {
            context.startActivity(openBatteryOptimization(context))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(batteryOptimizationFallback())
        } catch (_: SecurityException) {
            context.startActivity(batteryOptimizationFallback())
        }
    }

    fun notificationSettings(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun appDetailsSettings(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun bluetoothSettings(): Intent =
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun wearCompanion(context: Context): Intent? {
        val launch = context.packageManager.getLaunchIntentForPackage(WEAR_COMPANION_PACKAGE)
            ?: return null
        return launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun isWearCompanionInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(WEAR_COMPANION_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    fun canOpenBatteryOptimization(context: Context): Boolean =
        openBatteryOptimization(context).resolveActivity(context.packageManager) != null

    private const val WEAR_COMPANION_PACKAGE = "com.google.android.apps.wear.companion"
}
