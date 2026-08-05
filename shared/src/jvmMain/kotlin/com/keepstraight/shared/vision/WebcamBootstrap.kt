package com.keepstraight.shared.vision

import com.github.eduramiba.webcamcapture.drivers.NativeDriver
import com.github.sarxos.webcam.Webcam
import com.keepstraight.shared.platform.JvmOsSignals

/**
 * Installs a native webcam driver before any enumeration:
 * - macOS → AVFoundation (`libvideocapture`)
 * - Windows → Nokhwa/Media Foundation, with DirectShow fallback
 * - Linux → Nokhwa/V4L2 (`libcnokhwa`)
 *
 * The default BridJ/OpenIMAJ driver is unreliable on modern OS builds.
 */
object WebcamBootstrap {
    enum class OsFamily { MAC, WINDOWS, LINUX, OTHER }

    @Volatile
    private var initialized = false

    @Volatile
    private var activeDriverLabel: String = "uninitialized"

    fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            activeDriverLabel = installNativeDriver()
            initialized = true
            System.err.println(
                "KeepStraight: webcam driver = $activeDriverLabel (${osFamily()} / ${JvmOsSignals.archLower()})",
            )
        }
    }

    fun activeDriver(): String = activeDriverLabel

    fun osFamily(): OsFamily = when {
        JvmOsSignals.isMac() -> OsFamily.MAC
        JvmOsSignals.isWindows() -> OsFamily.WINDOWS
        JvmOsSignals.isLinux() -> OsFamily.LINUX
        else -> OsFamily.OTHER
    }

    fun isMac(): Boolean = osFamily() == OsFamily.MAC
    fun isWindows(): Boolean = osFamily() == OsFamily.WINDOWS
    fun isLinux(): Boolean = osFamily() == OsFamily.LINUX

    fun isUnsupportedWebcamArch(): Boolean = JvmOsSignals.isLinuxAarch64()

    private fun installNativeDriver(): String {
        if (isUnsupportedWebcamArch()) {
            System.err.println(
                "KeepStraight: no native webcam driver for Linux aarch64 — " +
                    "camera may not work. Use an x86_64 build or a USB cam with BridJ fallback.",
            )
        }
        return try {
            when (osFamily()) {
                OsFamily.WINDOWS -> installWindowsDriver()
                else -> {
                    Webcam.setDriver(NativeDriver())
                    "native/${osFamily().name.lowercase()}"
                }
            }
        } catch (t: Throwable) {
            System.err.println(
                "KeepStraight: native webcam driver failed (${t.javaClass.simpleName}: ${t.message}); " +
                    "falling back to BridJ (often broken on modern OS).",
            )
            "bridj-fallback"
        }
    }

    /**
     * Prefer Nokhwa; if it loads but finds zero devices, retry DirectShow —
     * some Windows USB cameras only enumerate through the classic path.
     */
    private fun installWindowsDriver(): String {
        Webcam.setDriver(NativeDriver(NativeDriver.WindowsBackend.NOKHWA))
        val nokhwaCount = runCatching { Webcam.getWebcams().size }.getOrDefault(-1)
        if (nokhwaCount > 0) return "native/windows-nokhwa ($nokhwaCount devices)"

        System.err.println(
            "KeepStraight: Windows Nokhwa found $nokhwaCount device(s); trying DirectShow…",
        )
        System.setProperty(NativeDriver.WINDOWS_BACKEND_PROPERTY, "directshow")
        Webcam.setDriver(NativeDriver(NativeDriver.WindowsBackend.DIRECTSHOW))
        val dshowCount = runCatching { Webcam.getWebcams().size }.getOrDefault(-1)
        if (dshowCount > 0) return "native/windows-directshow ($dshowCount devices)"

        // Leave DirectShow installed; NativeDriver still auto-swaps on LinkageError.
        return "native/windows-directshow (0 devices)"
    }
}
