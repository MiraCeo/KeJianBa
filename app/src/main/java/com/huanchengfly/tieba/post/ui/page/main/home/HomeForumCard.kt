package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import com.huanchengfly.tieba.post.ui.page.main.MainCardStyle

internal const val FollowedForumsHeaderKey = "home-followed-forums-header"
internal const val FollowedForumsFooterKey = "home-followed-forums-footer"

// Draw one continuous card behind the lazy cells. This also covers the unused cells
// of a partial final grid row without nesting a scrolling list or eagerly composing it.
internal fun Modifier.followedForumsCard(
    state: LazyGridState,
    firstSectionIndex: Int,
    enabled: Boolean,
    contentPadding: PaddingValues = PaddingValues(horizontal = MainCardStyle.horizontalSpacing),
): Modifier = if (!enabled) this else drawBehind {
    val visibleItems = state.layoutInfo.visibleItemsInfo
    if (visibleItems.none { it.index >= firstSectionIndex }) return@drawBehind

    val radius = MainCardStyle.radius.toPx()
    val spacing = MainCardStyle.verticalSpacing.toPx()
    val header = visibleItems.firstOrNull { it.key == FollowedForumsHeaderKey }
    val footer = visibleItems.firstOrNull { it.key == FollowedForumsFooterKey }
    // Lazy item offsets exclude beforeContentPadding; canvas coordinates include it.
    val beforePadding = state.layoutInfo.beforeContentPadding
    val bounds = followedForumsCardBounds(
        viewport = size,
        headerOffset = header?.offset?.y,
        footerBottom = footer?.let { it.offset.y + it.size.height },
        beforePadding = beforePadding,
        leftInset = contentPadding.calculateLeftPadding(layoutDirection).toPx(),
        rightInset = contentPadding.calculateRightPadding(layoutDirection).toPx(),
        spacing = spacing,
        radius = radius,
    )
    if (!bounds.isEmpty) {
        clipRect {
            drawRoundRect(
                color = MainCardStyle.container,
                topLeft = bounds.topLeft,
                size = bounds.size,
                cornerRadius = CornerRadius(radius),
            )
        }
    }
}

internal fun followedForumsCardBounds(
    viewport: Size,
    headerOffset: Int?,
    footerBottom: Int?,
    beforePadding: Int,
    leftInset: Float,
    rightInset: Float,
    spacing: Float,
    radius: Float,
): Rect = Rect(
    left = leftInset,
    top = headerOffset?.let { it + beforePadding + spacing } ?: -radius,
    right = viewport.width - rightInset,
    bottom = footerBottom?.let { it + beforePadding - spacing } ?: (viewport.height + radius),
)
