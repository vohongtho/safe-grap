package com.safegrap.app.distance

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionRiskEvaluatorTest {
    @Test fun `requires sustained collision risk`() {
        val evaluator = CollisionRiskEvaluator()
        assertFalse(evaluator.update(30f, 1_000))
        assertFalse(evaluator.update(25f, 2_000))
        assertFalse(evaluator.update(20f, 3_000))
        assertFalse(evaluator.update(15f, 4_000))
        assertFalse(evaluator.update(10f, 5_000))
        assertFalse(evaluator.update(6f, 6_000))
        assertTrue(evaluator.update(3f, 7_000))
    }

    @Test fun `stable distance is not collision risk`() {
        val evaluator = CollisionRiskEvaluator()
        repeat(8) { assertFalse(evaluator.update(20f, 1_000L + it * 500L)) }
    }
}
