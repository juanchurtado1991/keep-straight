package com.keepstraight.desktop

import com.keepstraight.desktop.presentation.DesktopMessageKey
import com.keepstraight.desktop.ui.i18n.DesktopMessageJvm
import java.io.File
import java.util.concurrent.TimeUnit

object LoginItemManager {

    data class Result(
        val enabled: Boolean,
        val messageKey: DesktopMessageKey? = null,
        val override: String? = null,
    )

    private data class LaunchSpec(
        val programArgs: List<String>,
        val workingDirectory: String?,
        val markerPath: String,
    )

    private const val LABEL = "com.keepstraight.desktop"
    private const val RUN_VALUE = "KeepStraight"
    private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"

    fun isAvailable(): Boolean = resolveLaunchSpec() != null

    fun isEnabled(): Boolean {
        resolveLaunchSpec() ?: return false
        return when {
            isMac -> macPlist().isFile
            isWindows -> windowsWrapper().isFile && queryWindowsRunEntry() != null
            else -> linuxDesktopEntry().isFile
        }
    }

    fun setEnabled(enabled: Boolean): Result {
        val spec = resolveLaunchSpec() ?: return Result(
            enabled = false,
            messageKey = DesktopMessageKey.LOGIN_NO_LAUNCHER,
        )
        return try {
            val ok = when {
                isMac -> if (enabled) writeMacPlist(spec) else removeMac()
                isWindows -> if (enabled) writeWindows(spec) else removeWindows()
                else -> if (enabled) writeLinux(spec) else removeLinux()
            }
            when {
                ok && enabled -> Result(true, DesktopMessageKey.LOGIN_ENABLED)
                ok -> Result(false, DesktopMessageKey.LOGIN_DISABLED)
                enabled -> Result(false, DesktopMessageKey.LOGIN_REGISTER_FAILED)
                else -> Result(false, DesktopMessageKey.LOGIN_DISABLED)
            }
        } catch (e: Exception) {
            Result(
                enabled = isEnabled(),
                messageKey = DesktopMessageKey.LOGIN_CHANGE_FAILED,
                override = e.message,
            )
        }
    }

    fun resolveMessage(result: Result): String? {
        val key = result.messageKey ?: return null
        return result.override ?: DesktopMessageJvm.text(key)
    }

    private fun resolveLaunchSpec(): LaunchSpec? {
        packagedSpec()?.let { return it }
        gradleSpec()?.let { return it }
        return processHandleSpec()
    }

    private fun packagedSpec(): LaunchSpec? {
        val path = System.getProperty("jpackage.app-path")?.takeIf { File(it).exists() } ?: return null
        val executable = if (isMac && path.endsWith(".app")) {
            macExecutable(path)
        } else {
            path
        }
        if (!File(executable).exists() && !path.endsWith(".app")) return null
        return LaunchSpec(
            programArgs = listOf(executable),
            workingDirectory = null,
            markerPath = executable,
        )
    }

    private fun gradleSpec(): LaunchSpec? {
        val root = findProjectRoot() ?: return null
        val wrapper = if (isWindows) File(root, "gradlew.bat") else File(root, "gradlew")
        if (!wrapper.isFile) return null
        return LaunchSpec(
            programArgs = listOf(wrapper.absolutePath, ":desktopApp:run", "-Pkeepstraight.skipApkSync=true"),
            workingDirectory = root.absolutePath,
            markerPath = wrapper.absolutePath,
        )
    }

