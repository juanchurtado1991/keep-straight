package com.keepstraight.shared.vision

actual object VisionPlatform {
    actual fun createCameraFrameSource(): CameraFrameSource = JvmCameraFrameSource()

    actual fun createPoseEstimator(modelBytes: ByteArray?): PoseEstimator =
        if (modelBytes != null && modelBytes.isNotEmpty()) {
            OnnxMoveNetPoseEstimator(modelBytes)
        } else {
            MissingModelPoseEstimator()
        }
}
