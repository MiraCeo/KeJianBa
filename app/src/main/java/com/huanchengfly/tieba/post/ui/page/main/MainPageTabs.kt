package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.widgets.compose.FancyAnimatedIndicatorWithModifier
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Shared toolbar tabs for Home and Explore. */
@Composable
internal fun MainPageTabs(pagerState: PagerState, titles: List<Int>, compactLabels: Boolean = false) {
    val coroutineScope = rememberCoroutineScope()
    SecondaryTabRow(
        modifier = Modifier.widthIn(max = 280.dp),
        selectedTabIndex = pagerState.currentPage,
        indicator = { FancyAnimatedIndicatorWithModifier(index = pagerState.currentPage) },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
    ) {
        titles.forEachIndexed { index, title ->
            val selected = pagerState.currentPage == index
            val label: @Composable () -> Unit = {
                Text(
                    text = stringResource(title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val selectTab: () -> Unit = {
                if (!selected) {
                    coroutineScope.launch {
                        if (abs(pagerState.currentPage - index) > 1) pagerState.scrollToPage(index)
                        else pagerState.animateScrollToPage(index)
                    }
                }
            }
            if (compactLabels) {
                // Three four-character labels need less inset than Material's text-tab padding.
                Tab(
                    selected = selected,
                    onClick = selectTab,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    Box(
                        Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) { label() }
                }
            } else {
                Tab(
                    text = label,
                    selected = selected,
                    onClick = selectTab,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
