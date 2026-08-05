package com.keepstraight.shared.bridge

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopLanJsonWireTest {
    @Test
    fun isOkTrue_acceptsSpacedJson() {
        assertTrue(DesktopLanJsonWire.isOkTrue("""{"ok": true}"""))
        assertTrue(DesktopLanJsonWire.isOkTrue("""{"ok":true,"token":"x"}"""))
    }

    @Test
    fun isOkTrue_rejectsFalseAndGarbage() {
        assertFalse(DesktopLanJsonWire.isOkTrue("""{"ok": false}"""))
        assertFalse(DesktopLanJsonWire.isOkTrue("not json"))
    }

    @Test
    fun parsePairResponse_distinguishesOkFalseFromInvalid() {
        val rejected = DesktopLanJson.parsePairResponse("""{"ok": false,"errorCode":"INVALID_CODE"}""")
        assertNotNull(rejected)
        assertFalse(rejected!!.ok)

        assertNull(DesktopLanJson.parsePairResponse("not json"))
        assertNull(DesktopLanJson.parsePairResponse("""{"token":"only"}"""))
    }

    @Test
    fun parsePairResponse_parsesSuccessWithSpaces() {
        val parsed = DesktopLanJson.parsePairResponse("""{"ok": true,"token":"abc"}""")
        assertNotNull(parsed)
        assertTrue(parsed!!.ok)
        assertTrue(parsed.token == "abc")
    }
}
