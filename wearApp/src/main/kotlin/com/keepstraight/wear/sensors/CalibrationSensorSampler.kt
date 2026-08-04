package com.keepstraight.wear.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper

/**
 * Direct accelerometer sampling for calibration that does **not** depend on the
 * foreground service being allowed to start from a WearableListenerService.
 */
class CalibrationSensorSampler(
    context: Context,
    private val onSample: (ax: Float, ay: Float, az: Float, timestampMs: Long) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private var latestAx = 0f
    private var latestAy = 0f
    private var latestAz = 0f
    private var hasSample = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            if (hasSample) {
                onSample(latestAx, latestAy, latestAz, System.currentTimeMillis())
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    fun start() {
        if (running) return
        running = true
        hasSample = false
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        handler.post(tick)
    }

    fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacks(tick)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        latestAx = event.values[0]
        latestAy = event.values[1]
        latestAz = event.values[2]
        hasSample = true
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val TICK_MS = 200L
    }
}
