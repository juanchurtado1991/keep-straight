package com.keepstraight.desktop.alert

import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.shared.platform.JvmOsSignals
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Native OS notifications only (no in-app toasts):
 * - macOS: bundled KeepStraightNotify.app (UserNotifications) — works in background
 * - Windows: PowerShell toast
 * - Linux: notify-send
 *
 * Optional AWT tray [displayMessage] is a secondary fallback when the platform helper fails.
 */
object NativeDesktopNotifier {

    data class Result(
        val shown: Boolean,
        val limited: Boolean,
        val detailKey: DesktopMessageKey? = null,
    )

    fun interface TrayNotifier {
        fun show(title: String, body: String): Boolean
    }

    private val trayNotifier = AtomicReference<TrayNotifier?>(null)

    fun bindTray(notifier: TrayNotifier?) {
        trayNotifier.set(notifier)
    }

    fun notify(title: String, body: String): Result {
        return try {
            when {
                JvmOsSignals.isMac() -> macNotify(title, body)
                JvmOsSignals.isWindows() -> windowsNotify(title, body)
                else -> linuxNotify(title, body)
            }
        } catch (_: Exception) {
            Result(shown = false, limited = true, detailKey = DesktopMessageKey.NOTIFIER_GENERIC_FAILURE)
        }
    }

    fun isLikelySupported(): Boolean = when {
        JvmOsSignals.isMac() -> resolveMacNotifyApp() != null
        JvmOsSignals.isWindows() -> true
        else -> commandExists("notify-send")
    }

    private fun macNotify(title: String, body: String): Result {
        val app = resolveMacNotifyApp()
        if (app != null) {
            // Launch via Launch Services (not as a Java child) so macOS treats it as a
            // normal app for Notification Center. open(1) returns when launch succeeds.
            val launched = runProcess(
                listOf("open", "-n", "-a", app.absolutePath, "--args", title, body),
                timeoutSec = 10,
            )
            if (launched) {
                return Result(shown = true, limited = false)
            }
            val binary = File(app, "Contents/MacOS/KeepStraightNotify")
            if (binary.isFile) {
                val direct = runCapture(
                    listOf(binary.absolutePath, title, body),
                    timeoutSec = 60,
                )
                if (direct != null && direct.exitCode == 0) {
                    return Result(shown = true, limited = false)
                }
                val directDetail = direct?.text?.trim().orEmpty()
                if (directDetail.contains(MAC_NOTIFY_DENIED_SIGNAL, ignoreCase = true)) {
                    openMacNotificationSettings()
                    val tray = trayNotifier.get()?.show(title, body) == true
                    return Result(
                        shown = tray,
                        limited = true,
                        detailKey = DesktopMessageKey.NOTIFIER_MAC_BLOCKED,
                    )
                }
                val tray = trayNotifier.get()?.show(title, body) == true
                if (tray) {
                    return Result(
                        shown = true,
                        limited = true,
                        detailKey = DesktopMessageKey.NOTIFIER_TRAY_FALLBACK,
                    )
                }
                return Result(
                    shown = false,
                    limited = true,
                    detailKey = DesktopMessageKey.NOTIFIER_MAC_HELPER_FAILED,
                )
            }
        }

        val tray = trayNotifier.get()?.show(title, body) == true
        if (tray) {
            return Result(shown = true, limited = true, detailKey = DesktopMessageKey.NOTIFIER_TRAY_FALLBACK)
        }
        return Result(
            shown = false,
            limited = true,
            detailKey = DesktopMessageKey.NOTIFIER_MAC_HELPER_MISSING,
        )
    }

    private fun openMacNotificationSettings() {
        // Best-effort deep link; ignore failures on older macOS builds.
        runProcess(
            listOf(
                "open",
                "x-apple.systempreferences:com.apple.Notifications-Settings.extension",
            ),
            timeoutSec = 5,
        )
    }

    private fun windowsNotify(title: String, body: String): Result {
        val ps = """
            [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] > ${'$'}null
            ${'$'}template = [Windows.UI.Notifications.ToastNotificationManager]::GetTemplateContent([Windows.UI.Notifications.ToastTemplateType]::ToastText02)
            ${'$'}text = ${'$'}template.GetElementsByTagName('text')
            ${'$'}text.Item(0).AppendChild(${'$'}template.CreateTextNode(${psString(title)})) > ${'$'}null
            ${'$'}text.Item(1).AppendChild(${'$'}template.CreateTextNode(${psString(body)})) > ${'$'}null
            ${'$'}toast = [Windows.UI.Notifications.ToastNotification]::new(${'$'}template)
            [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier('com.keepstraight.desktop').Show(${'$'}toast)
        """.trimIndent()
        val ok = runProcess(
            listOf(
                "powershell",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy", "Bypass",
                "-Command", ps,
            ),
            timeoutSec = 8,
        )
        if (ok) return Result(shown = true, limited = false)
        val tray = trayNotifier.get()?.show(title, body) == true
        return Result(
            shown = tray,
            limited = !tray,
            detailKey = if (tray) {
                DesktopMessageKey.NOTIFIER_TRAY_FALLBACK
            } else {
                DesktopMessageKey.NOTIFIER_WINDOWS_FAILED
            },
        )
    }

