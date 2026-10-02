package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val DefaultBackToTopVisibleDurationMillis = 3_000L

/** Shared visibility controller for list back-to-top actions. */
@Composable
fun LaunchedBackToTopFabStateEffect(
    listState: LazyListState,
    onVisibilityChanged: (visible: Boolean) -> Unit,
    isRefreshing: Boolean = false,
    isError: Boolean = false,
    revealDistance: Dp = 72.dp,
    reverseHideDistance: Dp = 24.dp,
    visibleDurationMillis: Long = DefaultBackToTopVisibleDurationMillis,
    /**
     * How many items must sit above the viewport before the action is offered.
     *
     * Defaults to the number of visible items, which suits long feeds but can never be
     * reached on a list shorter than about twice the viewport: the largest possible
     * first-visible index is `totalItems - visibleItems`. Short lists should pass a
     * small constant instead.
     */
    minIndexFromTop: Int? = null,
) {
    val latestOnVisibilityChanged by rememberUpdatedState(onVisibilityChanged)
    val revealDistancePx = with(LocalDensity.current) { revealDistance.toPx() }
    val reverseHideDistancePx = with(LocalDensity.current) { reverseHideDistance.toPx() }

    LaunchedEffect(
        listState,
        isRefreshing,
        isError,
        revealDistancePx,
        reverseHideDistancePx,
        visibleDurationMillis,
        minIndexFromTop,
    ) {
        latestOnVisibilityChanged(false)
        if (isRefreshing || isError) return@LaunchedEffect

        var previous: BackToTopListPosition? = null
        var revealTravel = 0f
        var reverseTravel = 0f
        var isVisible = false
        var timeoutJob: Job? = null

        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val firstItemSize = layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == listState.firstVisibleItemIndex }
                ?.size ?: 0
            BackToTopListPosition(
                index = listState.firstVisibleItemIndex,
                offset = listState.firstVisibleItemScrollOffset,
                firstItemSize = firstItemSize,
                visibleItemCount = layoutInfo.visibleItemsInfo.size,
            )
        }.collect { current ->
            val old = previous
            previous = current
            if (old == null) return@collect

            val farFromTopThreshold =
                (minIndexFromTop ?: current.visibleItemCount).coerceAtLeast(1)
            val isFarFromTop = current.index >= farFromTopThreshold
            if (!isFarFromTop) {
                revealTravel = 0f
                reverseTravel = 0f
                timeoutJob?.cancel()
                isVisible = false
                latestOnVisibilityChanged(false)
                return@collect
            }

            // Increasing list position means browsing toward later content.
            val laterContentDelta = when {
                current.index == old.index ->
                    (current.offset - old.offset).coerceAtLeast(0).toFloat()
                current.index == old.index + 1 ->
                    (old.firstItemSize - old.offset + current.offset).coerceAtLeast(0).toFloat()
                current.index > old.index -> reverseHideDistancePx
                else -> 0f
            }
            if (laterContentDelta > 0f) {
                revealTravel = 0f
                if (isVisible) {
                    reverseTravel += laterContentDelta
                    if (reverseTravel >= reverseHideDistancePx) {
                        reverseTravel = 0f
                        timeoutJob?.cancel()
                        isVisible = false
                        latestOnVisibilityChanged(false)
                    }
                }
                return@collect
            }

            // Decreasing list position means moving back toward earlier content.
            reverseTravel = 0f
            val earlierContentDelta = when {
                current.index == old.index ->
                    (old.offset - current.offset).coerceAtLeast(0).toFloat()
                current.index == old.index - 1 ->
                    (old.offset + current.firstItemSize - current.offset).coerceAtLeast(0).toFloat()
                current.index < old.index -> revealDistancePx
                else -> 0f
            }
            if (!isVisible) revealTravel += earlierContentDelta
            if (!isVisible && revealTravel >= revealDistancePx) {
                revealTravel = 0f
                isVisible = true
                latestOnVisibilityChanged(true)
                timeoutJob = launch {
                    delay(visibleDurationMillis)
                    isVisible = false
                    revealTravel = 0f
                    reverseTravel = 0f
                    latestOnVisibilityChanged(false)
                }
            }
        }
    }
}

/** Performs a short finishing animation even when the list starts far from the top. */
suspend fun LazyListState.animateScrollToTop() {
    if (firstVisibleItemIndex > 5) scrollToItem(2)
    animateScrollToItem(0)
}

private data class BackToTopListPosition(
    val index: Int,
    val offset: Int,
    val firstItemSize: Int,
    val visibleItemCount: Int,
)
