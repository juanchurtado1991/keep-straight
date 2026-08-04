package com.keepstraight.shared.util

class FixedSampleBuffer(private val capacity: Int) {
    private val values = FloatArray(capacity)
    private var size: Int = 0
    private var index: Int = 0

    fun add(value: Float) {
        values[index] = value
        index = (index + 1) % capacity
        if (size < capacity) size++
    }

    fun size(): Int = size

    fun average(): Float {
        if (size == 0) return 0f
        var sum = 0f
        for (i in 0 until size) {
            sum += values[i]
        }
        return sum / size
    }

    fun variance(): Float {
        if (size == 0) return 0f
        val avg = average()
        var sumSq = 0f
        for (i in 0 until size) {
            val delta = values[i] - avg
            sumSq += delta * delta
        }
        return sumSq / size
    }

    fun clear() {
        size = 0
        index = 0
    }
}
