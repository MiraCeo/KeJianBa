package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme

// Shared light-theme card treatment for Home and Explore.
internal object MainCardStyle {
    val enabled: Boolean
        @Composable @ReadOnlyComposable
        get() = !TiebaLiteTheme.extendedColorScheme.darkTheme

    val background = Color(0xFFF5F5F5)
    val container = Color.White
    val radius = 12.dp
    val horizontalSpacing = 8.dp
    val verticalSpacing = 4.dp
    val shape = RoundedCornerShape(radius)
}
