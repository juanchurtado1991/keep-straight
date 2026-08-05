package com.keepstraight.shared.bridge

/**
 * Desktop → phone LAN client. Phase 2: optional; alerts still work locally without it.
 */
interface DesktopBridgeClient {
    val isConfigured: Boolean
    suspend fun pair(host: String, port: Int, code: String): Result<String>
    suspend fun sendEvent(event: DesktopSlumpEvent): Result<Unit>
    suspend fun fetchSettings(): Result<DesktopPhoneSettings>
    /** Best-effort notify phone to drop this desktop token before clearing local creds. */
    suspend fun notifyRemoteUnpair()
    fun clear()
}
