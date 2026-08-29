package com.safegrap.app.camera

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class FrameQualityAnalyzer(
    private val policy: FrameQualityPolicy = FrameQualityPolicy()
) {
    private var poorSince = 0L

    fun analyze(bitmap: Bitmap, now: Long = System.currentTimeMillis()): FrameQualityIssue {
        val rgba = Mat()
        val scaled = Mat()
        val gray = Mat()
        val laplacian = Mat()
        val mean = MatOfDouble()
        val deviation = MatOfDouble()
        val laplacianMean = MatOfDouble()
        val laplacianDeviation = MatOfDouble()

        return try {
            Utils.bitmapToMat(bitmap, rgba)
            val targetWidth = minOf(320.0, rgba.width().toDouble())
            val targetHeight = rgba.height() * targetWidth / rgba.width().coerceAtLeast(1)
            Imgproc.resize(rgba, scaled, Size(targetWidth, targetHeight), 0.0, 0.0, Imgproc.INTER_AREA)
            Imgproc.cvtColor(scaled, gray, Imgproc.COLOR_RGBA2GRAY)
            Core.meanStdDev(gray, mean, deviation)
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
            Core.meanStdDev(laplacian, laplacianMean, laplacianDeviation)

            val metrics = FrameQualityMetrics(
                brightness = mean.get(0, 0)[0],
                contrast = deviation.get(0, 0)[0],
                sharpness = laplacianDeviation.get(0, 0)[0]
            )
            val issue = policy.classify(metrics)
            if (issue == FrameQualityIssue.NONE) {
                poorSince = 0L
                FrameQualityIssue.NONE
            } else {
                if (poorSince == 0L) poorSince = now
                if (now - poorSince >= 1_800L) issue else FrameQualityIssue.NONE
            }
        } finally {
            rgba.release(); scaled.release(); gray.release(); laplacian.release()
            mean.release(); deviation.release(); laplacianMean.release(); laplacianDeviation.release()
        }
    }
}
