package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

private val MainBarDividerColor = Color(0xFFDDDDDD)
internal fun Modifier.mainTopBarDividers(): Modifier = mainBarDividers(topY = null, bottom = true)

internal fun Modifier.mainBottomBarDivider(): Modifier = mainBarDividers(topY = 0f)

private fun Modifier.mainBarDividers(topY: Float?, bottom: Boolean = false): Modifier = drawWithContent {
    drawContent()
    // One physical pixel, drawn inside the existing bounds: no added height or half-pixel stroke.
    if (size.width > 0f && size.height >= 1f) {
        if (topY != null && topY >= 0f && topY <= size.height - 1f) {
            drawRect(MainBarDividerColor, Offset(0f, topY), Size(size.width, 1f))
        }
        if (bottom && topY != size.height - 1f) {
            drawRect(MainBarDividerColor, Offset(0f, size.height - 1f), Size(size.width, 1f))
        }
    }
}
