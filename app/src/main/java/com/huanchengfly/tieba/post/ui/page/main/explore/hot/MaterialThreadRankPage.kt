package com.huanchengfly.tieba.post.ui.page.main.explore.hot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.repository.HotTopicRepository
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.mainTopBarDividers
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.LaunchedBackToTopFabStateEffect
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.animateScrollToTop
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MaterialThreadRankPage(
    navigator: NavController,
    viewModel: MaterialThreadRankViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isBackToTopVisible by remember { mutableStateOf(false) }
    var initialRankPositionApplied by rememberSaveable { mutableStateOf(false) }
    val dividerThickness = with(LocalDensity.current) { 1f.toDp() }

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()
    ConsumeThreadPageResult<Destination.MaterialThreadRankList>(navigator, viewModel::onThreadResult)

    LaunchedBackToTopFabStateEffect(
        listState = listState,
        onVisibilityChanged = { isBackToTopVisible = it },
        isRefreshing = uiState.isRefreshing,
        isError = uiState.error != null,
    )

    LaunchedEffect(uiState.rankCard?.tabCode, uiState.rankCard?.threads?.size) {
        if (!initialRankPositionApplied && uiState.rankCard?.threads?.size.orZero() >= 6) {
            // One header item precedes the threads, so lazy-list index 6 is rank 6.
            listState.scrollToItem(6)
            initialRankPositionApplied = true
        }
    }

    val title = uiState.tabs.firstOrNull { it.tabCode == uiState.selectedTabCode }?.title
        ?: uiState.rankCard?.title
        ?: when (uiState.selectedTabCode) {
            HotTopicRepository.THREAD_RANK_AGREE -> "点赞最多贴"
            HotTopicRepository.THREAD_RANK_VIEW -> "浏览最多贴"
            else -> "帖子榜"
        }

    MyScaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.mainTopBarDividers(),
                title = { Text(title) },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = navigator::navigateUp)
                },
                expandedHeight = 56.dp,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            DefaultBackToTopFAB(visible = isBackToTopVisible) {
                coroutineScope.launch { listState.animateScrollToTop() }
            }
        },
    ) { contentPadding ->
        StateScreen(
            isEmpty = uiState.isEmpty,
            isLoading = uiState.isRefreshing && uiState.isEmpty,
            error = uiState.error,
            onReload = viewModel::onRefresh,
            screenPadding = contentPadding,
        ) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize(),
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::onRefresh,
                contentPadding = contentPadding,
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF5F5F5)),
                    state = listState,
                    contentPadding = contentPadding,
                ) {
                    item(key = "rank_rule") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = uiState.rankCard?.sortRule.orEmpty(),
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            uiState.updatedAtMillis?.let { updatedAt ->
                                Text(
                                    text = "更新于 ${formatMaterialRankTime(updatedAt)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        HorizontalDivider(
                            thickness = dividerThickness,
                            color = DividerDefaults.color,
                        )
                    }

                    val threads = uiState.rankCard?.threads.orEmpty()
                    itemsIndexed(
                        items = threads,
                        key = { _, item -> item.threadId },
                    ) { index, item ->
                        MaterialThreadRankListItem(
                            item = item,
                            rank = index + 1,
                            onThreadClick = { thread ->
                                navigator.navigateDebounced(
                                    Destination.Thread(
                                        threadId = thread.threadId,
                                        forumId = thread.forumId,
                                    )
                                )
                            },
                            onReplyClick = { thread ->
                                navigator.navigateDebounced(
                                    Destination.Thread(
                                        threadId = thread.threadId,
                                        forumId = thread.forumId,
                                        scrollToReply = true,
                                    )
                                )
                            },
                            onLikeClick = viewModel::onLikeClicked,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                        )
                        if (index != threads.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 16.dp),
                                thickness = dividerThickness,
                                color = DividerDefaults.color,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMaterialRankTime(timestamp: Long): String =
    SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun Int?.orZero(): Int = this ?: 0
