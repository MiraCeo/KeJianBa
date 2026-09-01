package com.huanchengfly.tieba.post.ui.page.main.explore.hot

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.MacrobenchmarkConstant
import com.huanchengfly.tieba.post.MacrobenchmarkConstant.testColumn
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.OrangeA700
import com.huanchengfly.tieba.post.theme.RedA700
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.theme.YellowA700
import com.huanchengfly.tieba.post.ui.common.theme.compose.BebasFamily
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.models.explore.HotTab
import com.huanchengfly.tieba.post.ui.models.explore.HotRankTopic
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankCard
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankItem
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankMedia
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.explore.ConsumeThreadPageResult
import com.huanchengfly.tieba.post.ui.page.main.explore.ExploreFeedStyle
import com.huanchengfly.tieba.post.ui.page.main.explore.ExploreFeedStyle.feedCard
import com.huanchengfly.tieba.post.ui.page.main.explore.createThreadClickListeners
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCard
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatar
import com.huanchengfly.tieba.post.ui.widgets.compose.LaunchedBackToTopFabStateEffect
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.icons.CommentNew
import com.huanchengfly.tieba.post.ui.widgets.compose.AnimatedLikeThumbIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.trace

private enum class HotType {
    TopicList, MaterialThreadRank, ThreadTabs, Thread, PlaceHolder
}

