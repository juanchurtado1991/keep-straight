package com.keepstraight.shared.vision

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * MoveNet Lightning single-pose (17 COCO keypoints) via ONNX Runtime.
 *
 * Xenova / TF MoveNet Lightning ONNX typically expects:
 * - input: int32 NHWC [1,192,192,3] with RGB 0…255
 * - output: float32 [1,1,17,3] as (y, x, score) normalized 0…1
 */
class OnnxMoveNetPoseEstimator(
    modelBytes: ByteArray,
) : PoseEstimator {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputTensorName: String
    private val modelInputSize: Int
    private val isNchw: Boolean
    private val inputIsInt32: Boolean

    init {
        val opts = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(2)
            setInterOpNumThreads(1)
        }
        session = env.createSession(modelBytes, opts)
        inputTensorName = session.inputNames.first()
        val info = session.inputInfo[inputTensorName]!!.info as TensorInfo
        val shape = info.shape
        inputIsInt32 = info.type == OnnxJavaType.INT32
        isNchw = shape.size == 4 && shape[1].toInt() == 3
        modelInputSize = when {
            shape.size == 4 && shape[1].toInt() == 3 ->
                resolveSize(shape[2], shape[3])
            shape.size == 4 ->
                resolveSize(shape[1], shape[2])
            else -> 192
        }
    }

    override fun estimate(frame: CameraFrame): BodyPose? {
        val size = modelInputSize
        return try {
            if (inputIsInt32) {
                val data = preprocessInt32(frame, size)
                val shape = if (isNchw) {
                    longArrayOf(1, 3, size.toLong(), size.toLong())
                } else {
                    longArrayOf(1, size.toLong(), size.toLong(), 3)
                }
                OnnxTensor.createTensor(env, IntBuffer.wrap(data), shape).use { tensor ->
                    runSession(tensor, frame.timestampMs)
                }
            } else {
                val data = preprocessFloat(frame, size)
                val shape = if (isNchw) {
                    longArrayOf(1, 3, size.toLong(), size.toLong())
                } else {
                    longArrayOf(1, size.toLong(), size.toLong(), 3)
                }
                OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape).use { tensor ->
                    runSession(tensor, frame.timestampMs)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun close() {
        try {
            session.close()
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun runSession(tensor: OnnxTensor, timestampMs: Long): BodyPose? {
        val inputs: Map<String, OnnxTensor> = mapOf(inputTensorName to tensor)
        session.run(inputs).use { result ->
            // Prefer named output; fall back to first.
            val value = try {
                result.get(0).value
            } catch (_: Exception) {
                result[0].value
            }
            return parseOutput(value, timestampMs)
        }
    }

    private fun preprocessInt32(frame: CameraFrame, size: Int): IntArray {
        val out = IntArray(size * size * 3)
        val fw = frame.width
        val fh = frame.height
        if (fw <= 0 || fh <= 0) return out
        for (y in 0 until size) {
            for (x in 0 until size) {
                val sx = (x * fw) / size
                val sy = (y * fh) / size
                val src = (sy * fw + sx) * 3
                val r = frame.rgb[src].toInt() and 0xFF
                val g = frame.rgb[src + 1].toInt() and 0xFF
                val b = frame.rgb[src + 2].toInt() and 0xFF
                if (isNchw) {
                    out[0 * size * size + y * size + x] = r
                    out[1 * size * size + y * size + x] = g
                    out[2 * size * size + y * size + x] = b
                } else {
                    val dst = (y * size + x) * 3
                    out[dst] = r
                    out[dst + 1] = g
                    out[dst + 2] = b
                }
            }
        }
        return out
    }

    private fun preprocessFloat(frame: CameraFrame, size: Int): FloatArray {
        val out = FloatArray(size * size * 3)
        val fw = frame.width
        val fh = frame.height
        if (fw <= 0 || fh <= 0) return out
        for (y in 0 until size) {
            for (x in 0 until size) {
                val sx = (x * fw) / size
                val sy = (y * fh) / size
                val src = (sy * fw + sx) * 3
                val r = (frame.rgb[src].toInt() and 0xFF) / 255f
                val g = (frame.rgb[src + 1].toInt() and 0xFF) / 255f
                val b = (frame.rgb[src + 2].toInt() and 0xFF) / 255f
                if (isNchw) {
                    out[0 * size * size + y * size + x] = r
                    out[1 * size * size + y * size + x] = g
                    out[2 * size * size + y * size + x] = b
                } else {
                    val dst = (y * size + x) * 3
                    out[dst] = r
                    out[dst + 1] = g
                    out[dst + 2] = b
                }
            }
        }
        return out
    }

    private fun parseOutput(value: Any?, timestampMs: Long): BodyPose? {
        val flat = flattenScores(value) ?: return null
        if (flat.size < 17 * 3) return null
        val map = LinkedHashMap<PoseLandmark, Landmark>()
        MoveNetOrder.forEachIndexed { index, landmark ->
            val base = index * 3
            val y = flat[base]
            val x = flat[base + 1]
            val score = flat[base + 2]
            map[landmark] = Landmark(x = x, y = y, confidence = score)
        }
        return BodyPose(landmarks = map, timestampMs = timestampMs)
    }

    private fun flattenScores(value: Any?): FloatArray? {
        return when (value) {
            is Array<*> -> {
                val list = ArrayList<Float>(17 * 3)
                fun walk(node: Any?) {
                    when (node) {
                        is FloatArray -> node.forEach { list.add(it) }
                        is Array<*> -> node.forEach { walk(it) }
                        is Float -> list.add(node)
                        is Double -> list.add(node.toFloat())
                    }
                }
                walk(value)
                list.toFloatArray()
            }
            is FloatArray -> value
            is OnnxTensor -> {
                val buffer = value.floatBuffer
                val arr = FloatArray(buffer.remaining())
                buffer.get(arr)
                arr
            }
            else -> null
        }
    }

    private fun resolveSize(a: Long, b: Long): Int {
        val av = a.toInt()
        val bv = b.toInt()
        return when {
            av > 1 -> av
            bv > 1 -> bv
            else -> 192
        }
    }

    companion object {
        private val MoveNetOrder = listOf(
            PoseLandmark.NOSE,
            PoseLandmark.LEFT_EYE,
            PoseLandmark.RIGHT_EYE,
            PoseLandmark.LEFT_EAR,
            PoseLandmark.RIGHT_EAR,
            PoseLandmark.LEFT_SHOULDER,
            PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_ELBOW,
            PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_ANKLE,
        )
    }
}

/** Used when the ONNX model file is not packaged yet. */
class MissingModelPoseEstimator : PoseEstimator {
    override fun estimate(frame: CameraFrame): BodyPose? = null
    override fun close() = Unit
}
