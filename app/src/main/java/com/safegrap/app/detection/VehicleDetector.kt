package com.safegrap.app.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.min

class VehicleDetector(context: Context) : AutoCloseable {
    private val interpreter = Interpreter(
        context.assets.openFd(MODEL_FILE).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.map(
                    FileInputStream.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength
                )
            }
        },
        Interpreter.Options().apply { setNumThreads(2) }
    )
    private val input = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3)
        .order(ByteOrder.nativeOrder())
    private val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)

    init {
        interpreter.allocateTensors()
    }

    fun detect(bitmap: Bitmap): VehicleDetection? {
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        input.rewind()
        pixels.forEach { color ->
            input.put((color shr 16 and 0xff).toByte())
            input.put((color shr 8 and 0xff).toByte())
            input.put((color and 0xff).toByte())
        }
        input.rewind()
        if (resized !== bitmap) resized.recycle()

        // This EfficientDet export exposes, in order: count, scores, classes, boxes.
        val count = FloatArray(1)
        val scores = Array(1) { FloatArray(MAX_DETECTIONS) }
        val classes = Array(1) { FloatArray(MAX_DETECTIONS) }
        val boxes = Array(1) { Array(MAX_DETECTIONS) { FloatArray(4) } }
        val outputs = hashMapOf<Int, Any>(
            0 to count,
            1 to scores,
            2 to classes,
            3 to boxes
        )
        interpreter.runForMultipleInputsOutputs(arrayOf<Any>(input), outputs)

        return (0 until min(count[0].toInt(), MAX_DETECTIONS)).mapNotNull { index ->
            val confidence = scores[0][index]
            if (confidence < SCORE_THRESHOLD) return@mapNotNull null
            val classId = classes[0][index].toInt()
            val label = VEHICLE_CLASSES[classId] ?: return@mapNotNull null
            val coordinates = boxes[0][index]
            val box = RectF(
                coordinates[1].coerceIn(0f, 1f) * bitmap.width,
                coordinates[0].coerceIn(0f, 1f) * bitmap.height,
                coordinates[3].coerceIn(0f, 1f) * bitmap.width,
                coordinates[2].coerceIn(0f, 1f) * bitmap.height
            )
            val center = box.centerX() / bitmap.width
            if (center !in 0.24f..0.76f || box.bottom / bitmap.height < 0.32f) return@mapNotNull null
            VehicleDetection(box, label, confidence, bitmap.width, bitmap.height)
        }.maxByOrNull { candidate ->
            val centerPenalty = abs(candidate.box.centerX() / bitmap.width - 0.5f)
            candidate.box.height() * candidate.box.width() * candidate.confidence * (1f - centerPenalty)
        }
    }

    override fun close() = interpreter.close()

    private companion object {
        const val MODEL_FILE = "efficientdet_lite0_320_ptq.tflite"
        const val INPUT_SIZE = 320
        const val MAX_DETECTIONS = 25
        const val SCORE_THRESHOLD = 0.45f

        // Zero-based indices from the COCO label map shipped with this model.
        val VEHICLE_CLASSES = mapOf(
            2 to "car",
            3 to "motorcycle",
            5 to "bus",
            7 to "truck"
        )
    }
}
