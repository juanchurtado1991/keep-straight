package com.keepstraight.shared.bridge

/** Literal fragments in the LAN bridge JSON wire format (not UI strings). */
object DesktopLanJsonWire {
    private val okTruePattern = Regex(""""ok"\s*:\s*true""")
    private val okFalsePattern = Regex(""""ok"\s*:\s*false""")

    fun isOkTrue(json: String): Boolean = okTruePattern.containsMatchIn(json)

    fun isOkFalse(json: String): Boolean = okFalsePattern.containsMatchIn(json)
}
