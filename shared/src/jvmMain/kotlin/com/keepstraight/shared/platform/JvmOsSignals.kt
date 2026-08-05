package com.keepstraight.shared.platform

/** Substrings of `os.name` / `os.arch` — not user-visible copy. */
object JvmOsSignals {
    private const val SIGNAL_MAC = "mac"
    private const val SIGNAL_DARWIN = "darwin"
    private const val SIGNAL_WINDOWS = "win"
    private const val SIGNAL_LINUX = "linux"
    private const val SIGNAL_AARCH64 = "aarch64"
    private const val SIGNAL_ARM64 = "arm64"

    fun osNameLower(): String = System.getProperty("os.name").orEmpty().lowercase()

    fun archLower(): String = System.getProperty("os.arch").orEmpty().lowercase()

    fun isMac(os: String = osNameLower()): Boolean =
        os.contains(SIGNAL_MAC) || os.contains(SIGNAL_DARWIN)

    fun isWindows(os: String = osNameLower()): Boolean = os.contains(SIGNAL_WINDOWS)

    fun isLinux(os: String = osNameLower()): Boolean = os.contains(SIGNAL_LINUX)

    fun isLinuxAarch64(arch: String = archLower(), os: String = osNameLower()): Boolean =
        isLinux(os) && (arch.contains(SIGNAL_AARCH64) || arch.contains(SIGNAL_ARM64))

    /** Bundled adb folder under desktop resources (`adb/macos`, etc.). */
    fun adbResourceFolder(os: String = osNameLower()): String = when {
        isMac(os) -> "macos"
        isWindows(os) -> "windows"
        else -> "linux"
    }
}
