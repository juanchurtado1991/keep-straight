package com.keepstraight.desktop.adb

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.util.concurrent.TimeUnit

enum class AdbErrorKind {
    ADB_MISSING,
    NO_DEVICE,
    PAIR_FAILED,
    CONNECT_FAILED,
    UNAUTHORIZED,
    OFFLINE,
    APK_MISSING,
    INSTALL_FAILED,
    TIMEOUT,
    INVALID_INPUT,
    UNKNOWN,
}

data class AdbDevice(
    val serial: String,
    val state: String,
    val isUnauthorized: Boolean = state.equals("unauthorized", ignoreCase = true),
    val isOffline: Boolean = state.equals("offline", ignoreCase = true),
    val isReady: Boolean = state.equals("device", ignoreCase = true),
)

sealed class AdbResult<out T> {
    data class Ok<T>(val value: T) : AdbResult<T>()
    data class Err(
        val kind: AdbErrorKind,
        val title: String,
        val body: String,
        val detail: String? = null,
    ) : AdbResult<Nothing>()
}

/**
 * Wireless-only sideload via Android **Wireless debugging** (ADB over Wi‑Fi).
 * No USB cable required: pair with code → connect → install.
 *
 * Every entry point is `suspend` and hops to [Dispatchers.IO] via [runInterruptible]: adb blocks
 * for minutes at a time, which would freeze the Compose window, and cancelling the caller has to
 * interrupt the wait instead of leaving the UI stuck behind it.
 */
