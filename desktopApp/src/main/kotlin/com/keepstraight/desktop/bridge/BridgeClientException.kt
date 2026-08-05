package com.keepstraight.desktop.bridge

import com.keepstraight.desktop.presentation.DesktopMessageKey

class BridgeClientException(
    val messageKey: DesktopMessageKey,
    detail: String? = null,
) : Exception(detail ?: messageKey.name)
