package com.keepstraight.shared.bridge

/** Literal fragments in the LAN bridge JSON wire format (not UI strings). */
object DesktopLanJsonWire {
    const val OK_TRUE_JSON = """"ok":true"""

    fun isOkTrue(json: String): Boolean = json.contains(OK_TRUE_JSON)
}
