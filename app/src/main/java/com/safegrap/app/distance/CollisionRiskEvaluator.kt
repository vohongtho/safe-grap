package com.safegrap.app.distance

class CollisionRiskEvaluator {
    private var lastDistance: Float? = null
    private var lastTime = 0L
    private var closingSpeed = 0f
    private var riskFrames = 0

    fun update(distance: Float, now: Long = System.currentTimeMillis()): Boolean {
        val previous = lastDistance
        if (previous != null && lastTime > 0) {
            val dt = (now - lastTime).coerceAtLeast(1) / 1000f
            if (dt in 0.05f..2f) {
                val instant = (previous - distance) / dt
                closingSpeed = closingSpeed * 0.7f + instant * 0.3f
            }
        }
        lastDistance = distance
        lastTime = now
        val ttc = if (closingSpeed > 1f) distance / closingSpeed else Float.POSITIVE_INFINITY
        riskFrames = if (distance < 50f && ttc < 3f) riskFrames + 1 else 0
        return riskFrames >= 3
    }

    fun reset() { lastDistance = null; lastTime = 0; closingSpeed = 0f; riskFrames = 0 }
}
