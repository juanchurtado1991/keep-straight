package com.keepstraight.bridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the desktop LAN bridge after reboot when already paired. */
class DesktopBridgeBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        PhoneLanBridgeService.startIfPaired(context)
    }
}
