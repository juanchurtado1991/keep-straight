package com.keepstraight.desktop.presentation

import com.keepstraight.desktop.bridge.BridgeClientException

object BridgeErrorMapper {
    fun userMessage(err: Throwable?): UserMessage = when (err) {
        is BridgeClientException -> when (err.messageKey) {
            DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED -> UserMessage(
                err.messageKey,
                listOf(err.message.orEmpty()),
            )
            else -> UserMessage(err.messageKey, override = err.message)
        }
        else -> userMessageFromDetail(err?.message)
    }

    fun userMessageFromDetail(detail: String?): UserMessage {
        val text = detail?.trim().orEmpty()
        if (text.isEmpty()) return UserMessage(DesktopMessageKey.BRIDGE_PAIRING_FAILED)
        runCatching { DesktopMessageKey.valueOf(text) }.getOrNull()?.let { return UserMessage(it) }
        return when (text) {
            "invalid_response",
            "invalid_settings",
            -> UserMessage(DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED, listOf(text))
            "401",
            "unauthorized 401",
            -> UserMessage(DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED)
            else -> when {
                text.all { it.isDigit() } ->
                    UserMessage(DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED, listOf(text))
                else -> UserMessage(DesktopMessageKey.BRIDGE_PAIRING_FAILED, override = text)
            }
        }
    }
}
