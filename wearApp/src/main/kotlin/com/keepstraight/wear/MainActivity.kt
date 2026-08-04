package com.keepstraight.wear

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepstraight.wear.alerts.AlertDispatcher
import com.keepstraight.wear.service.PostureMonitoringService
import com.keepstraight.wear.ui.MonitoringScreen

class MainActivity : ComponentActivity() {

    private var flashVisible by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* permissions handled on next service start */ }

    private val flashReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlertDispatcher.ACTION_ALERT_FLASH) {
                flashVisible = true
                window.decorView.postDelayed({ flashVisible = false }, FLASH_DURATION_MS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestSensorPermissions()

        if (PostureMonitoringService.isMonitoringEnabled(this)) {
            PostureMonitoringService.start(this)
        }

        setContent {
            val app = application as KeepStraightWearApp
            val state by app.monitoringSession.monitoringState.collectAsStateWithLifecycle()
            val calibrating by app.monitoringSession.isCalibrating.collectAsStateWithLifecycle()
            MonitoringScreen(
                state = state,
                isCalibrating = calibrating,
                flashVisible = flashVisible,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            flashReceiver,
            IntentFilter(AlertDispatcher.ACTION_ALERT_FLASH),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        (application as KeepStraightWearApp).inboundHandler.onUiVisible()
    }

    override fun onStop() {
        unregisterReceiver(flashReceiver)
        super.onStop()
    }

    private fun requestSensorPermissions() {
        val permissions = buildList {
            add(Manifest.permission.BODY_SENSORS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private companion object {
        const val FLASH_DURATION_MS = 300L
    }
}