    private fun linuxNotify(title: String, body: String): Result {
        if (commandExists("notify-send")) {
            val ok = runProcess(
                listOf("notify-send", "--app-name=KeepStraight", title, body),
                timeoutSec = 5,
            )
            if (ok) return Result(shown = true, limited = false)
        }
        val tray = trayNotifier.get()?.show(title, body) == true
        return Result(
            shown = tray,
            limited = !tray,
            detailKey = if (tray) {
                DesktopMessageKey.NOTIFIER_TRAY_FALLBACK
            } else {
                DesktopMessageKey.NOTIFIER_LINUX_NOTIFY_MISSING
            },
        )
    }

    /**
     * Extract the bundled .app under ~/Applications so Notification Center actually
     * shows banners (Application Support copies are often authorized but silent).
     */
    private fun resolveMacNotifyApp(): File? {
        val appsDir = File(System.getProperty("user.home"), "Applications")
        appsDir.mkdirs()
        val support = File(appsDir, "KeepStraightNotify.app")
        val binary = File(support, "Contents/MacOS/KeepStraightNotify")
        if (ensureMacNotifyApp(support) && binary.isFile) {
            binary.setExecutable(true)
            runProcess(
                listOf(
                    "/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister",
                    "-f",
                    support.absolutePath,
                ),
                timeoutSec = 5,
            )
            return support
        }
        return null
    }

    private fun ensureMacNotifyApp(destApp: File): Boolean {
        val marker = File(destApp, "Contents/MacOS/KeepStraightNotify")
        val base = "notify/macos/KeepStraightNotify.app"
        val classLoader = javaClass.classLoader
        val resourceBinary = classLoader.getResourceAsStream("$base/Contents/MacOS/KeepStraightNotify")
        val resourceBytes = resourceBinary?.use { it.readBytes() }
        val needsRefresh = when {
            !marker.isFile || marker.length() == 0L -> true
            resourceBytes != null && marker.length() != resourceBytes.size.toLong() -> true
            else -> false
        }
        if (!needsRefresh) return true

        destApp.parentFile?.mkdirs()
        runCatching { if (destApp.exists()) destApp.deleteRecursively() }

        var wrote = false
        if (resourceBytes != null) {
            val files = listOf(
                "Contents/Info.plist",
                "Contents/MacOS/KeepStraightNotify",
            )
            for (rel in files) {
                val bytes = if (rel.endsWith("KeepStraightNotify")) {
                    resourceBytes
                } else {
                    classLoader.getResourceAsStream("$base/$rel")?.use { it.readBytes() } ?: continue
                }
                val out = File(destApp, rel)
                out.parentFile?.mkdirs()
                out.writeBytes(bytes)
                if (rel.endsWith("KeepStraightNotify")) out.setExecutable(true)
                wrote = true
            }
        }
        if (!wrote) {
            // Dev fallback: load from source tree when running without packaged resources.
            val cwd = File(System.getProperty("user.dir") ?: ".")
            val src = File(cwd, "desktopApp/src/main/resources/$base")
            if (src.isDirectory) {
                src.copyRecursively(destApp, overwrite = true)
                File(destApp, "Contents/MacOS/KeepStraightNotify").setExecutable(true)
                wrote = true
            }
        }
        if (wrote) {
            // Ad-hoc sign so Gatekeeper/TCC accepts the extracted copy.
            runProcess(
                listOf("codesign", "--force", "--deep", "--sign", "-", destApp.absolutePath),
                timeoutSec = 10,
            )
        }
        return wrote && marker.isFile
    }

    private data class ProcOut(val exitCode: Int, val text: String)

    private fun runCapture(command: List<String>, timeoutSec: Long): ProcOut? {
        return try {
            val p = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val finished = p.waitFor(timeoutSec, TimeUnit.SECONDS)
            val text = p.inputStream.bufferedReader().readText()
            if (!finished) {
                p.destroyForcibly()
                null
            } else {
                ProcOut(p.exitValue(), text)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun runProcess(command: List<String>, timeoutSec: Long): Boolean {
        return try {
            val p = ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
            val finished = p.waitFor(timeoutSec, TimeUnit.SECONDS)
            if (!finished) {
                p.destroyForcibly()
                false
            } else {
                p.exitValue() == 0
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun commandExists(name: String): Boolean {
        return try {
            val p = ProcessBuilder("which", name).start()
            p.waitFor(3, TimeUnit.SECONDS) && p.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    private fun psString(value: String): String =
        "'" + value.replace("'", "''") + "'"

    private const val MAC_NOTIFY_DENIED_SIGNAL = "denied"
}
