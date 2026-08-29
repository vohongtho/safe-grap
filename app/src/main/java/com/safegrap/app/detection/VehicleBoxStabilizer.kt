package com.safegrap.app.detection

import android.graphics.RectF
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat

class VehicleBoxStabilizer(
    private val previousWeight: Double = 0.68
) : AutoCloseable {
    private val previous = Mat()
    private var lastSeenAt = 0L
    private var lastLabel: String? = null

    fun stabilize(detection: VehicleDetection, now: Long = System.currentTimeMillis()): VehicleDetection {
        val measurement = Mat(1, 4, CvType.CV_32F)
        measurement.put(0, 0, floatArrayOf(
            detection.box.left,
            detection.box.top,
            detection.box.right,
            detection.box.bottom
        ))

        val shouldReset = previous.empty() || now - lastSeenAt > 800L || lastLabel != detection.label
        if (shouldReset) {
            measurement.copyTo(previous)
        } else {
            Core.addWeighted(previous, previousWeight, measurement, 1.0 - previousWeight, 0.0, previous)
        }
        measurement.release()
        lastSeenAt = now
        lastLabel = detection.label

        val values = FloatArray(4)
        previous.get(0, 0, values)
        val box = RectF(
            values[0].coerceIn(0f, detection.frameWidth.toFloat()),
            values[1].coerceIn(0f, detection.frameHeight.toFloat()),
            values[2].coerceIn(0f, detection.frameWidth.toFloat()),
            values[3].coerceIn(0f, detection.frameHeight.toFloat())
        )
        return detection.copy(box = box)
    }

    fun reset() {
        previous.release()
        lastSeenAt = 0L
        lastLabel = null
    }

    override fun close() = reset()
}
