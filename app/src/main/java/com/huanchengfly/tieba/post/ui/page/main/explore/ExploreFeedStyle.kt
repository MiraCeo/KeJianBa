package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.huanchengfly.tieba.post.ui.page.main.MainCardStyle

// Shared FeedCard defaults stay unchanged; Home uses the same card style tokens.
internal object ExploreFeedStyle {
    val useCards: Boolean
        @Composable @ReadOnlyComposable
        get() = MainCardStyle.enabled

    @Composable
    fun Modifier.feedBackground(): Modifier =
        if (useCards) background(MainCardStyle.background) else this

    @Composable
    fun Modifier.feedCard(): Modifier =
        if (useCards) {
            // Outer spacing only: retain the existing content padding and typography.
            padding(horizontal = MainCardStyle.horizontalSpacing, vertical = MainCardStyle.verticalSpacing)
                .clip(MainCardStyle.shape)
                .background(MainCardStyle.container)
        } else {
            this
        }
}
