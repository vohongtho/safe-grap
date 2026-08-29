package com.safegrap.app.distance

import com.safegrap.app.detection.VehicleDetection
import kotlin.math.max

class DistanceEstimator(private var factor: Float = 1.3f) {
    private var smoothed: Float? = null

    fun estimate(detection: VehicleDetection): Float {
        val vehicleWidth = widthFor(detection.label)
        val raw = factor * (vehicleWidth / 1.8f) / max(detection.normalizedWidth, 0.025f)
        val bounded = raw.coerceIn(2f, 150f)
        smoothed = smoothed?.let { it * 0.72f + bounded * 0.28f } ?: bounded
        return smoothed!!
    }

    fun calibrate(detection: VehicleDetection, actualMetres: Float): Float {
        factor = actualMetres * detection.normalizedWidth * 1.8f / widthFor(detection.label)
        smoothed = actualMetres
        return factor
    }

    fun reset(newFactor: Float = factor) { factor = newFactor; smoothed = null }

    private fun widthFor(label: String) = when (label.lowercase()) {
        "bus", "truck" -> 2.5f
        "motorcycle" -> 0.8f
        else -> 1.8f
    }
}
