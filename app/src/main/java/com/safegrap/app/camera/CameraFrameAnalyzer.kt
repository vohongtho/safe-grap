package com.safegrap.app.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.safegrap.app.detection.VehicleDetection
import com.safegrap.app.detection.VehicleBoxStabilizer
import com.safegrap.app.detection.VehicleDetector
import java.util.concurrent.atomic.AtomicBoolean

data class FrameResult(val detection: VehicleDetection?, val qualityIssue: FrameQualityIssue) {
    val invalidCamera: Boolean get() = qualityIssue != FrameQualityIssue.NONE
}

class CameraFrameAnalyzer(
    private val detector: VehicleDetector,
    private val qualityAnalyzer: FrameQualityAnalyzer,
    private val onResult: (FrameResult) -> Unit
) : ImageAnalysis.Analyzer {
    private val busy = AtomicBoolean(false)
    private val stabilizer = VehicleBoxStabilizer()

    override fun analyze(image: ImageProxy) {
        if (!busy.compareAndSet(false, true)) { image.close(); return }
        try {
            val bitmap = image.toRotatedBitmap()
            val detection = detector.detect(bitmap)?.let(stabilizer::stabilize)
            onResult(FrameResult(detection, qualityAnalyzer.analyze(bitmap)))
            bitmap.recycle()
        } finally {
            busy.set(false)
            image.close()
        }
    }

    private fun ImageProxy.toRotatedBitmap(): Bitmap {
        val plane = planes[0]
        plane.buffer.rewind()
        val rowPixels = plane.rowStride / plane.pixelStride
        val padded = Bitmap.createBitmap(rowPixels, height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(plane.buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
        if (padded !== cropped) padded.recycle()
        val rotation = imageInfo.rotationDegrees
        if (rotation == 0) return cropped
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
        if (rotated !== cropped) cropped.recycle()
        return rotated
    }
}
