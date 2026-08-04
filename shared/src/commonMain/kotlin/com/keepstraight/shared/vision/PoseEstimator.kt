package com.keepstraight.shared.vision

interface PoseEstimator {
    fun estimate(frame: CameraFrame): BodyPose?
    fun close()
}

/**
 * Platform factories for desktop vision. Android stubs throw; only JVM desktop uses them.
 */
expect object VisionPlatform {
    fun createCameraFrameSource(): CameraFrameSource
    fun createPoseEstimator(modelBytes: ByteArray?): PoseEstimator
}
