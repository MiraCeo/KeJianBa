package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.repository.ForumSquareRecommendationLoginRequired
import com.huanchengfly.tieba.post.repository.ForumSquareRecommendationUnavailable
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.MainCardStyle
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.OnMainNavigationScrollTopEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatar
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
internal fun ForumSquarePage(
    contentPadding: PaddingValues,
    isActive: Boolean,
    viewModel: ForumSquareViewModel = hiltViewModel(),
) {
    val uid = LocalAccount.current?.uid
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalNavController.current
    viewModel.uiEvent.collectCommonUiEventWithLifecycle()
    LaunchedEffect(uid, isActive) { viewModel.activate(uid, isActive) }
    DisposableEffect(viewModel) { onDispose { viewModel.deactivate() } }

    val surfaceColor = if (MainCardStyle.enabled) MainCardStyle.container else MaterialTheme.colorScheme.surface
    Column(
        Modifier.fillMaxSize().padding(contentPadding).background(surfaceColor),
    ) {
        // Full-width entry mirroring the official client's 全站搜吧 field: it separates the
        // top bar from the two-column content and gives the category rail a top boundary.
        // Search is unified app-wide, so this just opens the search page, which already
        // starts on its 搜吧 tab.
        ForumSquareSearchEntry(
            fieldColor = surfaceColor,
            onClick = { navigator.navigateDebounced(Destination.Search) },
        )
        Row(
            // Flush columns: only scaffold/system insets remain, with no card gutter.
            Modifier.fillMaxSize().background(surfaceColor),
        ) {
            key(uid) {
                ForumSquareCategoryRail(
                    categories = state.categories.ifEmpty { listOf(state.category) },
                    selectedCategory = state.category,
                    surfaceColor = surfaceColor,
                    onSelect = viewModel::selectCategory,
                )
            }
            key(uid, state.category) {
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()
                OnMainNavigationScrollTopEvent<MainDestination.Home>(listState = { listState.takeIf { isActive } })
                LaunchedEffect(isActive, state.page, state.refreshing, state.loadingMore, state.error, state.paused) {
                    if (!isActive || state.refreshing || state.loadingMore || state.error != null || state.paused) return@LaunchedEffect
                    snapshotFlow {
                        val layout = listState.layoutInfo
                        layout.totalItemsCount > 0 &&
                            (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) >= layout.totalItemsCount - 3
                    }.distinctUntilChanged().collect { nearEnd ->
                        if (nearEnd) viewModel.loadMore()
                    }
                }
                Column(Modifier.weight(1f).fillMaxSize().background(surfaceColor)) {
                    PullToRefreshBox(
                        isRefreshing = state.refreshing,
                        onRefresh = {
                            scope.launch { listState.scrollToItem(0) }
                            viewModel.refresh()
                        },
                        enabled = isActive,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                        ) {
                            items(state.forums, key = { it.forum_id }) { forum ->
                                // Square records may belong to either followed or unfollowed forums.
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        navigator.navigateDebounced(Destination.Forum(forumName = forum.forum_name, avatar = forum.avatar))
                                    }.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ForumAvatar(data = forum.avatar, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.width(14.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            forum.forum_name,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "关注${forum.member_count.toLong().getShortNumString()}  帖子${forum.thread_count.toLong().getShortNumString()}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = Color(0xFFA5A6AE),
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (uid == null) navigator.navigateDebounced(Destination.Login)
                                            else viewModel.toggleFollow(forum)
                                        },
                                        enabled = forum.forum_id !in state.followBusy,
                                        modifier = Modifier.width(60.dp).height(32.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (forum.is_like != 0) Color(0xFFF3F3F3) else Color(0xFFEEF2FF),
                                            contentColor = if (forum.is_like != 0) Color(0xFFA8A8A8) else Color(0xFF5C86F6),
                                            disabledContainerColor = if (forum.is_like != 0) Color(0xFFF3F3F3) else Color(0xFFEEF2FF),
                                            disabledContentColor = if (forum.is_like != 0) Color(0xFFA8A8A8) else Color(0xFF5C86F6),
                                        ),
                                        contentPadding = PaddingValues(0.dp),
                                    ) {
                                        Text(
                                            if (forum.is_like != 0) stringResource(R.string.text_followed)
                                            else stringResource(R.string.button_follow),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                            // Only emit the footer when it actually renders something. A footer
                            // that is always present becomes the scroll anchor while the list is
                            // still empty, and LazyColumn then preserves its position as the first
                            // page is inserted in front of it, leaving the user mid-list.
                            val showStatus = state.loadingMore || state.error != null ||
                                (!state.refreshing && state.initialized)
                            if (showStatus) item(key = "status") {
                                Column(
                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    when {
                                        state.loadingMore -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                        state.error != null -> {
                                            val message = when (state.error) {
                                                is ForumSquareRecommendationLoginRequired -> R.string.forum_square_recommend_login
                                                is ForumSquareRecommendationUnavailable -> R.string.forum_square_recommend_unavailable
                                                else -> R.string.forum_square_load_error
                                            }
                                            Text(stringResource(message), textAlign = TextAlign.Center)
                                            TextButton(onClick = {
                                                if (state.errorIsRefresh) viewModel.refresh()
                                                else viewModel.loadMore(manual = true)
                                            }) { Text(stringResource(R.string.button_retry)) }
                                        }
                                        !state.refreshing && state.initialized -> {
                                            if (state.forums.isEmpty()) Text(stringResource(R.string.forum_square_empty))
                                            if (state.paused) Text(stringResource(R.string.forum_square_paused), textAlign = TextAlign.Center)
                                            if (state.hasMore) {
                                                TextButton(onClick = { viewModel.loadMore(manual = true) }) {
                                                    Text(stringResource(R.string.forum_square_load_more))
                                                }
                                            } else if (state.forums.isNotEmpty()) {
                                                Text(stringResource(R.string.no_more), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumSquareSearchEntry(fieldColor: Color, onClick: () -> Unit) {
    // Tieba's own treatment: the surrounding band carries the page background and the field
    // itself is content-white. Sampled from the official client: #F6F6F8 band, #FFFFFF field.
    val trackColor = if (MainCardStyle.enabled) MainCardStyle.background
    else MaterialTheme.colorScheme.background
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(trackColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(fieldColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.forum_square_search_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
