package com.keepstraight.shared.sync

/** Shared phone↔watch timing so capture does not time out on one side only. */
object SyncTiming {
    const val CALIBRATION_CAPTURE_TIMEOUT_MS = 20_000L
    /** Ignore persistent Data Layer calibrate requests older than this. */
    const val CALIBRATE_REQUEST_STALE_MS = 60_000L
}
