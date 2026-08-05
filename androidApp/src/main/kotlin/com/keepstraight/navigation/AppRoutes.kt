package com.keepstraight.navigation

object AppRoutes {
    const val ONBOARDING = "onboarding"
    const val CHANGE_WATCH = "change_watch"
    const val DASHBOARD = "dashboard"
    const val CONNECTION = "connection?${NavArguments.AUTO_START}={${NavArguments.AUTO_START}}"
    const val HISTORY = "history"
    const val ALERT_SETTINGS = "alert_settings"
    const val SENSITIVITY = "sensitivity"
    const val SETTINGS = "settings"
    const val CALIBRATE = "calibrate"
    const val DESKTOP_QR = "desktop_qr"

    fun connection(autoStart: Boolean): String =
        "connection?${NavArguments.AUTO_START}=$autoStart"
}
