package com.keepstraight.shared.vision

import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.WebcamException
import com.github.sarxos.webcam.WebcamLockException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.awt.Dimension
import java.awt.image.BufferedImage

class JvmCameraFrameSource : CameraFrameSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var captureJob: Job? = null
    private var webcam: Webcam? = null

    private val _devices = MutableStateFlow<List<CameraDeviceInfo>>(emptyList())
    override val devices: StateFlow<List<CameraDeviceInfo>> = _devices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    override val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    private val _lastError = MutableStateFlow<CameraError?>(null)
    override val lastError: StateFlow<CameraError?> = _lastError.asStateFlow()

    private val _frames = MutableSharedFlow<CameraFrame>(extraBufferCapacity = 1)
    override val frames: Flow<CameraFrame> = _frames.asSharedFlow()

    /** Reusable buffers to limit GC pressure during capture. */
    private var rgbBuffer: ByteArray? = null
    private var argbScratch: IntArray? = null
    private var lastTargetFps: Int = 5

    init {
        WebcamBootstrap.ensureInitialized()
        refreshDevices()
    }

    override fun refreshDevices() {
        WebcamBootstrap.ensureInitialized()
        val list = try {
            Webcam.getWebcams().mapIndexed { index, cam ->
                val name = cam.name?.takeIf { it.isNotBlank() } ?: "$CAMERA_FALLBACK_PREFIX$index"
                CameraDeviceInfo(
                    id = name,
                    name = name,
                )
            }
        } catch (error: Exception) {
            System.err.println("KeepStraight: webcam enumerate failed: ${error.message}")
            emptyList()
        }
        _devices.value = list
        val selected = _selectedDeviceId.value
        if (selected == null || list.none { it.id == selected }) {
            _selectedDeviceId.value = list.firstOrNull()?.id
        }
        if (list.isEmpty()) {
            _lastError.value = emptyListError()
        } else if (
            _lastError.value == CameraError.NOT_FOUND ||
            _lastError.value == CameraError.PERMISSION_DENIED
        ) {
            _lastError.value = null
        }
    }

    override fun selectDevice(deviceId: String?) {
        selectDevice(deviceId, lastTargetFps)
    }

    fun selectDevice(deviceId: String?, targetFps: Int) {
        val wasRunning = captureJob?.isActive == true
        if (wasRunning) stop()
        _selectedDeviceId.value = deviceId
        if (wasRunning) start(targetFps)
    }

    override fun start(targetFps: Int) {
        if (captureJob?.isActive == true) return
        _lastError.value = null
        lastTargetFps = targetFps.coerceIn(2, 10)
        val fps = lastTargetFps
        val frameDelayMs = (1000L / fps).coerceAtLeast(100L)

        captureJob = scope.launch {
            val cam = openSelectedWebcam() ?: return@launch
            webcam = cam
            try {
                while (isActive) {
                    val image = try {
                        cam.image
                    } catch (_: WebcamLockException) {
                        _lastError.value = CameraError.IN_USE
                        break
                    } catch (_: WebcamException) {
                        _lastError.value = CameraError.DISCONNECTED
                        break
                    } catch (_: Exception) {
                        _lastError.value = CameraError.OPEN_FAILED
                        break
                    }
                    if (image != null) {
                        val frame = toCameraFrame(image, System.currentTimeMillis())
                        _frames.tryEmit(frame)
                    }
                    delay(frameDelayMs)
                }
            } finally {
                closeWebcam()
            }
        }
    }

    override fun stop() {
        captureJob?.cancel()
        captureJob = null
        closeWebcam()
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    private fun openSelectedWebcam(): Webcam? {
        WebcamBootstrap.ensureInitialized()
        refreshDevices()
        val id = _selectedDeviceId.value
        val cams = try {
            Webcam.getWebcams()
        } catch (error: Exception) {
            System.err.println("KeepStraight: webcam list failed on open: ${error.message}")
            _lastError.value = classifyAccessError(error.message) ?: emptyListError()
            return null
        }
        if (cams.isEmpty()) {
            _lastError.value = emptyListError()
            return null
        }
        val cam = cams.firstOrNull { (it.name ?: "") == id }
            ?: id?.toIntOrNull()?.let { idx -> cams.getOrNull(idx) }
            ?: cams.first()
        return try {
            cam.viewSize = pickViewSize(cam)
            if (!cam.isOpen) {
                val opened = cam.open(false)
                if (!opened) {
                    _lastError.value = CameraError.IN_USE
                    return null
                }
            }
            cam
        } catch (_: WebcamLockException) {
            _lastError.value = CameraError.IN_USE
            null
        } catch (error: WebcamException) {
            _lastError.value = classifyAccessError(error.message) ?: CameraError.IN_USE
            null
        } catch (error: Exception) {
            _lastError.value = classifyAccessError(error.message) ?: CameraError.OPEN_FAILED
            null
        }
    }

    /**
     * Empty enumeration is usually privacy/TCC on Mac/Windows, or missing V4L device on Linux.
     */
    private fun emptyListError(): CameraError = when {
        WebcamBootstrap.isMac() || WebcamBootstrap.isWindows() -> CameraError.PERMISSION_DENIED
        WebcamBootstrap.isUnsupportedWebcamArch() -> CameraError.OPEN_FAILED
        else -> CameraError.NOT_FOUND
    }

    private fun classifyAccessError(message: String?): CameraError? {
        val detail = message.orEmpty().lowercase()
        if (detail.isBlank()) return null
        return when {
            detail.contains("permission") ||
                detail.contains("not authorized") ||
                detail.contains("access is denied") ||
                detail.contains("access denied") ||
                detail.contains("eacces") ||
                detail.contains("privacy") ->
                CameraError.PERMISSION_DENIED
            detail.contains("busy") ||
                detail.contains("in use") ||
                detail.contains("locked") ->
                CameraError.IN_USE
            detail.contains("no such file") ||
                detail.contains("no device") ||
                detail.contains("not found") ->
                CameraError.NOT_FOUND
            else -> null
        }
    }

    private fun pickViewSize(cam: Webcam): Dimension {
        val preferred = Dimension(640, 480)
        val sizes = cam.viewSizes?.toList().orEmpty()
        if (sizes.isEmpty()) return preferred
        return sizes.minByOrNull { size ->
            val area = size.width * size.height
            val target = preferred.width * preferred.height
            abs(area - target)
        } ?: preferred
    }

    private fun closeWebcam() {
        try {
            webcam?.close()
        } catch (_: Exception) {
            // ignore
        }
        webcam = null
    }

    private fun toCameraFrame(image: BufferedImage, timestampMs: Long): CameraFrame {
        val w = image.width
        val h = image.height
        val pixelCount = w * h
        val needed = pixelCount * 3
        val buffer = rgbBuffer?.takeIf { it.size == needed } ?: ByteArray(needed).also { rgbBuffer = it }
        val argb = argbScratch?.takeIf { it.size == pixelCount }
            ?: IntArray(pixelCount).also { argbScratch = it }
        image.getRGB(0, 0, w, h, argb, 0, w)
        var i = 0
        for (px in argb) {
            buffer[i++] = ((px shr 16) and 0xFF).toByte()
            buffer[i++] = ((px shr 8) and 0xFF).toByte()
            buffer[i++] = (px and 0xFF).toByte()
        }
        return CameraFrame(width = w, height = h, rgb = buffer.copyOf(), timestampMs = timestampMs)
    }

    private fun abs(v: Int): Int = if (v < 0) -v else v

    private companion object {
        /** Matched by desktop [com.keepstraight.desktop.ui.i18n.DesktopStrings.cameraDisplayName]. */
        const val CAMERA_FALLBACK_PREFIX = "Camera "
    }
}
