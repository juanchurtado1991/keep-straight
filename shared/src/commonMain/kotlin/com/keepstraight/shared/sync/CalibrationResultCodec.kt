package com.keepstraight.shared.sync

import com.keepstraight.shared.model.CalibrationCaptureResult

/** Plain UTF-8 payload — avoids Ghost encode/decode mismatches on the hot calibrate path. */
object CalibrationResultCodec {
    const val KEY_PITCH = "pitch"
    const val KEY_ROLL = "roll"
    const val KEY_CAPTURED_AT = "capturedAt"
    const val KEY_SENT_AT = "sentAt"

    fun encode(result: CalibrationCaptureResult): ByteArray =
        "${result.basePitch},${result.baseRoll},${result.capturedAt}".encodeToByteArray()

    fun decode(bytes: ByteArray): CalibrationCaptureResult {
        val parts = bytes.decodeToString().split(',')
        require(parts.size >= 3) { "Invalid calibration payload" }
        return CalibrationCaptureResult(
            basePitch = parts[0].toFloat(),
            baseRoll = parts[1].toFloat(),
            capturedAt = parts[2].toLong(),
        )
    }
}
