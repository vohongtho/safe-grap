package com.safegrap.app.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import kotlin.math.abs

class VehicleDetector(context: Context) : AutoCloseable {
    private val detector = ObjectDetector.createFromFileAndOptions(
        context,
        "efficientdet_lite0_320_ptq.tflite",
        ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(8).setScoreThreshold(0.45f).build()
    )
    private val allowed = setOf("car", "truck", "bus", "motorcycle")

    fun detect(bitmap: Bitmap): VehicleDetection? {
        return detector.detect(TensorImage.fromBitmap(bitmap)).mapNotNull { result ->
            val category = result.categories.maxByOrNull { it.score } ?: return@mapNotNull null
            val label = category.label.lowercase()
            if (label !in allowed) return@mapNotNull null
            val box = RectF(result.boundingBox)
            val center = box.centerX() / bitmap.width
            if (center !in 0.24f..0.76f || box.bottom / bitmap.height < 0.32f) return@mapNotNull null
            VehicleDetection(box, label, category.score, bitmap.width, bitmap.height)
        }.maxByOrNull { candidate ->
            val centerPenalty = abs(candidate.box.centerX() / bitmap.width - 0.5f)
            candidate.box.height() * candidate.box.width() * candidate.confidence * (1f - centerPenalty)
        }
    }

    override fun close() = detector.close()
}