@Composable
fun HotPage(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    listState: LazyListState = rememberLazyListState(),
    navigator: NavController,
    onHideFab: (Boolean) -> Unit,
    viewModel: HotViewModel = hiltViewModel()
) {
    val isRefreshing by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::isRefreshing,
        initial = true
    )

    val error by viewModel.uiState.collectPartialAsState(
        prop1 = HotUiState::error,
        initial = null
    )
    val isError = error != null

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    LaunchedBackToTopFabStateEffect(
        listState = listState,
        onVisibilityChanged = { visible -> onHideFab(!visible) },
        isRefreshing = isRefreshing,
        isError = isError,
    )

    val threadClickListeners = remember(navigator) {
        createThreadClickListeners(onNavigate = navigator::navigateDebounced)
    }

    ConsumeThreadPageResult<Destination.Main>(navigator, viewModel::onThreadResult)

    StateScreen(
        isLoading = isRefreshing,
        error = error,
        onReload = viewModel::onRefresh,
        screenPadding = contentPadding
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::onRefresh,
            contentPadding = contentPadding
        ) {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val topicList = uiState.topics
            val threadList = uiState.threads

            LazyColumn(
                modifier = modifier.fillMaxSize().testColumn(),
                state = listState,
                contentPadding = contentPadding,
            ) {
                if (topicList.isNotEmpty()) {
                    item(key = HotType.TopicList, contentType = HotType.TopicList) {
                        HotTopicRankCard(
                            topics = topicList,
                            titleRes = R.string.hot_topic_rank,
                            onTopicClick = { topic ->
                                navigator.navigateDebounced(
                                    Destination.HotTopicDetail(topic.topicId, topic.topicName)
                                )
                            },
                            onViewAll = threadClickListeners.onNavigateHotTopicList,
                        )
                    }

                }

                items(
                    items = uiState.materialThreadRanks,
                    key = { it.tabCode },
                    contentType = { HotType.MaterialThreadRank },
                ) { rankCard ->
                    MaterialThreadRankCardSection(
                        rankCard = rankCard,
                        onThreadClick = { item ->
                            navigator.navigateDebounced(
                                Destination.Thread(
                                    threadId = item.threadId,
                                    forumId = item.forumId,
                                )
                            )
                        },
                        onReplyClick = { item ->
                            navigator.navigateDebounced(
                                Destination.Thread(
                                    threadId = item.threadId,
                                    forumId = item.forumId,
                                    scrollToReply = true,
                                )
                            )
                        },
                        onLikeClick = viewModel::onMaterialThreadLikeClicked,
                        onViewMore = { card ->
                            navigator.navigateDebounced(
                                Destination.MaterialThreadRankList(card.tabCode)
                            )
                        },
                    )
                }

                if (uiState.tabs.isNotEmpty()) {
                    item(key = HotType.ThreadTabs, contentType = HotType.ThreadTabs) {
                        ThreadTabs(uiState.tabs, uiState.selectedTab, viewModel::onTabSelected)
                    }
                }

                if (threadList.isNullOrEmpty()) {
                    items(4, contentType = { HotType.PlaceHolder }) {
                        Box(modifier = Modifier.feedCard()) {
                            FeedCardPlaceholder()
                        }
                    }
                    return@LazyColumn
                }

                itemsIndexed(
                    items = threadList,
                    key = { _, thread -> thread.id },
                    contentType = { _,_ -> HotType.Thread }
                ) { index, thread ->
                    trace(MacrobenchmarkConstant.TRACE_FEED_CARD) {
                        FeedCard(
                            modifier = Modifier.feedCard(),
                            thread = thread,
                            onClick = threadClickListeners.onClicked,
                            onLike = viewModel::onThreadLikeClicked,
                            onClickReply = threadClickListeners.onReplyClicked,
                            onClickUser = threadClickListeners.onAuthorClicked,
                            onClickForum = threadClickListeners.onForumClicked,
                            cardDivider = !ExploreFeedStyle.useCards && index != threadList.lastIndex,
                        ) {
                            HotRankText(rank = index + 1, hotNum = thread.hotNum)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialThreadRankCardSection(
    rankCard: MaterialThreadRankCard,
    onThreadClick: (MaterialThreadRankItem) -> Unit,
    onReplyClick: (MaterialThreadRankItem) -> Unit,
    onLikeClick: (MaterialThreadRankItem) -> Unit,
    onViewMore: (MaterialThreadRankCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dividerThickness = with(LocalDensity.current) { 1f.toDp() }

    Column(modifier = modifier.feedCard()) {
        MaterialRankHeading(
            title = rankCard.title,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
        )

        rankCard.threads.forEachIndexed { index, item ->
            MaterialThreadRankListItem(
                item = item,
                rank = index + 1,
                onThreadClick = onThreadClick,
                onReplyClick = onReplyClick,
                onLikeClick = onLikeClick,
            )

            if (index != rankCard.threads.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = dividerThickness,
                    color = Color(0xFFDDDDDD),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = dividerThickness,
            color = Color(0xFFDDDDDD),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewMore(rankCard) }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = rankCard.moreText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun MaterialThreadRankListItem(
    item: MaterialThreadRankItem,
    rank: Int,
    onThreadClick: (MaterialThreadRankItem) -> Unit,
    onReplyClick: (MaterialThreadRankItem) -> Unit,
    onLikeClick: (MaterialThreadRankItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val rankColor = when (rank) {
        1 -> RedA700
        2 -> OrangeA700
        else -> Color(0xFFFF9800)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onThreadClick(item) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            rankColor.copy(alpha = 0.16f),
                            rankColor.copy(alpha = 0.08f),
                            Color.Transparent,
                        )
                    )
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(rankColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = rank.toString(),
                    color = Color.White,
                    fontFamily = BebasFamily,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            MaterialRankMetricText(text = item.metricLabel, color = rankColor)
        }

        Text(
            text = item.title,
            modifier = Modifier.padding(top = 2.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (item.abstractText.isNotEmpty()) {
            Text(
                text = item.abstractText,
                modifier = Modifier.padding(top = 4.dp),
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (item.media.isNotEmpty()) {
            MaterialRankMedia(
                media = item.media,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                ForumAvatar(
                    data = item.forumAvatarUrl.ifEmpty { null },
                    size = 16.dp,
                )
                Text(
                    text = item.forumName + "吧",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.agreeNum > 0L || item.replyNum > 0L) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onReplyClick(item) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CommentNew,
                            contentDescription = "评论",
                            modifier = Modifier.size(18.dp),
                            tint = colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = item.replyNum.getShortNumString(),
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickableNoIndication { onLikeClick(item) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AnimatedLikeThumbIcon(
                            liked = item.liked,
                            contentDescription = "点赞",
                            modifier = Modifier.size(18.dp),
                            inactiveColor = colorScheme.onSurface.copy(alpha = 0.78f),
                            activeColor = colorScheme.primary,
                        )
                        Text(
                            text = item.agreeNum.getShortNumString(),
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

private val MaterialRankMetricNumberRegex = Regex("\\d+(?:\\.\\d+)?(?:[Ww万])?")

@Composable
private fun MaterialRankMetricText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val decoratedText = remember(text, color) {
        val number = MaterialRankMetricNumberRegex.find(text)
        buildAnnotatedString {
            if (number == null) {
                withStyle(SpanStyle(color = color.copy(alpha = 0.76f))) {
                    append(text)
                }
            } else {
                withStyle(SpanStyle(color = color.copy(alpha = 0.76f))) {
                    append(text.substring(0, number.range.first))
                }
                withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) {
                    append(number.value)
                }
                withStyle(SpanStyle(color = color.copy(alpha = 0.76f))) {
                    append(text.substring(number.range.last + 1))
                }
            }
        }
    }
    Text(
        text = decoratedText,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MaterialRankHeading(
    title: String,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.titleMedium.copy(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.ExtraBold,
    )
    Text(
        text = title,
        modifier = modifier,
        style = style,
    )
}

@Composable
private fun MaterialRankMedia(
    media: List<MaterialThreadRankMedia>,
    modifier: Modifier = Modifier,
) {
    val visibleMedia = media.take(3)
    if (visibleMedia.size == 1) {
        val item = visibleMedia.first()
        val sourceRatio = if (item.width > 0 && item.height > 0) {
            item.width.toFloat() / item.height.toFloat()
        } else {
            4f / 3f
        }
        val isExtremeRatio = sourceRatio !in 0.6f..2f
        val displayRatio = sourceRatio.coerceIn(0.6f, 2f)
        NetworkImage(
            imageUrl = item.url,
            contentDescription = null,
            contentScale = if (isExtremeRatio) ContentScale.Crop else ContentScale.Fit,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .aspectRatio(displayRatio),
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .aspectRatio(visibleMedia.size.toFloat()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            visibleMedia.forEach { item ->
                NetworkImage(
                    imageUrl = item.url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun HotTopicRankCard(
    topics: List<HotRankTopic>,
    titleRes: Int,
    onTopicClick: (HotRankTopic) -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val visibleTopics = topics.take(5)

    Column(modifier = modifier.feedCard()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.hot_topic_update_rule),
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier
                    .clickableNoIndication(onClick = onViewAll)
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.tip_more_topic),
                    color = colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = Dp.Hairline,
            color = colorScheme.outlineVariant,
        )

        visibleTopics.forEachIndexed { index, topic ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTopicClick(topic) }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = (index + 1).toString(),
                    color = when (index) {
                        0 -> RedA700
                        1 -> OrangeA700
                        2 -> YellowA700
                        else -> colorScheme.onSurfaceVariant
                    },
                    fontFamily = BebasFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp),
                )
                Text(
                    text = topic.topicName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                when (topic.tag) {
                    2 -> TopicTag(modifier = Modifier.padding(start = 8.dp), isHot = true)
                    1 -> TopicTag(modifier = Modifier.padding(start = 8.dp), isHot = false)
                }
                if (topic.discussNum > 0) {
                    Text(
                        text = stringResource(
                            R.string.topic_discuss_num,
                            topic.discussNum.getShortNumString(),
                        ),
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }

            if (index != visibleTopics.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 40.dp, end = 16.dp),
                    thickness = Dp.Hairline,
                    color = colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
fun TopicTag(modifier: Modifier = Modifier, isHot: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(22.dp)
            .background(
                color = if (isHot) RedA700 else OrangeA700,
                shape = MaterialTheme.shapes.extraSmall
            ),
    ) {
        Text(
            text = stringResource(id = if (isHot) R.string.topic_tag_hot else R.string.topic_tag_new),
            fontSize = 10.sp,
            color = Color.White,
        )
    }
}

@Composable
private fun ThreadTabs(
    tabs: List<HotTab>,
    selected: HotTab,
    onTabSelected: (HotTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val dividerThickness = with(LocalDensity.current) { 1f.toDp() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.title_thread_rankings),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.hot_thread_rank_rule),
                color = colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val isSelected = selected.tabCode == tab.tabCode
                val baseName = tab.name.ifEmpty {
                    stringResource(id = R.string.tab_all_hot_thread)
                }
                val displayName = if (baseName.endsWith('榜')) baseName else "${baseName}榜"
                Column(
                    modifier = Modifier
                        .height(44.dp)
                        .clickableNoIndication { onTabSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = displayName,
                        color = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                        fontSize = if (isSelected) 15.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .width(24.dp)
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (isSelected) colorScheme.primary else Color.Transparent)
                    )
                }
            }
        }

        HorizontalDivider(
            thickness = dividerThickness,
            color = Color(0xFFDDDDDD),
        )
    }
}

@Composable
private fun HotRankText(
    modifier: Modifier = Modifier,
    rank: Int,
    hotNum: Int
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val color = when (rank) {
            1 -> RedA700
            2 -> OrangeA700
            3 -> YellowA700
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = rank.toString(),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = stringResource(id = R.string.hot_num, hotNum.getShortNumString()),
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Preview("HotRankText")
@Composable
private fun HotRankTextPreview() = TiebaLiteTheme {
    Surface {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            HotRankText(rank = 1, hotNum = 21000)
            HotRankText(rank = 2, hotNum = 19000)
            HotRankText(rank = 3, hotNum = 15000)
            HotRankText(rank = 4, hotNum = 12000)
        }
    }
}
