package com.safegrap.app.camera

data class FrameQualityMetrics(
    val brightness: Double,
    val contrast: Double,
    val sharpness: Double
)

enum class FrameQualityIssue {
    NONE,
    TOO_DARK,
    LOW_DETAIL,
    BLURRY
}

class FrameQualityPolicy {
    fun classify(metrics: FrameQualityMetrics): FrameQualityIssue = when {
        metrics.brightness < 18.0 -> FrameQualityIssue.TOO_DARK
        metrics.contrast < 7.0 -> FrameQualityIssue.LOW_DETAIL
        metrics.sharpness < 12.0 -> FrameQualityIssue.BLURRY
        else -> FrameQualityIssue.NONE
    }
}
