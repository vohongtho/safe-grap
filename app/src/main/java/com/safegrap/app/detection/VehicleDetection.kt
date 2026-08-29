package com.safegrap.app.detection

import android.graphics.RectF

data class VehicleDetection(
    val box: RectF,
    val label: String,
    val confidence: Float,
    val frameWidth: Int,
    val frameHeight: Int
) {
    val normalizedWidth: Float get() = box.width() / frameWidth
}
