package com.keepstraight.desktop.adb

/** Substrings in `adb` CLI stdout/stderr — not user-visible copy. */
object AdbCliOutputSignals {
    private const val PAIR_SUCCESS = "Successfully paired"
    private const val PAIRED_TO = "paired to"
    private const val CONNECTED_TO = "connected to"
    private const val ALREADY_CONNECTED = "already connected"
    private const val INSTALL_SUCCESS = "Success"
    private const val INSTALL_INCOMPATIBLE = "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
    private const val SIGNATURE_MISMATCH = "signatures do not match"
    private const val WATCH_CHARACTERISTIC = "watch"

    fun pairedSuccessfully(output: String): Boolean =
        output.contains(PAIR_SUCCESS, ignoreCase = true) ||
            output.contains(PAIRED_TO, ignoreCase = true)

    fun connectedSuccessfully(output: String): Boolean =
        output.contains(CONNECTED_TO, ignoreCase = true) ||
            output.contains(ALREADY_CONNECTED, ignoreCase = true)

    fun installSucceeded(output: String): Boolean =
        output.contains(INSTALL_SUCCESS, ignoreCase = true)

    fun needsSignatureReinstall(output: String): Boolean =
        output.contains(INSTALL_INCOMPATIBLE, ignoreCase = true) ||
            output.contains(SIGNATURE_MISMATCH, ignoreCase = true)

    fun isWatchDevice(getpropOutput: String): Boolean =
        getpropOutput.contains(WATCH_CHARACTERISTIC, ignoreCase = true)
}
