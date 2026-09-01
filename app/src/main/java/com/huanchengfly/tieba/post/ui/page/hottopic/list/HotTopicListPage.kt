package com.huanchengfly.tieba.post.ui.page.hottopic.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.topicList.NewTopicList
import com.huanchengfly.tieba.post.arch.collectCommonUiEventWithLifecycle
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.theme.OrangeA700
import com.huanchengfly.tieba.post.theme.RedA700
import com.huanchengfly.tieba.post.theme.YellowA700
import com.huanchengfly.tieba.post.ui.common.theme.compose.BebasFamily
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.main.mainTopBarDividers
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BlurScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.LaunchedBackToTopFabStateEffect
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.NetworkImage
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.animateScrollToTop
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun RankedTopicImage(
    imageUri: String,
    index: Int,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        NetworkImage(
            modifier = Modifier.fillMaxSize(),
            imageUrl = imageUri,
            contentScale = ContentScale.Crop,
        )
        TopicRankBadge(
            index = index,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun TopicRankBadge(
    index: Int,
    modifier: Modifier = Modifier,
) {
    val containerColor = when (index) {
        0 -> RedA700
        1 -> OrangeA700
        2 -> YellowA700
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
    }
    val contentColor = when (index) {
        0, 1 -> Color.White
        2 -> Color(0xFF5D4900)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .width(20.dp)
            .height(18.dp)
            .background(
                color = containerColor,
                shape = RoundedCornerShape(bottomEnd = 4.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${index + 1}",
            color = contentColor,
            fontFamily = BebasFamily,
            fontSize = if (index < 3) 15.sp else 13.sp,
            lineHeight = if (index < 3) 15.sp else 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun TopicStatusTag(isHot: Boolean) {
    val contentColor = if (isHot) RedA700 else OrangeA700
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(
                color = contentColor.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.extraSmall,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = if (isHot) R.string.topic_tag_hot else R.string.topic_tag_new),
            color = contentColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Medium,
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
        )
    }
}

@Composable
private fun TopicHeat(
    item: NewTopicList,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val heatValue = item.discuss_num.getShortNumString()
    val heatColor = color.copy(alpha = 0.85f)
    val heatDescription = stringResource(id = R.string.hot_num, heatValue)

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = heatDescription
        },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedHeatIcon(
            color = heatColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = heatValue,
            style = MaterialTheme.typography.labelSmall,
            color = heatColor,
            maxLines = 1,
        )
    }
}

@Composable
private fun OutlinedHeatIcon(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 1.4.dp,
) {
    Canvas(modifier = modifier) {
        val flamePath = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.95f)
            cubicTo(
                size.width * 0.29f,
                size.height * 0.94f,
                size.width * 0.15f,
                size.height * 0.82f,
                size.width * 0.16f,
                size.height * 0.65f,
            )
            cubicTo(
                size.width * 0.17f,
                size.height * 0.52f,
                size.width * 0.25f,
                size.height * 0.39f,
                size.width * 0.31f,
                size.height * 0.29f,
            )
            cubicTo(
                size.width * 0.31f,
                size.height * 0.39f,
                size.width * 0.34f,
                size.height * 0.47f,
                size.width * 0.40f,
                size.height * 0.51f,
            )
            cubicTo(
                size.width * 0.42f,
                size.height * 0.34f,
                size.width * 0.44f,
                size.height * 0.16f,
                size.width * 0.53f,
                size.height * 0.06f,
            )
            cubicTo(
                size.width * 0.54f,
                size.height * 0.23f,
                size.width * 0.63f,
                size.height * 0.35f,
                size.width * 0.66f,
                size.height * 0.50f,
            )
            cubicTo(
                size.width * 0.71f,
                size.height * 0.44f,
                size.width * 0.76f,
                size.height * 0.33f,
                size.width * 0.82f,
                size.height * 0.25f,
            )
            cubicTo(
                size.width * 0.82f,
                size.height * 0.38f,
                size.width * 0.91f,
                size.height * 0.48f,
                size.width * 0.91f,
                size.height * 0.63f,
            )
            cubicTo(
                size.width * 0.92f,
                size.height * 0.80f,
                size.width * 0.76f,
                size.height * 0.93f,
                size.width * 0.57f,
                size.height * 0.95f,
            )
            cubicTo(
                size.width * 0.54f,
                size.height * 0.96f,
                size.width * 0.51f,
                size.height * 0.96f,
                size.width * 0.48f,
                size.height * 0.95f,
            )
            close()
        }
        drawPath(
            path = flamePath,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private data class TrailingTextLayout(
    val leadingLines: List<String>,
    val trailingLine: String,
)

@Composable
private fun TwoLineTopicDescriptionWithTrailingHeat(
    item: NewTopicList,
    heatColor: Color,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.bodySmall
    val heatStyle = MaterialTheme.typography.labelSmall
    val heatValue = item.discuss_num.getShortNumString()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fullWidth = constraints.maxWidth
        val heatDecorationWidth = with(density) { 17.dp.roundToPx() }
        val heatWidth = remember(heatValue, heatStyle, heatDecorationWidth) {
            textMeasurer.measure(
                text = AnnotatedString(heatValue),
                style = heatStyle,
                maxLines = 1,
            ).size.width + heatDecorationWidth
        }
        val heatGap = with(density) { 6.dp.roundToPx() }
        val trailingTextWidth = (fullWidth - heatWidth - heatGap).coerceAtLeast(0)
        val lineLayout = remember(
            item.topic_desc,
            fullWidth,
            trailingTextWidth,
            textStyle,
        ) {
            val description = item.topic_desc.trim()

            fun measure(value: String, width: Int) = textMeasurer.measure(
                text = AnnotatedString(value),
                style = textStyle,
                constraints = Constraints(maxWidth = width),
            )

            fun fitsTrailingLine(value: String): Boolean =
                value.isEmpty() || measure(value, trailingTextWidth).lineCount <= 1

            if (fitsTrailingLine(description)) {
                TrailingTextLayout(emptyList(), description)
            } else {
                val firstLayout = measure(description, fullWidth)
                val firstEnd = firstLayout.getLineEnd(lineIndex = 0, visibleEnd = true)
                val firstLine = description.take(firstEnd).trimEnd()
                val afterFirstLine = description.drop(firstEnd).trimStart()

                TrailingTextLayout(listOf(firstLine), afterFirstLine)
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            lineLayout.leadingLines.forEach { line ->
                Text(
                    text = line,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = textStyle,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = lineLayout.trailingLine,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = textStyle,
                )
                TopicHeat(item = item, color = heatColor)
            }
        }
    }
}

@Composable
private fun TopicListDivider(modifier: Modifier = Modifier) {
    val twoPhysicalPixels = with(LocalDensity.current) { 2f.toDp() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(twoPhysicalPixels),
    ) {
        val lineY = size.height / 2f
        drawLine(
            color = Color(0xFFDDDDDD),
            start = Offset(0f, lineY),
            end = Offset(size.width, lineY),
            strokeWidth = 2f,
        )
    }
}

@Composable
private fun TopicBody(
    item: NewTopicList,
    index: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.topic_name,
                modifier = Modifier.weight(1.0f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (index < 3) {
                        FontWeight.SemiBold
                    } else {
                        MaterialTheme.typography.titleMedium.fontWeight
                    },
                ),
            )
            when (item.topic_tag) {
                2 -> TopicStatusTag(isHot = true)

                1 -> TopicStatusTag(isHot = false)
            }
        }
        TwoLineTopicDescriptionWithTrailingHeat(
            item = item,
            heatColor = if (index < 3) RedA700 else OrangeA700,
        )
    }
}

