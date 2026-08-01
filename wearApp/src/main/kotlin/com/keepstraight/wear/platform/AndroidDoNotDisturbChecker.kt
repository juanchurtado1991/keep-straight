package com.keepstraight.wear.platform

import android.app.NotificationManager
import android.content.Context
import com.keepstraight.shared.platform.DoNotDisturbChecker

class AndroidDoNotDisturbChecker(
    context: Context,
) : DoNotDisturbChecker {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override fun isActive(): Boolean =
        notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
}
