package com.keepstraight.shared.repository

enum class DeviceSyncFailureReason {
    NO_PAIRED_WATCH,
    WATCH_UNREACHABLE,
}

/** Typed sync failure — avoids string matching in presentation layers. */
class DeviceSyncException(
    val reason: DeviceSyncFailureReason,
) : IllegalStateException(reason.name)

fun Throwable.deviceSyncFailureReason(): DeviceSyncFailureReason? =
    (this as? DeviceSyncException)?.reason
