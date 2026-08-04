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
    private val offBodySensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)
    private val magnitudeBuffer = FixedSampleBuffer(VARIANCE_WINDOW)
    private var sensorOnBody: Boolean? = null
    private var accelerometerSamples = 0
    private var lastStepCount = -1
    private var stillSinceMs = -1L
    private var lastSampleTimeMs = 0L
    private var lastAx = 0f
    private var lastAy = 0f
    private var lastAz = 0f

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

    fun onAccelerometerSample(
        ax: Float,
        ay: Float,
        az: Float,
        stepCount: Int = lastStepCount.coerceAtLeast(0),
        timestampMs: Long = android.os.SystemClock.elapsedRealtime(),
    ) {
        lastAx = ax
        lastAy = ay
        lastAz = az
        lastSampleTimeMs = timestampMs

        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        magnitudeBuffer.add(magnitude)
        accelerometerSamples++

        val stepsChanged = lastStepCount >= 0 && stepCount != lastStepCount
        lastStepCount = stepCount

        val variance = if (magnitudeBuffer.size() >= VARIANCE_WINDOW) {
            magnitudeBuffer.variance()
        } else {
            Float.MAX_VALUE
        }
        val microMovement = microMovementDelta(ax, ay, az)
        val quiet = variance <= NEAR_ZERO_VARIANCE && microMovement <= MICRO_MOVEMENT_MAX

        if (quiet && !stepsChanged) {
            if (stillSinceMs < 0L) {
                stillSinceMs = timestampMs
            }
        } else {
            stillSinceMs = -1L
        }
    }

    fun isOffWrist(): Boolean {
        sensorOnBody?.let { onBody -> return !onBody }

        if (accelerometerSamples < VARIANCE_WINDOW) return false

        val stillDuration = if (stillSinceMs >= 0L) {
            lastSampleTimeMs - stillSinceMs
        } else {
            0L
        }

        // Near-zero variance for > 60 s with no step motion → desk/charger.
        if (stillDuration >= STILL_OFF_WRIST_MS) {
            return true
        }

        // Stable face-up flat + lack of wrist micro-movements.
        if (isFaceUpFlat(lastAx, lastAy, lastAz) &&
            magnitudeBuffer.variance() <= NEAR_ZERO_VARIANCE &&
            stillDuration >= FACE_UP_HOLD_MS
        ) {
            return true
        }

        val avgMagnitude = magnitudeBuffer.average()
        if (abs(avgMagnitude - GRAVITY_MS2) > OFF_WRIST_MAGNITUDE_DELTA &&
            magnitudeBuffer.variance() <= NEAR_ZERO_VARIANCE
        ) {
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

    private fun isFaceUpFlat(ax: Float, ay: Float, az: Float): Boolean {
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        if (magnitude < 1f) return false
        val absAz = abs(az)
        val horizontal = sqrt(ax * ax + ay * ay)
        return absAz / magnitude >= FACE_UP_AZ_RATIO &&
            horizontal / magnitude <= FACE_UP_HORIZONTAL_MAX &&
            abs(magnitude - GRAVITY_MS2) <= GRAVITY_STABLE_BAND
    }

    private fun microMovementDelta(ax: Float, ay: Float, az: Float): Float {
        // Proxy: deviation of each axis from a pure gravity vector along the dominant axis.
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        return abs(magnitude - GRAVITY_MS2)
    }

    private companion object {
        const val GRAVITY_MS2 = 9.81f
        const val OFF_WRIST_MAGNITUDE_DELTA = 2.5f
        const val GRAVITY_STABLE_BAND = 1.5f
        const val VARIANCE_WINDOW = 20
        const val NEAR_ZERO_VARIANCE = 0.04f
        const val MICRO_MOVEMENT_MAX = 0.4f
        const val STILL_OFF_WRIST_MS = 60_000L
        const val FACE_UP_HOLD_MS = 15_000L
        const val FACE_UP_AZ_RATIO = 0.9f
        const val FACE_UP_HORIZONTAL_MAX = 0.35f
    }
}
