package com.keepstraight.shared.sync

object ConnectionRetryPolicy {
    const val RETRY_INTERVAL_MS = 15 * 60 * 1000L
    const val MAX_RETRY_DURATION_MS = 2 * 60 * 60 * 1000L
}
