package com.keepstraight.shared.bridge

import com.ghost.serialization.annotations.GhostSerialization

/** Stable LAN / pair-assist error codes — localize on each client, never show raw on UI. */
@GhostSerialization
enum class BridgeProtocolError {
    INVALID_CODE,
    CODE_EXPIRED,
    TOO_MANY_ATTEMPTS,
    UPDATE_APP,
    PAIRED,
    INVALID_RESPONSE,
    INVALID_SETTINGS,
    UNAUTHORIZED,
    INVALID_QR,
    PAIRING_FAILED,
}
