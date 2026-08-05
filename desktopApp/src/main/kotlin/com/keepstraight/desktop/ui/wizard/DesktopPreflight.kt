package com.keepstraight.desktop.ui.wizard

import com.keepstraight.shared.platform.JvmOsSignals
import java.io.File

/** OS-specific first-run hints before companion sideload / LAN pairing. */
object DesktopPreflight {
    data class Hint(val title: String, val body: String)

    fun hints(): List<Hint> = buildList {
        when {
            JvmOsSignals.isWindows() -> {
                add(
                    Hint(
                        title = "Developer options on your phone",
                        body = "On Android: Settings → About phone → tap Build number 7 times, then " +
                            "Settings → Developer options → enable Wireless debugging.",
                    ),
                )
                add(
                    Hint(
                        title = "Firewall / antivirus",
                        body = "If pairing times out, allow KeepStraight and adb through Windows Defender " +
                            "Firewall on private networks.",
                    ),
                )
            }
            JvmOsSignals.isMac() -> {
                add(
                    Hint(
                        title = "First launch on Mac",
                        body = "If macOS blocks the app, right-click KeepStraight → Open once, then allow " +
                            "Camera and Local Network when prompted.",
                    ),
                )
                add(
                    Hint(
                        title = "Same Wi‑Fi",
                        body = "Phone and Mac must be on the same network for QR pairing and posture sync.",
                    ),
                )
            }
            else -> {
                if (JvmOsSignals.isLinuxAarch64()) {
                    add(
                        Hint(
                            title = "ARM64 Linux",
                            body = "Bundled adb and webcam drivers target x86_64. Companion sideload and some " +
                                "cameras may not work on ARM64 yet.",
                        ),
                    )
                }
                if (!canAccessVideoDevices()) {
                    add(
                        Hint(
                            title = "Camera access",
                            body = "Your user may need the video group: sudo usermod -aG video \$USER, " +
                                "then log out and back in.",
                        ),
                    )
                }
                if (!commandExists("notify-send")) {
                    add(
                        Hint(
                            title = "Desktop notifications",
                            body = "Install libnotify-bin (e.g. sudo apt install libnotify-bin) for slump alerts.",
                        ),
                    )
                }
                if (isWaylandSession()) {
                    add(
                        Hint(
                            title = "Wayland tray",
                            body = "On Wayland the menu-bar icon may be missing. Use Quit from the window or " +
                                "Settings instead of closing the tray.",
                        ),
                    )
                }
            }
        }
    }

    private fun canAccessVideoDevices(): Boolean {
        val videoDir = File("/dev")
        val devices = videoDir.listFiles { f -> f.name.startsWith("video") } ?: return true
        if (devices.isEmpty()) return true
        return devices.any { it.canRead() }
    }

    private fun isWaylandSession(): Boolean =
        !System.getenv("WAYLAND_DISPLAY").isNullOrBlank()

    private fun commandExists(name: String): Boolean = try {
        val p = ProcessBuilder("which", name).start()
        p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS) && p.exitValue() == 0
    } catch (_: Exception) {
        false
    }
}
