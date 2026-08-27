package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme

// Keep this first light-theme pass local to Explore; shared FeedCard defaults stay unchanged.
internal object ExploreFeedStyle {
    val useCards: Boolean
        @Composable @ReadOnlyComposable
        get() = !TiebaLiteTheme.extendedColorScheme.darkTheme

    private val cardShape = RoundedCornerShape(12.dp)

    @Composable
    fun Modifier.feedBackground(): Modifier =
        if (useCards) background(Color(0xFFF5F5F5)) else this

    @Composable
    fun Modifier.feedCard(): Modifier =
        if (useCards) {
            // Outer spacing only: retain the existing content padding and typography.
            padding(horizontal = 8.dp, vertical = 4.dp)
                .clip(cardShape)
                .background(Color.White)
        } else {
            this
        }
}
