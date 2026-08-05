package com.keepstraight.desktop.bridge

import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.shared.bridge.BridgeProtocolError

class BridgeClientException(
    val messageKey: DesktopMessageKey,
    val protocolError: BridgeProtocolError? = null,
) : Exception(protocolError?.name)

fun BridgeClientException.isUnauthorized(): Boolean =
    messageKey == DesktopMessageKey.BRIDGE_CLIENT_UNAUTHORIZED ||
        protocolError == BridgeProtocolError.UNAUTHORIZED