class AdbInstaller(
    private val resourceRoot: File? = null,
) {
    private suspend fun <T> onIo(block: () -> T): T = runInterruptible(Dispatchers.IO, block)

    fun resolveAdb(): AdbResult<File> {
        try {
            bundledAdb()?.let { return AdbResult.Ok(it) }
            systemAdb()?.let { return AdbResult.Ok(it) }
        } catch (e: Exception) {
            return AdbResult.Err(
                kind = AdbErrorKind.UNKNOWN,
                title = "Couldn’t prepare the installer tool",
                body = "Close other Android tools (Android Studio, platform-tools), then try again. " +
                    "If it keeps failing, restart KeepStraight.",
                detail = e.message,
            )
        }
        return AdbResult.Err(
            kind = AdbErrorKind.ADB_MISSING,
            title = "Something’s missing on this computer",
            body = "KeepStraight couldn’t find the tool that installs on phone and watch. Reinstall the desktop app and try again.",
        )
    }

    /** `adb pair host:pairingPort pairingCode` — wireless debugging pairing (phone or watch). */
    suspend fun pairWireless(host: String, pairingPort: Int, pairingCode: String): AdbResult<Unit> =
        onIo { pairWirelessBlocking(host, pairingPort, pairingCode) }

    /** `adb connect host:port` using the port from the Wireless debugging screen. */
    suspend fun connectWireless(host: String, port: Int): AdbResult<Unit> =
        onIo { connectWirelessBlocking(host, port) }

    suspend fun installPhoneApk(serial: String? = null): AdbResult<Unit> =
        onIo { installApkBlocking(resolvePhoneApk(), PHONE_APK_MISSING, serial, wantWatch = false) }

    suspend fun installWearApk(serial: String? = null): AdbResult<Unit> =
        onIo { installApkBlocking(resolveWearApk(), WEAR_APK_MISSING, serial, wantWatch = true) }

    suspend fun listDevices(): AdbResult<List<AdbDevice>> = onIo { listDevicesBlocking() }

    /** Ready watches already visible to adb (IP:port or mDNS serial). */
    suspend fun listReadyWatches(): AdbResult<List<AdbDevice>> = onIo {
        val adb = when (val r = resolveAdb()) {
            is AdbResult.Ok -> r.value
            is AdbResult.Err -> return@onIo r
        }
        when (val devices = listDevicesBlocking()) {
            is AdbResult.Err -> devices
            is AdbResult.Ok -> AdbResult.Ok(
                devices.value.filter { it.isReady && isWatch(adb, it.serial) },
            )
        }
    }

    private fun pairWirelessBlocking(
        host: String,
        pairingPort: Int,
        pairingCode: String,
    ): AdbResult<Unit> {
        val h = host.trim()
        val code = pairingCode.trim()
        if (h.isEmpty() || pairingPort !in 1..65535 || code.length < 4) {
            return AdbResult.Err(
                AdbErrorKind.INVALID_INPUT,
                "Those details don’t look right",
                "Copy the IP:port and 6‑digit code from Wireless debugging on the device.",
            )
        }
        val adb = when (val r = resolveAdb()) {
            is AdbResult.Ok -> r.value
            is AdbResult.Err -> return r
        }
        val endpoint = "$h:$pairingPort"
        val output = run(adb, listOf("pair", endpoint, code), timeoutSec = 45)
            ?: return AdbResult.Err(
                AdbErrorKind.TIMEOUT,
                "That took too long",
                "Keep the device awake, Wireless debugging open, and stay on the same Wi‑Fi. Then try again.",
            )
        val ok = output.contains("Successfully paired", ignoreCase = true) ||
            output.contains("paired to", ignoreCase = true)
        return if (ok) {
            AdbResult.Ok(Unit)
        } else {
            AdbResult.Err(
                AdbErrorKind.PAIR_FAILED,
                "Couldn’t pair over Wi‑Fi",
                "Use a fresh pairing code from the device’s Wireless debugging screen. " +
                    "For phones you can also Pair device with QR code instead of typing.",
                detail = output.takeLast(300),
            )
        }
    }

    /** Port comes from the main Wireless debugging screen, not the pairing port. */
    private fun connectWirelessBlocking(host: String, port: Int): AdbResult<Unit> {
        val h = host.trim()
        if (h.isEmpty() || port !in 1..65535) {
            return AdbResult.Err(
                AdbErrorKind.INVALID_INPUT,
                "Check the connection address",
                "Use the IP and port shown at the top of Wireless debugging (e.g. 192.168.1.20:41234).",
            )
        }
        val adb = when (val r = resolveAdb()) {
            is AdbResult.Ok -> r.value
            is AdbResult.Err -> return r
        }
        val endpoint = "$h:$port"
        val output = run(adb, listOf("connect", endpoint), timeoutSec = 30)
            ?: return AdbResult.Err(
                AdbErrorKind.TIMEOUT,
                "Connect timed out",
                "Same Wi‑Fi? Leave Wireless debugging on, then retry Connect.",
            )
        val ok = output.contains("connected to", ignoreCase = true) ||
            output.contains("already connected", ignoreCase = true)
        return if (ok) {
            AdbResult.Ok(Unit)
        } else {
            AdbResult.Err(
                AdbErrorKind.CONNECT_FAILED,
                "Couldn’t connect",
                "If this is the first time, pair with the 6‑digit code first, then connect with the IP:port from Wireless debugging.",
                detail = output.takeLast(300),
            )
        }
    }

    private fun listDevicesBlocking(): AdbResult<List<AdbDevice>> {
        val adb = when (val r = resolveAdb()) {
            is AdbResult.Ok -> r.value
            is AdbResult.Err -> return r
        }
        val output = run(adb, listOf("devices"), timeoutSec = 10)
            ?: return AdbResult.Err(
                AdbErrorKind.TIMEOUT,
                "Device scan timed out",
                "ADB didn’t respond. Toggle Wireless debugging off/on and try again.",
            )
        val devices = output.lines()
            .drop(1)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("*") }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"))
                if (parts.size < 2) null
                else AdbDevice(serial = parts[0], state = parts[1])
            }
        return AdbResult.Ok(devices)
    }

    private fun installApkBlocking(
        apk: File?,
        missing: AdbResult.Err,
        serial: String?,
        wantWatch: Boolean,
    ): AdbResult<Unit> {
        if (apk == null) return missing
        val adb = when (val r = resolveAdb()) {
            is AdbResult.Ok -> r.value
            is AdbResult.Err -> return r
        }
        val devices = when (val r = listDevicesBlocking()) {
            is AdbResult.Ok -> r.value
            is AdbResult.Err -> return r
        }
        if (devices.isEmpty()) {
            return AdbResult.Err(
                AdbErrorKind.NO_DEVICE,
                "No wireless device",
                "Pair and connect with Wireless debugging first (same Wi‑Fi). Then install.",
            )
        }
        if (devices.any { it.isUnauthorized } && devices.none { it.isReady }) {
            return AdbResult.Err(
                AdbErrorKind.UNAUTHORIZED,
                "Authorize this computer",
                "On the phone/watch, allow the debugging prompt, then retry.",
            )
        }
        if (devices.any { it.isOffline } && devices.none { it.isReady }) {
            return AdbResult.Err(
                AdbErrorKind.OFFLINE,
                "Device offline",
                "Reconnect wireless debugging (Connect again), keep the screen on, and retry.",
            )
        }
        val ready = devices.filter { it.isReady }
        // adb often lists one device twice (as IP:port and as its mDNS name), and only one of the
        // two entries is usable, so an exact serial miss falls back to any ready device of the
        // right form factor rather than failing or picking the wrong device.
        val target = ready.firstOrNull { it.serial == serial && isWatch(adb, it.serial) == wantWatch }
            ?: ready.firstOrNull { isWatch(adb, it.serial) == wantWatch }
            ?: return AdbResult.Err(
                AdbErrorKind.NO_DEVICE,
                if (wantWatch) "The watch isn’t ready" else "The phone isn’t ready",
                if (wantWatch) {
                    "Keep the watch awake with Wireless debugging open on the same Wi‑Fi, then try again."
                } else {
                    "Keep the phone unlocked with Wireless debugging open, then try again."
                },
                detail = "Connected: " + devices.joinToString { "${it.serial} (${it.state})" },
            )

        val args = listOf("-s", target.serial, "install", "-r", apk.absolutePath)
        var output = run(adb, args, timeoutSec = 180)
            ?: return AdbResult.Err(
                AdbErrorKind.TIMEOUT,
                "Install timed out",
                "Stay on the same Wi‑Fi with Wireless debugging open, then retry.",
            )
        if (output.contains("Success", ignoreCase = true)) {
            launchApp(adb, target.serial, wantWatch)
            return AdbResult.Ok(Unit)
        }
        // Reinstall after a differently signed build (common when switching debug ↔ sideload).
        if (output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE", ignoreCase = true) ||
            output.contains("signatures do not match", ignoreCase = true)
        ) {
            val pkg = "com.keepstraight"
            run(adb, listOf("-s", target.serial, "uninstall", pkg), timeoutSec = 30)
            output = run(adb, args, timeoutSec = 180)
                ?: return AdbResult.Err(
                    AdbErrorKind.TIMEOUT,
                    "Install timed out",
                    "Removed the old app, but the new install timed out. Try again.",
                )
            if (output.contains("Success", ignoreCase = true)) {
                launchApp(adb, target.serial, wantWatch)
                return AdbResult.Ok(Unit)
            }
        }
        return AdbResult.Err(
            AdbErrorKind.INSTALL_FAILED,
            "Install failed",
            if (wantWatch) {
                "Couldn’t install KeepStraight on the watch. Check free storage and that Wireless debugging stays open."
            } else {
                "Couldn’t install KeepStraight on the phone. Check free storage and that Wireless debugging stays open."
            },
            detail = output.takeLast(400),
        )
    }

    /**
     * Opens KeepStraight after a successful sideload so the user lands in onboarding / monitoring
     * instead of hunting for the icon. Failures are ignored — install already succeeded.
     */
    private fun launchApp(adb: File, serial: String, wantWatch: Boolean) {
        val component = if (wantWatch) {
            "com.keepstraight/com.keepstraight.wear.MainActivity"
        } else {
            "com.keepstraight/com.keepstraight.MainActivity"
        }
        // Watches often stay dark; wake first so the activity is visible.
        run(adb, listOf("-s", serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP"), timeoutSec = 5)
        run(
            adb,
            listOf(
                "-s", serial,
                "shell", "am", "start",
                "-a", "android.intent.action.MAIN",
                "-c", "android.intent.category.LAUNCHER",
                "-n", component,
            ),
            timeoutSec = 15,
        )
    }

    /** Wear builds report `watch` here, which is what keeps the watch APK off the phone. */
    private fun isWatch(adb: File, serial: String): Boolean {
        val output = run(
            adb,
            listOf("-s", serial, "shell", "getprop", "ro.build.characteristics"),
            timeoutSec = 15,
        )
        return output?.contains("watch", ignoreCase = true) == true
    }

    private fun resolvePhoneApk(): File? = findApk("keepstraight-phone.apk", "androidApp.apk")
    private fun resolveWearApk(): File? = findApk("keepstraight-wear.apk", "wearApp.apk")

    private fun findApk(vararg names: String): File? {
        names.forEach { name ->
            extractResourceApk("apks/$name")?.let { return it }
        }
        val cwd = File(System.getProperty("user.dir") ?: ".")
        val candidates = names.flatMap {
            listOf(
                File(cwd, "desktopApp/src/main/resources/apks/$it"),
                File(cwd, "apks/$it"),
                resourceRoot?.let { root -> File(root, "apks/$it") },
            )
        }.filterNotNull()
        return candidates.firstOrNull { it.isFile }
    }

    private fun extractResourceApk(path: String): File? {
        val stream = javaClass.classLoader.getResourceAsStream(path) ?: return null
        val outDir = File(System.getProperty("java.io.tmpdir"), "keepstraight-apks")
        outDir.mkdirs()
        val out = File(outDir, File(path).name)
        if (out.isFile && out.length() > 0) {
            return out
        }
        return try {
            stream.use { input -> out.outputStream().use { output -> input.copyTo(output) } }
            out.takeIf { it.isFile && it.length() > 0 }
        } catch (_: Exception) {
            out.takeIf { it.isFile && it.length() > 0 }
        }
    }

    private fun bundledAdb(): File? {
        cachedBundledAdb?.takeIf { it.isFile && it.length() > 0 }?.let { cached ->
            ensureUnixExecutable(cached)
            return cached
        }

        synchronized(BundledAdbLock) {
            cachedBundledAdb?.takeIf { it.isFile && it.length() > 0 }?.let { cached ->
                ensureUnixExecutable(cached)
                return cached
            }

            val os = System.getProperty("os.name").orEmpty().lowercase()
            val folder = when {
                os.contains("mac") -> "macos"
                os.contains("win") -> "windows"
                else -> "linux"
            }
            val isWindows = os.contains("win")
            val exeName = if (isWindows) "adb.exe" else "adb"
            val adb = extractResourceBinary("adb/$folder/$exeName")
                ?: resourceRoot?.let { root ->
                    File(root, "adb/$folder/$exeName").takeIf { it.isFile }
                }
                ?: return null

            // Windows adb needs its WinUSB helper DLLs beside the exe.
            if (isWindows) {
                extractResourceBinary("adb/windows/AdbWinApi.dll")
                extractResourceBinary("adb/windows/AdbWinUsbApi.dll")
            } else {
                ensureUnixExecutable(adb)
            }
            cachedBundledAdb = adb
            return adb
        }
    }

    /** Linux/macOS: adb must stay executable after extract or reuse from disk. */
    private fun ensureUnixExecutable(file: File) {
        if (System.getProperty("os.name").orEmpty().lowercase().contains("win")) return
        if (!file.isFile) return
        if (!file.canExecute()) {
            file.setReadable(true, false)
            file.setExecutable(true, false)
        }
    }

    /**
     * Extract bundled adb/DLLs once under the user profile (not %TEMP%).
     * On Windows, adb.exe is locked while the adb server runs — never overwrite an existing copy.
     */
    private fun extractResourceBinary(path: String): File? {
        val outDir = adbInstallDir()
        outDir.mkdirs()
        val out = File(outDir, File(path).name)
        if (out.isFile && out.length() > 0) {
            ensureUnixExecutable(out)
            return out
        }

        val stream = javaClass.classLoader.getResourceAsStream(path) ?: return null
        return try {
            stream.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            }
            out.takeIf { it.isFile && it.length() > 0 }?.also { ensureUnixExecutable(it) }
        } catch (_: Exception) {
            // File may be locked (Windows adb server) or ETXTBSY on Linux — reuse if present.
            out.takeIf { it.isFile && it.length() > 0 }?.also { ensureUnixExecutable(it) }
        }
    }

    private fun adbInstallDir(): File {
        val home = System.getProperty("user.home").orEmpty().ifBlank { "." }
        return File(home, ".keepstraight/adb")
    }

    private fun systemAdb(): File? {
        val which = if (System.getProperty("os.name").orEmpty().lowercase().contains("win")) {
            listOf("where", "adb")
        } else {
            listOf("which", "adb")
        }
        return try {
            val p = ProcessBuilder(which).start()
            if (!p.waitFor(3, TimeUnit.SECONDS) || p.exitValue() != 0) return null
            val path = p.inputStream.bufferedReader().readLine()?.trim().orEmpty()
            File(path).takeIf { it.isFile }
        } catch (_: Exception) {
            null
        }
    }

    private fun run(adb: File, args: List<String>, timeoutSec: Long): String? {
        var process: Process? = null
        return try {
            val p = ProcessBuilder(listOf(adb.absolutePath) + args)
                .redirectErrorStream(true)
                .start()
            process = p
            val finished = p.waitFor(timeoutSec, TimeUnit.SECONDS)
            val text = p.inputStream.bufferedReader().readText()
            if (!finished) {
                p.destroyForcibly()
                null
            } else {
                text
            }
        } catch (interrupted: InterruptedException) {
            // The user cancelled (Stop waiting): kill the child instead of orphaning an adb process.
            process?.destroyForcibly()
            throw interrupted
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        private val BundledAdbLock = Any()

        @Volatile
        private var cachedBundledAdb: File? = null

        val PHONE_APK_MISSING = AdbResult.Err(
            AdbErrorKind.APK_MISSING,
            "Phone app package missing",
            "This desktop build doesn’t include the Android APK yet. Package KeepStraight with keepstraight-phone.apk, then retry.",
        )

        val WEAR_APK_MISSING = AdbResult.Err(
            AdbErrorKind.APK_MISSING,
            "Watch app package missing",
            "Wear APK isn’t bundled. Connect the watch over wireless debugging (or install from the phone) and retry when the package is available.",
        )
    }
}
