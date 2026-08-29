package com.safegrap.app.distance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeadVehicleMovementDetectorTest {
    @Test fun `announces when stopped lead vehicle moves`() {
        val detector = LeadVehicleMovementDetector()
        assertFalse(detector.update(6f, 1_000))
        assertFalse(detector.update(6.1f, 2_000))
        assertTrue(detector.update(7.5f, 2_700))
        assertFalse(detector.update(8.5f, 3_000))
    }

    @Test fun `does not arm for a distant vehicle`() {
        val detector = LeadVehicleMovementDetector()
        assertFalse(detector.update(18f, 1_000))
        assertFalse(detector.update(22f, 3_000))
    }
}
