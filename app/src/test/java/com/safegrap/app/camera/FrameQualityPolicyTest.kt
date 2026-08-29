package com.safegrap.app.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameQualityPolicyTest {
    private val policy = FrameQualityPolicy()

    @Test fun `classifies dark image`() {
        assertEquals(FrameQualityIssue.TOO_DARK, policy.classify(FrameQualityMetrics(12.0, 30.0, 40.0)))
    }

    @Test fun `classifies covered lens`() {
        assertEquals(FrameQualityIssue.LOW_DETAIL, policy.classify(FrameQualityMetrics(90.0, 3.0, 20.0)))
    }

    @Test fun `classifies blurry image`() {
        assertEquals(FrameQualityIssue.BLURRY, policy.classify(FrameQualityMetrics(90.0, 30.0, 5.0)))
    }

    @Test fun `accepts usable image`() {
        assertEquals(FrameQualityIssue.NONE, policy.classify(FrameQualityMetrics(90.0, 30.0, 25.0)))
    }
}
