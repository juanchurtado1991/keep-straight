package com.keepstraight.shared.bridge

/** Stable LAN / pair-assist error codes — localize on each client, never show raw on UI. */
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
