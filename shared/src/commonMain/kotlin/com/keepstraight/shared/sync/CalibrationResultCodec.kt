package com.keepstraight.shared.sync

import com.ghost.serialization.Ghost
import com.keepstraight.shared.model.CalibrationCaptureResult

/** Ghost JSON on the Wear Message API; DataMap keys stay manual for the Wear sync layer. */
object CalibrationResultCodec {
    const val KEY_PITCH = "pitch"
    const val KEY_ROLL = "roll"
    const val KEY_CAPTURED_AT = "capturedAt"
    const val KEY_SENT_AT = "sentAt"

    fun encode(result: CalibrationCaptureResult): ByteArray =
        Ghost.encodeToBytes(result)

    fun decode(bytes: ByteArray): CalibrationCaptureResult =
        runCatching {
            Ghost.deserialize<CalibrationCaptureResult>(bytes)
        }.getOrElse {
            decodeLegacyCsv(bytes)
        }

    private fun decodeLegacyCsv(bytes: ByteArray): CalibrationCaptureResult {
        val parts = bytes.decodeToString().split(',')
        require(parts.size >= 3) { "Invalid calibration payload" }
        return CalibrationCaptureResult(
            basePitch = parts[0].toFloat(),
            baseRoll = parts[1].toFloat(),
            capturedAt = parts[2].toLong(),
        )
    }
}
