package com.safegrap.app.distance

import kotlin.math.abs

class LeadVehicleMovementDetector {
    private var baseDistance: Float? = null
    private var stableSince = 0L
    private var fired = false

    fun update(distance: Float, now: Long = System.currentTimeMillis()): Boolean {
        if (distance > 10f) { reset(); return false }
        val base = baseDistance
        if (base == null) { baseDistance = distance; stableSince = now; return false }
        if (!fired && now - stableSince > 1500 && distance - base > 1.2f) {
            fired = true
            return true
        }
        if (abs(distance - base) < 0.5f) return false
        if (!fired && distance < base + 0.5f) { baseDistance = distance; stableSince = now }
        return false
    }

    fun reset() { baseDistance = null; stableSince = 0; fired = false }
}
