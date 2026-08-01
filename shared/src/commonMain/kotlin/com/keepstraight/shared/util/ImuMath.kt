package com.keepstraight.shared.util

import kotlin.math.atan2
import kotlin.math.sqrt

object ImuMath {
    fun pitchRollDegrees(ax: Float, ay: Float, az: Float): Pair<Float, Float> {
        val pitch = Math.toDegrees(atan2(-ax.toDouble(), sqrt((ay * ay + az * az).toDouble()))).toFloat()
        val roll = Math.toDegrees(atan2(ay.toDouble(), az.toDouble())).toFloat()
        return pitch to roll
    }
}
