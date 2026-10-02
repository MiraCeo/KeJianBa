package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme

/**
 * Shared card treatment for the thread detail page and its sub-post page, so the two
 * cannot drift apart: inset rounded cards on a dimmed page background, with hairline
 * dividers between consecutive floors.
 */
internal object PostCardStyle {
    val shape = RoundedCornerShape(12.dp)
    val headerShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    val footerShape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
    val horizontalSpacing = 8.dp
    val verticalSpacing = 4.dp
    val dividerInset = 16.dp

    /** Caps the bottom of a floor column so the last divider is not left hanging. */
    val footerHeight = 12.dp

    private val LightBackground = Color(0xFFF5F5F5)

    /** Light mode needs a dimmed page colour, otherwise white cards do not read as cards. */
    val pageBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (TiebaLiteTheme.extendedColorScheme.darkTheme) {
            MaterialTheme.colorScheme.surface
        } else {
            LightBackground
        }
}

/**
 * One floor of a continuous card column: a full-bleed surface with an optional rounded
 * top and a hairline divider along its bottom edge.
 */
internal fun Modifier.postListSurface(
    color: Color,
    dividerColor: Color,
    roundedTop: Boolean = false,
    drawDivider: Boolean = true,
): Modifier = this
    .padding(horizontal = PostCardStyle.horizontalSpacing)
    .clip(if (roundedTop) PostCardStyle.headerShape else RoundedCornerShape(0.dp))
    .background(color)
    .drawBehind {
        if (drawDivider) {
            val inset = PostCardStyle.dividerInset.toPx()
            drawLine(
                color = dividerColor,
                start = Offset(inset, size.height - 0.5f),
                end = Offset(size.width - inset, size.height - 0.5f),
                strokeWidth = 1f,
            )
        }
    }
