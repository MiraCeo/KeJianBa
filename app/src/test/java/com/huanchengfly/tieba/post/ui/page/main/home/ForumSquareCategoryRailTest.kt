package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ForumSquareCategoryRailTest {
    private fun corners(index: Int, selected: Int): List<Float> {
        val shape = forumSquareCategoryShape(index, selected)
        return listOf(shape.topStart, shape.topEnd, shape.bottomEnd, shape.bottomStart)
            .map { it.toPx(Size(68f, 48f), Density(1f)) }
    }

    @Test fun compactGeometryPreservesMinimumTouchHeight() {
        assertEquals(68.dp, ForumSquareRailWidth)
        assertEquals(48.dp, ForumSquareCategoryMinHeight)
    }

    @Test fun selectedRowIsAFullWidthRectangle() {
        assertEquals(listOf(0f, 0f, 0f, 0f), corners(3, 3))
    }

    @Test fun onlyAdjacentGreyRowsHaveEndCorners() {
        assertEquals(listOf(0f, 0f, 10f, 0f), corners(2, 3))
        assertEquals(listOf(0f, 10f, 0f, 0f), corners(4, 3))
        assertEquals(listOf(0f, 0f, 0f, 0f), corners(1, 3))
    }

    @Test fun firstSelectionAndMissingSelectionDoNotRoundUnrelatedRows() {
        assertEquals(listOf(0f, 0f, 0f, 0f), corners(0, 0))
        assertEquals(listOf(0f, 10f, 0f, 0f), corners(1, 0))
        assertEquals(listOf(0f, 0f, 0f, 0f), corners(0, -1))
    }
}
