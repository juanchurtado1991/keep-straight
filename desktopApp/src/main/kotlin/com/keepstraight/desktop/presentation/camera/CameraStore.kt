package com.keepstraight.desktop.presentation.camera

import androidx.compose.ui.graphics.ImageBitmap
import com.keepstraight.desktop.ui.FramePreview
import com.keepstraight.shared.domain.CalibrationPhase
import com.keepstraight.shared.domain.DesktopPostureSession
import com.keepstraight.shared.domain.DesktopSessionPhase
import com.keepstraight.shared.vision.CameraFrameSource
import com.keepstraight.shared.vision.JvmCameraFrameSource
import com.keepstraight.shared.vision.MissingModelPoseEstimator
import com.keepstraight.shared.vision.PoseEstimator
import com.keepstraight.shared.vision.VisionPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.prefs.Preferences

class CameraStore(
    private val prefs: Preferences,
    private val session: DesktopPostureSession,
    private val scope: CoroutineScope,
) {
    private var camera: CameraFrameSource? = null
    private var poseEstimator: PoseEstimator? = null
    private var pipelineJob: Job? = null
    private val visionLock = Any()
    private var lastTargetFps: Int = 5
    private var modelBytesLoaded: Boolean = false

    val devices get() = camera?.devices
    val selectedDeviceId get() = camera?.selectedDeviceId

    private val _showPreview = MutableStateFlow(prefs.getBoolean("show_preview", false))
    val showPreview: StateFlow<Boolean> = _showPreview.asStateFlow()

    /** Full-screen calibration UI forces live preview regardless of the home toggle. */
    private val _calibrationUiActive = MutableStateFlow(false)
    val calibrationUiActive: StateFlow<Boolean> = _calibrationUiActive.asStateFlow()

    private val _previewBitmap = MutableStateFlow<ImageBitmap?>(null)
    val previewBitmap: StateFlow<ImageBitmap?> = _previewBitmap.asStateFlow()

    private val _lowPower = MutableStateFlow(prefs.getBoolean("low_power", false))
    val lowPower: StateFlow<Boolean> = _lowPower.asStateFlow()

    fun initIfConsentGranted() {
        if (prefs.getBoolean("camera_consent_accepted", false)) {
            warmUpVision()
        }
    }

    fun onCameraConsentGranted() {
        prefs.putBoolean("camera_consent_accepted", true)
        warmUpVision()
    }

    fun warmUpVision() {
        // Loading the pose model and enumerating webcams takes seconds — keep off the UI thread.
        scope.launch {
            ensurePose()
            ensureCamera()
            if (_showPreview.value) {
                startCameraPipeline()
            }
        }
    }

    fun enterCalibrationUi() {
        _calibrationUiActive.value = true
        ensureModelReady()
        restartCameraPipeline()
    }

    fun onExitCalibrationUi() {
        _calibrationUiActive.value = false
        if (!_showPreview.value) {
            _previewBitmap.value = null
        }
        if (!needsCamera()) stopCameraPipeline()
    }

    fun setShowPreview(value: Boolean) {
        prefs.putBoolean("show_preview", value)
        _showPreview.value = value
        if (value) {
            startCameraPipeline()
        } else {
            _previewBitmap.value = null
            if (!needsCamera()) stopCameraPipeline()
        }
    }

    fun setLowPower(value: Boolean) {
        prefs.putBoolean("low_power", value)
        _lowPower.value = value
        lastTargetFps = if (value) 3 else 5
        if (needsCamera()) restartCameraPipeline()
    }

    fun selectCamera(deviceId: String) {
        ensureCamera()
        (camera as? JvmCameraFrameSource)?.selectDevice(deviceId, lastTargetFps)
            ?: camera?.selectDevice(deviceId)
        prefs.put("camera_id", deviceId)
    }

    fun refreshCameras() {
        ensureCamera()
        camera?.refreshDevices()
        val err = camera?.lastError?.value
        if (err != null) {
            session.onCameraError(err)
        } else {
            session.clearIssue()
        }
    }

    fun retryCamera() {
        session.clearIssue()
        stopCameraPipeline()
        ensureCamera()
        camera?.refreshDevices()
        val err = camera?.lastError?.value
        if (err != null) {
            session.onCameraError(err)
            return
        }
        if (needsCamera()) {
            startCameraPipeline()
        }
    }

    fun ensureModelReady(): Boolean {
        ensurePose()
        if (!modelBytesLoaded) {
            session.setModelReady(false)
            return false
        }
        session.setModelReady(true)
        return true
    }

    fun needsCamera(): Boolean {
        if (_showPreview.value || _calibrationUiActive.value) return true
        val ui = session.uiState.value
        return ui.phase != DesktopSessionPhase.IDLE ||
            ui.calibrationPhase == CalibrationPhase.CAPTURE_ERECT ||
            ui.calibrationPhase == CalibrationPhase.CAPTURE_SLUMP
    }

    fun startCameraPipeline() {
        ensurePose()
        if (pipelineJob?.isActive == true) return
        ensureCamera()
        val cam = camera ?: return
        val estimator = poseEstimator
        val wantPose = estimator != null && estimator !is MissingModelPoseEstimator
        if (!wantPose && !_showPreview.value) {
            session.setModelReady(false)
            return
        }
        if (!wantPose) {
            session.setModelReady(false)
        } else {
            session.setModelReady(true)
        }
        lastTargetFps = if (_lowPower.value) 3 else 5
        cam.start(targetFps = lastTargetFps)
        pipelineJob = scope.launch {
            launch {
                cam.lastError.collectLatest { err ->
                    if (err != null) {
                        session.onCameraError(err)
                    } else {
                        session.onCameraRecovered()
                    }
                }
            }
            cam.frames.collect { frame ->
                if (_showPreview.value || _calibrationUiActive.value) {
                    val maxW = if (_calibrationUiActive.value) 1280 else 480
                    _previewBitmap.value = withContext(Dispatchers.Default) {
                        FramePreview.toImageBitmap(frame, maxWidth = maxW)
                    }
                }
                if (wantPose) {
                    val pose = withContext(Dispatchers.Default) {
                        estimator!!.estimate(frame)
                    }
                    // Wall clock — frame timestamps must not stall calibration.
                    session.onPose(pose, System.currentTimeMillis())
                }
            }
        }
    }

    fun stopCameraPipeline() {
        pipelineJob?.cancel()
        pipelineJob = null
        camera?.stop()
        if (!_showPreview.value) {
            _previewBitmap.value = null
        }
    }

    fun restartCameraPipeline() {
        stopCameraPipeline()
        if (needsCamera()) startCameraPipeline()
    }

    fun shutdown() {
        poseEstimator?.close()
        poseEstimator = null
        (camera as? JvmCameraFrameSource)?.dispose()
        camera = null
    }

    // The warm-up runs in the background while the UI can still trigger these, so both entry
    // points share a lock to avoid creating two cameras or two ONNX sessions.
    private fun ensureCamera() {
        synchronized(visionLock) {
            if (!prefs.getBoolean("camera_consent_accepted", false)) return
            if (camera == null) {
                camera = VisionPlatform.createCameraFrameSource()
                val saved = prefs.get("camera_id", null)
                if (saved != null) {
                    (camera as? JvmCameraFrameSource)?.selectDevice(saved, lastTargetFps)
                        ?: camera?.selectDevice(saved)
                }
                // Surface empty-device / permission state immediately on Mac.
                camera?.refreshDevices()
                camera?.lastError?.value?.let { session.onCameraError(it) }
            }
        }
    }

    private fun ensurePose() = synchronized(visionLock) {
        if (poseEstimator == null) {
            val bytes = loadModelBytes()
            modelBytesLoaded = bytes != null
            poseEstimator = VisionPlatform.createPoseEstimator(bytes)
            session.setModelReady(modelBytesLoaded)
        }
    }

    private fun loadModelBytes(): ByteArray? {
        val resource = javaClass.getResourceAsStream("/models/movenet_lightning.onnx")
        if (resource != null) return resource.use { it.readBytes() }
        val file = java.io.File("desktopApp/src/main/resources/models/movenet_lightning.onnx")
        if (file.isFile) return file.readBytes()
        val local = java.io.File(System.getProperty("user.home"), ".keepstraight/movenet_lightning.onnx")
        if (local.isFile) return local.readBytes()
        return null
    }
}
