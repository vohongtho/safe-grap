package com.safegrap.app.camera

import android.graphics.Bitmap
import kotlin.math.sqrt

class FrameQualityAnalyzer {
    private var poorSince = 0L

    fun isInvalid(bitmap: Bitmap, now: Long = System.currentTimeMillis()): Boolean {
        var sum = 0.0
        var squareSum = 0.0
        var count = 0
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 12).coerceAtLeast(1)
        for (y in stepY / 2 until bitmap.height step stepY) for (x in stepX / 2 until bitmap.width step stepX) {
            val pixel = bitmap.getPixel(x, y)
            val lum = (0.2126 * ((pixel shr 16) and 255) + 0.7152 * ((pixel shr 8) and 255) + 0.0722 * (pixel and 255))
            sum += lum; squareSum += lum * lum; count++
        }
        val mean = sum / count.coerceAtLeast(1)
        val variance = squareSum / count.coerceAtLeast(1) - mean * mean
        val poor = mean < 18 || sqrt(variance.coerceAtLeast(0.0)) < 7
        if (poor && poorSince == 0L) poorSince = now
        if (!poor) poorSince = 0L
        return poorSince > 0 && now - poorSince > 1800
    }
}
