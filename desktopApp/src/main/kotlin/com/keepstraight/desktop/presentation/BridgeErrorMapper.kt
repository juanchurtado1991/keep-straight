package com.keepstraight.desktop.presentation

object BridgeErrorMapper {
    fun userMessage(detail: String?): UserMessage {
        val text = detail?.trim().orEmpty()
        if (text.isEmpty()) return UserMessage(DesktopMessageKey.BRIDGE_PAIRING_FAILED)
        return when {
            text.contains("401") || text.contains("unauthorized", ignoreCase = true) ->
                UserMessage(DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED)
            text.contains("Pairing rejected", ignoreCase = true) ->
                UserMessage(DesktopMessageKey.BRIDGE_CLIENT_PAIR_REJECTED)
            text.contains("missing token", ignoreCase = true) ->
                UserMessage(DesktopMessageKey.BRIDGE_CLIENT_MISSING_TOKEN)
            text.contains("Not paired", ignoreCase = true) ->
                UserMessage(DesktopMessageKey.BRIDGE_CLIENT_NOT_PAIRED)
            text.contains("Pairing failed", ignoreCase = true) ->
                UserMessage(DesktopMessageKey.BRIDGE_CLIENT_PAIR_FAILED, listOf(extractStatus(text)))
            else -> UserMessage(DesktopMessageKey.BRIDGE_PAIRING_FAILED, override = text)
        }
    }

    private fun extractStatus(text: String): String {
        val open = text.indexOf('(')
        val close = text.lastIndexOf(')')
        return if (open >= 0 && close > open) {
            text.substring(open + 1, close)
        } else {
            text
        }
    }
}
