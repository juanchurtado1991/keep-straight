package com.keepstraight.wear.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.keepstraight.shared.util.FixedSampleBuffer
import kotlin.math.abs
import kotlin.math.sqrt

class OffBodyDetector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val offBodySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)
    private val magnitudeBuffer = FixedSampleBuffer(VARIANCE_WINDOW)
    private var sensorOnBody: Boolean? = null
    private var accelerometerSamples = 0

    val hasOffBodySensor: Boolean = offBodySensor != null

    fun register() {
        offBodySensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun unregister() {
        if (offBodySensor != null) {
            sensorManager.unregisterListener(this)
        }
    }

    fun onAccelerometerSample(ax: Float, ay: Float, az: Float) {
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        magnitudeBuffer.add(magnitude)
        accelerometerSamples++
    }

    fun isOffWrist(): Boolean {
        sensorOnBody?.let { onBody -> return !onBody }

        if (accelerometerSamples < VARIANCE_WINDOW) return false

        val avgMagnitude = magnitudeBuffer.average()
        if (abs(avgMagnitude - GRAVITY_MS2) > OFF_WRIST_MAGNITUDE_DELTA) {
            return true
        }

        return false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT) {
            sensorOnBody = event.values[0] >= 1.0f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val GRAVITY_MS2 = 9.81f
        const val OFF_WRIST_MAGNITUDE_DELTA = 2.5f
        const val VARIANCE_WINDOW = 10
    }
}
