package com.keepstraight.shared.vision

actual object VisionPlatform {
    actual fun createCameraFrameSource(): CameraFrameSource {
        error("CameraFrameSource is only available on the desktop JVM target")
    }

    actual fun createPoseEstimator(modelBytes: ByteArray?): PoseEstimator {
        error("PoseEstimator is only available on the desktop JVM target")
    }
}
