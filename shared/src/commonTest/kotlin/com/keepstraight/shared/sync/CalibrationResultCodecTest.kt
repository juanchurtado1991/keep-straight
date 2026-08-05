package com.keepstraight.shared.sync

import com.keepstraight.shared.model.CalibrationCaptureResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalibrationResultCodecTest {
    @Test
    fun roundTrip_ghostJson() {
        val result = CalibrationCaptureResult(
            basePitch = 12.5f,
            baseRoll = -3.25f,
            capturedAt = 1_700_000_000_123L,
        )
        val decoded = CalibrationResultCodec.decode(CalibrationResultCodec.encode(result))
        assertEquals(result, decoded)
    }

    @Test
    fun decode_legacyCsvWireFormat() {
        val legacy = "12.5,-3.25,1700000000123".encodeToByteArray()
        val decoded = CalibrationResultCodec.decode(legacy)
        assertEquals(
            CalibrationCaptureResult(
                basePitch = 12.5f,
                baseRoll = -3.25f,
                capturedAt = 1_700_000_000_123L,
            ),
            decoded,
        )
    }

    @Test
    fun decode_rejectsInvalidPayload() {
        assertFailsWith<IllegalArgumentException> {
            CalibrationResultCodec.decode("not-valid".encodeToByteArray())
        }
    }
}