private val HotTopicHeaderStartColor = Color(0xFFE5F5FF)
private val HotTopicHeaderEndColor = Color(0xFFEDE8FF)
private val HotTopicHeaderInk = Color(0xFF183467)
private val HotTopicHeaderBrush = Brush.horizontalGradient(
    colors = listOf(HotTopicHeaderStartColor, HotTopicHeaderEndColor),
)

private enum class RefreshContentPhase {
    Idle,
    FadingOut,
    Holding,
    FadingIn,
}

private fun formatUpdateTime(timestamp: Long): String {
    return SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(timestamp))
}

@Composable
private fun RefreshAction(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        modifier = modifier,
        enabled = !isRefreshing,
        onClick = onRefresh,
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = stringResource(R.string.title_refresh),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun HotTopicArtwork() {
    Column(
        modifier = Modifier.graphicsLayer(rotationZ = -0.8f),
        verticalArrangement = Arrangement.spacedBy((-2).dp),
    ) {
        Text(
            text = stringResource(R.string.hot_topic_header_tieba),
            color = HotTopicHeaderInk.copy(alpha = 0.78f),
            fontSize = 12.sp,
            lineHeight = 13.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.hot_topic_rank),
            color = HotTopicHeaderInk,
            fontSize = 27.sp,
            lineHeight = 29.sp,
            letterSpacing = (-0.6).sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun LayeredHeaderFlame(
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val outerFlame = Path().apply {
            moveTo(size.width * 0.48f, size.height * 0.95f)
            cubicTo(size.width * 0.29f, size.height * 0.94f, size.width * 0.15f, size.height * 0.82f, size.width * 0.16f, size.height * 0.65f)
            cubicTo(size.width * 0.17f, size.height * 0.52f, size.width * 0.25f, size.height * 0.39f, size.width * 0.31f, size.height * 0.29f)
            cubicTo(size.width * 0.31f, size.height * 0.39f, size.width * 0.34f, size.height * 0.47f, size.width * 0.40f, size.height * 0.51f)
            cubicTo(size.width * 0.42f, size.height * 0.34f, size.width * 0.44f, size.height * 0.16f, size.width * 0.53f, size.height * 0.06f)
            cubicTo(size.width * 0.54f, size.height * 0.23f, size.width * 0.63f, size.height * 0.35f, size.width * 0.66f, size.height * 0.50f)
            cubicTo(size.width * 0.71f, size.height * 0.44f, size.width * 0.76f, size.height * 0.33f, size.width * 0.82f, size.height * 0.25f)
            cubicTo(size.width * 0.82f, size.height * 0.38f, size.width * 0.91f, size.height * 0.48f, size.width * 0.91f, size.height * 0.63f)
            cubicTo(size.width * 0.92f, size.height * 0.80f, size.width * 0.76f, size.height * 0.93f, size.width * 0.57f, size.height * 0.95f)
            cubicTo(size.width * 0.54f, size.height * 0.96f, size.width * 0.51f, size.height * 0.96f, size.width * 0.48f, size.height * 0.95f)
            close()
        }
        val nestingCenter = Offset(size.width * 0.53f, size.height * 0.60f)
        drawPath(outerFlame, Color(0xFFFFB13B).copy(alpha = 0.16f))
        scale(scaleX = 0.72f, scaleY = 0.72f, pivot = nestingCenter) {
            drawPath(outerFlame, Color(0xFFFF7638).copy(alpha = 0.20f))
        }
        scale(scaleX = 0.44f, scaleY = 0.44f, pivot = nestingCenter) {
            drawPath(outerFlame, Color(0xFFEF4444).copy(alpha = 0.24f))
        }
    }
}

@Composable
private fun HotTopicHeaderAnchor() {
    LayeredHeaderFlame(
        modifier = Modifier
            .offset(x = 30.dp, y = (-8).dp)
            .size(width = 132.dp, height = 108.dp),
    )
}

@Composable
private fun HotTopicHero(
    lastUpdatedAtMillis: Long?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val updateTime = lastUpdatedAtMillis?.let(::formatUpdateTime)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Transparent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            HotTopicArtwork()
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .width(1.dp)
                    .height(28.dp)
                    .background(HotTopicHeaderInk.copy(alpha = 0.24f)),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.hot_topic_updated_label),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = updateTime ?: stringResource(R.string.hot_topic_updating),
                        color = HotTopicHeaderInk,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                RefreshAction(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HotTopicRankingHeader(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.hot_topic_rank_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HotTopicHeader(
    lastUpdatedAtMillis: Long?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clipToBounds()
            .background(HotTopicHeaderBrush),
    ) {
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            HotTopicHeaderAnchor()
        }
        HotTopicHero(
            lastUpdatedAtMillis = lastUpdatedAtMillis,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.align(Alignment.TopStart),
        )
        HotTopicRankingHeader(
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun HotTopicList(
    uiState: HotTopicListUiState,
    onRefresh: () -> Unit = {},
    onTopicClicked: (NewTopicList) -> Unit = {},
    navigateUp: () -> Unit = {},
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()

    StateScreen(
        isEmpty = uiState.topicList.isEmpty(),
        isLoading = uiState.isRefreshing && uiState.topicList.isEmpty(),
        error = uiState.error,
        onReload = onRefresh
    ) {
        val lazyListState = rememberLazyListState()
        var isBackToTopFabVisible by remember { mutableStateOf(false) }
        var displayedTopicList by remember { mutableStateOf(uiState.topicList) }
        var displayedUpdatedAtMillis by remember { mutableStateOf(uiState.lastUpdatedAtMillis) }
        var pendingTopicList by remember { mutableStateOf(uiState.topicList) }
        var pendingUpdatedAtMillis by remember { mutableStateOf(uiState.lastUpdatedAtMillis) }
        var refreshContentPhase by remember { mutableStateOf(RefreshContentPhase.Idle) }
        var refreshCycle by remember { mutableIntStateOf(0) }
        var minimumBlankElapsed by remember { mutableStateOf(false) }
        var refreshResponseReady by remember { mutableStateOf(false) }
        val isRefreshTransitionRunning = uiState.isRefreshing ||
                refreshContentPhase != RefreshContentPhase.Idle
        val requestRefreshWithTransition: () -> Unit = {
            if (!isRefreshTransitionRunning && displayedTopicList.isNotEmpty()) {
                minimumBlankElapsed = false
                refreshResponseReady = false
                refreshContentPhase = RefreshContentPhase.FadingOut
                refreshCycle += 1
            }
            onRefresh()
        }
        val listContentAlpha by animateFloatAsState(
            targetValue = when (refreshContentPhase) {
                RefreshContentPhase.FadingOut, RefreshContentPhase.Holding -> 0f
                RefreshContentPhase.Idle, RefreshContentPhase.FadingIn -> 1f
            },
            animationSpec = tween(durationMillis = 300),
            label = "hot_topic_refresh_content_alpha",
        )

        LaunchedEffect(uiState.isRefreshing, uiState.lastUpdatedAtMillis, uiState.error) {
            if (uiState.isRefreshing && displayedTopicList.isNotEmpty()) {
                if (refreshContentPhase != RefreshContentPhase.FadingOut &&
                    refreshContentPhase != RefreshContentPhase.Holding
                ) {
                    minimumBlankElapsed = false
                    refreshResponseReady = false
                    refreshContentPhase = RefreshContentPhase.FadingOut
                    refreshCycle += 1
                }
            } else if (!uiState.isRefreshing) {
                pendingTopicList = uiState.topicList
                pendingUpdatedAtMillis = uiState.lastUpdatedAtMillis
                refreshResponseReady = true
                if (minimumBlankElapsed && refreshContentPhase != RefreshContentPhase.Idle) {
                    displayedTopicList = pendingTopicList
                    displayedUpdatedAtMillis = pendingUpdatedAtMillis
                    refreshContentPhase = RefreshContentPhase.FadingIn
                } else if (refreshContentPhase == RefreshContentPhase.Idle) {
                    displayedTopicList = uiState.topicList
                    displayedUpdatedAtMillis = uiState.lastUpdatedAtMillis
                }
            }
        }

        LaunchedEffect(refreshCycle) {
            if (refreshCycle == 0) return@LaunchedEffect
            delay(300)
            if (refreshContentPhase == RefreshContentPhase.FadingOut) {
                refreshContentPhase = RefreshContentPhase.Holding
            }
            delay(100)
            minimumBlankElapsed = true
            if (refreshResponseReady) {
                displayedTopicList = pendingTopicList
                displayedUpdatedAtMillis = pendingUpdatedAtMillis
                refreshContentPhase = RefreshContentPhase.FadingIn
            }
        }

        LaunchedEffect(refreshContentPhase) {
            if (refreshContentPhase == RefreshContentPhase.FadingIn) {
                delay(300)
                refreshContentPhase = RefreshContentPhase.Idle
            }
        }
        val heroHeightPx = with(LocalDensity.current) { 60.dp.toPx() }
        val headerCollapseProgress by remember(lazyListState, heroHeightPx) {
            derivedStateOf {
                when {
                    lazyListState.firstVisibleItemIndex > 0 -> 1f
                    heroHeightPx <= 0f -> 0f
                    else -> (lazyListState.firstVisibleItemScrollOffset / heroHeightPx)
                        .coerceIn(0f, 1f)
                }
            }
        }
        val collapsedTopBarContentVisible = headerCollapseProgress >= 0.65f
        val topBarModifier = Modifier
            .background(HotTopicHeaderBrush)
            .background(MaterialTheme.colorScheme.background.copy(alpha = headerCollapseProgress))
            .then(
                if (headerCollapseProgress >= 0.99f) {
                    Modifier.mainTopBarDividers()
                } else {
                    Modifier
                }
            )

        LaunchedBackToTopFabStateEffect(
            listState = lazyListState,
            onVisibilityChanged = { isBackToTopFabVisible = it },
            isRefreshing = isRefreshTransitionRunning,
            isError = uiState.error != null,
        )

        BlurScaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = topBarModifier,
                    title = {
                        AnimatedVisibility(
                            visible = collapsedTopBarContentVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Text(
                                text = stringResource(R.string.title_hot_message_list),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = navigateUp)
                    },
                    actions = {
                        AnimatedVisibility(
                            visible = collapsedTopBarContentVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            RefreshAction(
                                isRefreshing = isRefreshTransitionRunning,
                                onRefresh = requestRefreshWithTransition,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    expandedHeight = 56.dp,
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                DefaultBackToTopFAB(visible = isBackToTopFabVisible) {
                    coroutineScope.launch {
                        lazyListState.animateScrollToTop()
                        scrollBehavior.state.contentOffset = 0f
                    }
                }
            },
        ) { contentPaddings ->
            Container {
                PullToRefreshBox(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HotTopicHeaderBrush),
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = requestRefreshWithTransition,
                    contentPadding = contentPaddings,
                ) {
                    MyLazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        verticalArrangement = Arrangement.Top,
                        contentPadding = contentPaddings,
                        overscrollEffect = null,
                    ) {
                        item(key = "hot_topic_header") {
                            HotTopicHeader(
                                lastUpdatedAtMillis = displayedUpdatedAtMillis,
                                isRefreshing = isRefreshTransitionRunning,
                                onRefresh = requestRefreshWithTransition,
                            )
                        }

                        itemsIndexed(
                            items = displayedTopicList,
                            key = { _, item -> item.topic_id },
                        ) { index, item ->
                            val topicClickedListener: () -> Unit = { onTopicClicked(item) }

                            Column(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                                    .graphicsLayer { alpha = listContentAlpha },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = topicClickedListener)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    RankedTopicImage(
                                        imageUri = item.topic_image,
                                        index = index,
                                    )
                                    TopicBody(
                                        item = item,
                                        index = index,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (index < displayedTopicList.lastIndex) {
                                    TopicListDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
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
fun HotTopicListPage(
    viewModel: HotTopicListViewModel = hiltViewModel<HotTopicListViewModel>(),
    navigator: NavController,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    viewModel.uiEvent.collectCommonUiEventWithLifecycle()

    HotTopicList(
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onTopicClicked = { item ->
            navigator.navigateDebounced(
                route = Destination.HotTopicDetail(item.topic_id, item.topic_name)
            )
        },
        navigateUp = navigator::navigateUp,
    )
}
