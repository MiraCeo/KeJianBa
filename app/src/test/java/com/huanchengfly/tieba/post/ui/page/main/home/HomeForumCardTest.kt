package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow

class HomeForumCardTest {
    @Test
    fun avatarContourIsClosedAndSmoothAtEveryJoin() {
        val curves = ForumAvatarShape.curves
        curves.forEachIndexed { index, curve ->
            val next = curves[(index + 1) % curves.size]
            assertEquals(curve.end, next.start)
            val incoming = curve.end - curve.control2
            val outgoing = next.control1 - next.start
            assertEquals(incoming.x, outgoing.x, 0.000001f)
            assertEquals(incoming.y, outgoing.y, 0.000001f)
        }
        assertEquals(Offset(1f, 0.5f), curves[0].start)
        assertEquals(Offset(0.5f, 1f), curves[curves.size / 4].start)
        assertEquals(Offset(0f, 0.5f), curves[curves.size / 2].start)
        assertEquals(Offset(0.5f, 0f), curves[curves.size * 3 / 4].start)
    }

    @Test
    fun avatarCurvesKeepBowedSuperellipseInsideBounds() {
        ForumAvatarShape.curves.forEach { curve ->
            for (step in 0..20) {
                val t = step / 20f
                val u = 1f - t
                val point = curve.start * (u * u * u) + curve.control1 * (3 * u * u * t) +
                    curve.control2 * (3 * u * t * t) + curve.end * (t * t * t)
                assertTrue(point.x in -0.000001f..1.000001f && point.y in -0.000001f..1.000001f)
                val equation = (2 * point.x - 1).pow(4) + (2 * point.y - 1).pow(4)
                assertTrue("Contour must approximate x^4 + y^4 = 1", abs(equation - 1f) < 0.003f)
            }
        }
        // The upper side gently bows: it must not stay flat like a rounded rectangle.
        val upperSide = ForumAvatarShape.curves[ForumAvatarShape.curves.size * 13 / 16].start
        assertTrue(upperSide.y > 0f && upperSide.y < 0.02f)
    }

    @Test
    fun switchingModesKeepsTwoColumnsAfterCardInsetsOnNarrowScreens() {
        with(Density(1f)) {
            // A 360dp page becomes 344dp inside the card. Adaptive(180.dp)
            // used to collapse this to one column while still hiding avatars.
            val innerWidth = 360 - 8 * 2
            assertEquals(
                listOf(172, 172),
                with(homeForumGridCells(singleColumn = false)) {
                    calculateCrossAxisCellSizes(innerWidth, 0)
                },
            )
            assertEquals(
                listOf(344),
                with(homeForumGridCells(singleColumn = true)) {
                    calculateCrossAxisCellSizes(innerWidth, 0)
                },
            )
        }
    }

    private fun bounds(header: Int?, footer: Int?, beforePadding: Int = 80): Rect =
        followedForumsCardBounds(
            viewport = Size(400f, 800f),
            headerOffset = header,
            footerBottom = footer,
            beforePadding = beforePadding,
            leftInset = 8f,
            rightInset = 8f,
            spacing = 4f,
            radius = 12f,
        )

    @Test
    fun shortSectionIncludesContentPaddingAndKeepsBothOuterGaps() {
        assertEquals(Rect(8f, 204f, 392f, 436f), bounds(header = 120, footer = 360))
    }

    @Test
    fun scrolledHeaderExtendsRoundCornersOutsideViewport() {
        assertEquals(Rect(8f, -12f, 392f, 576f), bounds(header = null, footer = 500))
    }

    @Test
    fun middleOfLongSectionCoversTheWholeViewport() {
        assertEquals(Rect(8f, -12f, 392f, 812f), bounds(header = null, footer = null))
    }

    @Test
    fun loadingSkeletonWithoutToolbarPaddingUsesSameEdges() {
        assertEquals(Rect(8f, 4f, 392f, 812f), bounds(header = 0, footer = null, beforePadding = 0))
    }
}
