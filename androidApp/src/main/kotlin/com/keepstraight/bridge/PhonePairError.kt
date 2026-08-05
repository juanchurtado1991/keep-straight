package com.keepstraight.bridge

import androidx.annotation.StringRes
import com.keepstraight.R
import com.keepstraight.shared.bridge.BridgeProtocolError

enum class PhonePairError(@StringRes val messageRes: Int) {
    NO_WIFI(R.string.phone_pair_no_wifi),
    DESKTOP_UNREACHABLE(R.string.phone_pair_desktop_unreachable),
    INVALID_QR(R.string.desktop_qr_invalid),
    UPDATE_APP(R.string.phone_pair_update_app),
    INVALID_CODE(R.string.lan_pair_invalid_code),
    CODE_EXPIRED(R.string.lan_pair_code_expired),
    TOO_MANY_ATTEMPTS(R.string.lan_pair_too_many_attempts),
    DESKTOP_REJECTED(R.string.desktop_qr_failed),
}

fun phonePairErrorFromProtocol(code: BridgeProtocolError?): PhonePairError = when (code) {
    BridgeProtocolError.INVALID_QR -> PhonePairError.INVALID_QR
    BridgeProtocolError.UPDATE_APP -> PhonePairError.UPDATE_APP
    BridgeProtocolError.INVALID_CODE -> PhonePairError.INVALID_CODE
    BridgeProtocolError.CODE_EXPIRED -> PhonePairError.CODE_EXPIRED
    BridgeProtocolError.TOO_MANY_ATTEMPTS -> PhonePairError.TOO_MANY_ATTEMPTS
    BridgeProtocolError.UNAUTHORIZED,
    BridgeProtocolError.PAIRING_FAILED,
    BridgeProtocolError.INVALID_RESPONSE,
    BridgeProtocolError.INVALID_SETTINGS,
    BridgeProtocolError.PAIRED,
    null,
    -> PhonePairError.DESKTOP_REJECTED
}

class PhonePairException(
    val error: PhonePairError,
) : Exception(error.name)
