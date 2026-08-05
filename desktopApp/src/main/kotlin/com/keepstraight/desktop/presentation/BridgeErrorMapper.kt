package com.keepstraight.desktop.presentation

import com.keepstraight.desktop.bridge.BridgeClientException
import com.keepstraight.shared.bridge.BridgeProtocolError

object BridgeErrorMapper {
    fun userMessage(err: Throwable?): UserMessage = when (err) {
        is BridgeClientException -> userMessageFromProtocol(err.protocolError)
            ?: UserMessage(err.messageKey)
        else -> userMessageFromProtocol(
            err?.message?.let { raw ->
                runCatching { BridgeProtocolError.valueOf(raw.trim()) }.getOrNull()
            },
        ) ?: UserMessage(DesktopMessageKey.BRIDGE_PAIRING_FAILED)
    }

    fun userMessageFromProtocol(error: BridgeProtocolError?): UserMessage? {
        if (error == null) return null
        return when (error) {
            BridgeProtocolError.INVALID_CODE,
            BridgeProtocolError.CODE_EXPIRED,
            BridgeProtocolError.TOO_MANY_ATTEMPTS,
            BridgeProtocolError.PAIRING_FAILED,
            -> UserMessage(DesktopMessageKey.BRIDGE_PAIRING_FAILED)
            BridgeProtocolError.UPDATE_APP -> UserMessage(DesktopMessageKey.PAIR_ASSIST_UPDATE_APP)
            BridgeProtocolError.PAIRED -> UserMessage(DesktopMessageKey.BRIDGE_PAIRED_PROTOCOL)
            BridgeProtocolError.INVALID_QR -> UserMessage(DesktopMessageKey.PAIR_ASSIST_INVALID_QR)
            BridgeProtocolError.UNAUTHORIZED -> UserMessage(DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED)
            BridgeProtocolError.INVALID_RESPONSE,
            BridgeProtocolError.INVALID_SETTINGS,
            -> UserMessage(DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED)
        }
    }
}