    private fun processHandleSpec(): LaunchSpec? {
        return try {
            val info = ProcessHandle.current().info()
            val command = info.command().orElse(null) ?: return null
            val args = info.arguments().orElse(emptyArray()).toList()
            if (command.isBlank()) return null
            LaunchSpec(
                programArgs = listOf(command) + args,
                workingDirectory = System.getProperty("user.dir"),
                markerPath = command,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun findProjectRoot(): File? {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(8) {
            val wrapper = if (isWindows) File(dir, "gradlew.bat") else File(dir, "gradlew")
            val settings = File(dir, "settings.gradle.kts")
            if (wrapper.isFile && settings.isFile) return dir
            dir = dir.parentFile ?: return null
        }
        return null
    }

    private val isMac get() = osName.contains("mac")
    private val isWindows get() = osName.contains("win")
    private val osName get() = System.getProperty("os.name").orEmpty().lowercase()

    private fun macPlist(): File =
        File(System.getProperty("user.home"), "Library/LaunchAgents/$LABEL.plist")

    private fun writeMacPlist(spec: LaunchSpec): Boolean {
        val plist = macPlist()
        plist.parentFile?.mkdirs()
        val argsXml = spec.programArgs.joinToString("\n") { "              <string>${xmlEscape(it)}</string>" }
        val workingXml = spec.workingDirectory?.let {
            """
              <key>WorkingDirectory</key>
              <string>${xmlEscape(it)}</string>
            """.trimIndent()
        }.orEmpty()
        plist.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
              <key>Label</key>
              <string>$LABEL</string>
              <key>ProgramArguments</key>
              <array>
$argsXml
              </array>
$workingXml
              <key>RunAtLoad</key>
              <true/>
            </dict>
            </plist>
            """.trimIndent() + "\n",
        )
        runCapture(listOf("launchctl", "unload", plist.absolutePath), timeoutSec = 5)
        runCapture(listOf("launchctl", "load", plist.absolutePath), timeoutSec = 5)
        return plist.isFile
    }

    private fun removeMac(): Boolean {
        val plist = macPlist()
        if (plist.isFile) {
            runCapture(listOf("launchctl", "unload", plist.absolutePath), timeoutSec = 5)
            plist.delete()
        }
        return !plist.isFile
    }

    private fun macExecutable(path: String): String {
        val name = File(path).nameWithoutExtension
        val direct = File(path, "Contents/MacOS/$name")
        if (direct.isFile) return direct.absolutePath
        val macos = File(path, "Contents/MacOS")
        return macos.listFiles()?.firstOrNull { it.isFile && it.canExecute() }?.absolutePath ?: path
    }

    private fun windowsWrapper(): File =
        File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "KeepStraight\\start-at-login.cmd")

    private fun writeWindows(spec: LaunchSpec): Boolean {
        val wrapper = windowsWrapper()
        wrapper.parentFile?.mkdirs()
        val cd = spec.workingDirectory?.let { "cd /d \"${it.replace("\"", "")}\"\r\n" }.orEmpty()
        val cmdline = spec.programArgs.joinToString(" ") { arg ->
            if (arg.any { it.isWhitespace() }) "\"$arg\"" else arg
        }
        wrapper.writeText("@echo off\r\n$cd$cmdline\r\n")
        val ok = runCapture(
            listOf(
                "reg", "add", RUN_KEY,
                "/v", RUN_VALUE,
                "/t", "REG_SZ",
                "/d", wrapper.absolutePath,
                "/f",
            ),
            timeoutSec = 8,
        ) != null
        return ok && wrapper.isFile
    }

    private fun removeWindows(): Boolean {
        runCapture(listOf("reg", "delete", RUN_KEY, "/v", RUN_VALUE, "/f"), timeoutSec = 8)
        windowsWrapper().delete()
        return queryWindowsRunEntry() == null
    }

    private fun queryWindowsRunEntry(): String? {
        val out = runCapture(
            listOf("reg", "query", RUN_KEY, "/v", RUN_VALUE),
            timeoutSec = 8,
        ) ?: return null
        return out.takeIf { it.contains(RUN_VALUE) }
    }

    private fun linuxDesktopEntry(): File =
        File(System.getProperty("user.home"), ".config/autostart/keepstraight.desktop")

    private fun writeLinux(spec: LaunchSpec): Boolean {
        val entry = linuxDesktopEntry()
        entry.parentFile?.mkdirs()
        val exec = spec.programArgs.joinToString(" ") { shellQuote(it) }
        val lines = buildString {
            appendLine("[Desktop Entry]")
            appendLine("Type=Application")
            appendLine("Name=KeepStraight")
            appendLine("Exec=$exec")
            spec.workingDirectory?.let { appendLine("Path=$it") }
            appendLine("X-GNOME-Autostart-enabled=true")
        }
        entry.writeText(lines)
        return entry.isFile
    }

    private fun removeLinux(): Boolean {
        linuxDesktopEntry().delete()
        return !linuxDesktopEntry().isFile
    }

    private fun shellQuote(value: String): String =
        if (value.any { it.isWhitespace() || it == '"' || it == '\'' }) {
            "'" + value.replace("'", "'\\''") + "'"
        } else {
            value
        }

    private fun runCapture(command: List<String>, timeoutSec: Long): String? {
        return try {
            val p = ProcessBuilder(command).redirectErrorStream(true).start()
            val finished = p.waitFor(timeoutSec, TimeUnit.SECONDS)
            val text = p.inputStream.bufferedReader().readText()
            if (!finished) {
                p.destroyForcibly()
                null
            } else if (p.exitValue() == 0) {
                text
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun xmlEscape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
}
