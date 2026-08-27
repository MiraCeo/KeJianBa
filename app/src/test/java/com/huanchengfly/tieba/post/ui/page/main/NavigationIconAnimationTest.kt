package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.core.TargetBasedAnimation
import androidx.compose.animation.core.VectorConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class NavigationIconAnimationTest {
    @Test
    fun swingPassesThroughCenterWithoutStoppingButSlowsAtTurningPoint() {
        for (amplitude in listOf(20f, 15f)) {
            val animation = TargetBasedAnimation(
                messageSwingSpec(amplitude, 0f), Float.VectorConverter, 0f, 0f,
            )
            fun angle(ms: Int) = animation.getValueFromNanos(ms * 1_000_000L)
            val midpoint = MainNavigationFeedbackDurationMillis / 2
            val before = angle(midpoint) - angle(midpoint - 1)
            val after = angle(midpoint + 1) - angle(midpoint)

            assertTrue("Crossing the center must not restart easing", before > amplitude * 0.01f)
            assertEquals("Speed should be continuous across the center", before, after, 0.01f)
            val turningPoint = MainNavigationFeedbackDurationMillis / 4
            assertTrue(abs(angle(turningPoint + 1) - angle(turningPoint)) < before / 10)
        }
    }

    @Test
    fun interruptedSwingStartsAtCurrentAngleAndFinishesAtRest() {
        for (initialRotation in listOf(-19f, 12f)) {
            val animation = TargetBasedAnimation(
                messageSwingSpec(15f, initialRotation), Float.VectorConverter, initialRotation, 0f,
            )
            assertEquals(initialRotation, animation.getValueFromNanos(0L), 0.001f)
            assertTrue(abs(animation.getValueFromNanos(1_000_000L) - initialRotation) < 0.02f)
            assertEquals(0f, animation.getValueFromNanos(animation.durationNanos), 0.001f)
        }
    }

    @Test
    fun planetTiltsInOppositeDirectionsWithoutPausingAtCenter() {
        for (selected in listOf(true, false)) {
            val animation = TargetBasedAnimation(
                planetSwingSpec(0f, selected), Float.VectorConverter, 0f, 0f,
            )
            fun angle(ms: Int) = animation.getValueFromNanos(ms * 1_000_000L)
            assertEquals(if (selected) 15f else 10f, angle(100), 0.001f)
            assertEquals(-5f, angle(270), 0.001f)
            val crossing = if (selected) 213 else 203
            assertTrue(angle(crossing - 1) > 0f)
            assertTrue(angle(crossing + 2) < 0f)
            val before = angle(crossing) - angle(crossing - 1)
            val after = angle(crossing + 1) - angle(crossing)
            assertTrue("The smaller opposite tilt must not stop at zero", before < -0.1f)
            assertEquals(before, after, 0.01f)
            assertEquals(0f, angle(MainNavigationFeedbackDurationMillis), 0.001f)
        }
    }

    @Test
    fun interruptedPlanetTransitionPreservesCurrentAngle() {
        for (selected in listOf(true, false)) {
            val animation = TargetBasedAnimation(
                planetSwingSpec(-3f, selected), Float.VectorConverter, -3f, 0f,
            )
            assertEquals(-3f, animation.getValueFromNanos(0), 0.001f)
            assertTrue(abs(animation.getValueFromNanos(1_000_000L) + 3f) < 0.01f)
            assertEquals(0f, animation.getValueFromNanos(animation.durationNanos), 0.001f)
        }
    }
}
