package com.keepstraight.shared.vision

/**
 * Normalized body landmarks from a pose estimator (0…1 image space, y down).
 * Confidence is per-keypoint in 0…1.
 */
data class Landmark(
    val x: Float,
    val y: Float,
    val confidence: Float,
)

enum class PoseLandmark {
    NOSE,
    LEFT_EYE,
    RIGHT_EYE,
    LEFT_EAR,
    RIGHT_EAR,
    LEFT_SHOULDER,
    RIGHT_SHOULDER,
    LEFT_ELBOW,
    RIGHT_ELBOW,
    LEFT_WRIST,
    RIGHT_WRIST,
    LEFT_HIP,
    RIGHT_HIP,
    LEFT_KNEE,
    RIGHT_KNEE,
    LEFT_ANKLE,
    RIGHT_ANKLE,
}

data class BodyPose(
    val landmarks: Map<PoseLandmark, Landmark>,
    val timestampMs: Long,
) {
    fun get(name: PoseLandmark): Landmark? = landmarks[name]

    fun meanConfidence(vararg names: PoseLandmark): Float {
        val values = names.mapNotNull { landmarks[it]?.confidence }
        if (values.isEmpty()) return 0f
        return values.average().toFloat()
    }
}

data class CameraFrame(
    val width: Int,
    val height: Int,
    /** RGB888 packed bytes, size = width * height * 3 */
    val rgb: ByteArray,
    val timestampMs: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CameraFrame) return false
        return width == other.width &&
            height == other.height &&
            timestampMs == other.timestampMs &&
            rgb.contentEquals(other.rgb)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + rgb.contentHashCode()
        result = 31 * result + timestampMs.hashCode()
        return result
    }
}

enum class CameraError {
    IN_USE,
    PERMISSION_DENIED,
    NOT_FOUND,
    OPEN_FAILED,
    DISCONNECTED,
}
