package com.huanchengfly.tieba.post.ui.page.main.explore.concern

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.ExploreFeedStyle
import com.huanchengfly.tieba.post.ui.page.main.explore.ExploreFeedStyle.feedCard
import com.huanchengfly.tieba.post.ui.page.main.explore.LaunchedFabStateEffect
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.SwipeUpLazyLoadColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.ThreadContentType
import com.huanchengfly.tieba.post.ui.widgets.compose.defaultBottomIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen

@Composable
fun ConcernPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    navigator: NavController,
    onHideFab: (Boolean) -> Unit,
    viewModel: ConcernViewModel = hiltViewModel(),
    isActive: Boolean = true,
) {
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isRefreshing,
        initial = true
    )
    val isEmpty by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::isEmpty,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = ConcernUiState::error,
        initial = null
    )

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    LaunchedFabStateEffect(listState, onHideFab, isRefreshing, isError = error != null)

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigateDebounced)
    }

    if (isActive) {
        ConsumeThreadPageResult<Destination.Main>(navigator, viewModel::onThreadResult)
    }

    StateScreen(
        isEmpty = isEmpty,
        isLoading = isRefreshing && isEmpty,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding,
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val isLoadingMore = uiState.isLoadingMore
            val data = uiState.data

            SwipeUpLazyLoadColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = contentPadding,
                isLoading = isLoadingMore,
                onLazyLoad = viewModel::onLoadMore.takeIf { uiState.hasMore },
                bottomIndicator = defaultBottomIndicator,
            ) {
                if (uiState.followedUsers.isNotEmpty()) {
                    item(key = "followed-users") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .feedCard(),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                                    Icon(
                                        imageVector = Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.title_my_following),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            LazyRow(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(uiState.followedUsers, key = { it.uid }) { user ->
                                    Column(
                                        modifier = Modifier
                                            .width(56.dp)
                                            .clickable {
                                                navigator.navigateDebounced(
                                                    Destination.UserProfile(
                                                        uid = user.uid,
                                                        avatar = user.avatar,
                                                        nickname = user.displayName,
                                                    )
                                                )
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Avatar(
                                            data = user.avatar,
                                            size = 44.dp,
                                            shape = CircleShape,
                                        )
                                        Text(
                                            text = user.displayName,
                                            modifier = Modifier.padding(top = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                itemsIndexed(data, key = { _, it -> it.id }, ThreadContentType) { i, thread ->
                    FeedCard(
                        modifier = Modifier.feedCard(),
                        thread = thread,
                        onClick = threadClickListeners.onClicked,
                        onLike = viewModel::onThreadLikeClicked,
                        onClickReply = threadClickListeners.onReplyClicked,
                        onClickUser = threadClickListeners.onAuthorClicked,
                        onClickForum = threadClickListeners.onForumClicked,
                        cardDivider = !ExploreFeedStyle.useCards && i < data.lastIndex
                    )
                }
            }
        }
    }
}
