package com.keepstraight.shared.vision

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

data class CameraDeviceInfo(
    val id: String,
    val name: String,
)

interface CameraFrameSource {
    val devices: StateFlow<List<CameraDeviceInfo>>
    val selectedDeviceId: StateFlow<String?>
    val lastError: StateFlow<CameraError?>
    val frames: Flow<CameraFrame>

    fun refreshDevices()
    fun selectDevice(deviceId: String?)
    fun start(targetFps: Int = 5)
    fun stop()
}
