package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.placeholder.PlaceholderDefaults
import com.huanchengfly.tieba.post.LocalUISettings
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.plus
import com.huanchengfly.tieba.post.theme.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.ForumAvatarSharedBoundsKey
import com.huanchengfly.tieba.post.ui.ForumTitleSharedBoundsKey
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.animateEnterExit
import com.huanchengfly.tieba.post.ui.common.localSharedBounds
import com.huanchengfly.tieba.post.ui.common.theme.compose.clickableNoIndication
import com.huanchengfly.tieba.post.ui.common.theme.compose.onCase
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.models.LikedForum
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.MainPageTabs
import com.huanchengfly.tieba.post.ui.page.main.MainCardStyle
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.ui.page.main.OnMainNavigationScrollTopEvent
import com.huanchengfly.tieba.post.ui.page.main.PlanetNavigationIcon
import com.huanchengfly.tieba.post.ui.page.main.bottomNavigationPlaceholder
import com.huanchengfly.tieba.post.ui.page.main.calculateMainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ForumAvatarShape
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshBox
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.page.main.mainTopBarDividers
import com.huanchengfly.tieba.post.ui.widgets.compose.color
import com.huanchengfly.tieba.post.ui.widgets.compose.placeholder
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
import dev.chrisbanes.haze.hazeSource
import kotlin.random.Random

private val FORUM_AVATAR_SIZE = 40.dp

// Card insets must not make the two-column mode fall back to one column.
internal fun homeForumGridCells(singleColumn: Boolean): GridCells =
    GridCells.Fixed(if (singleColumn) 1 else 2)

@Composable
private fun HomeForumActions(
    isSigning: Boolean,
    onSign: () -> Unit,
    onListModeChanged: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onSign,
            enabled = !isSigning,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_oksign),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.title_oksign), maxLines = 1)
        }
        IconButton(onClick = onListModeChanged) {
            Icon(
                imageVector = Icons.Outlined.ViewAgenda,
                contentDescription = stringResource(R.string.title_home_list_style),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// Keep the relocated actions available during loading, empty and error states too.
@Composable
private fun HomeStateContent(
    showForumHeader: Boolean,
    actions: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().onCase(showForumHeader && MainCardStyle.enabled) {
        padding(horizontal = MainCardStyle.horizontalSpacing, vertical = MainCardStyle.verticalSpacing)
            .clip(MainCardStyle.shape)
            .background(MainCardStyle.container)
    }) {
        if (showForumHeader) FollowedForumsHeader(actions = actions)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun Header(
    text: String,
    modifier: Modifier = Modifier,
    invert: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    icon: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier) {
        if (invert) {
            Chip(text = text, modifier = Modifier.padding(start = 16.dp), invertColor = true)
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) { icon() }
                    }
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FollowedForumsHeader(
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Header(
            text = stringResource(R.string.forum_list_title),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            maxLines = 1,
            icon = {
                // A constant unselected state reuses the existing silhouette without animating.
                PlanetNavigationIcon(selected = false, description = null, modifier = Modifier.fillMaxSize())
            },
        )
        actions()
    }
}

@Composable
private fun ForumItemPlaceholder(showAvatar: Boolean) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val placeholderColor = PlaceholderDefaults.color()

        if (showAvatar) {
            Box(
                modifier = Modifier
                    .size(FORUM_AVATAR_SIZE)
                    .placeholder(color = placeholderColor, shape = ForumAvatarShape),
            )
            Spacer(modifier = Modifier.width(14.dp))
        }

        Text(
            text = "",
            modifier = Modifier
                .weight(1.0f)
                .placeholder(color = placeholderColor),
            fontSize = 15.sp,
        )

        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .padding(vertical = 4.dp)
                .placeholder(color = placeholderColor)
        ) {
            Text(text = "0", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HistoryItem(
    modifier: Modifier = Modifier,
    title: String,
    avatar: @Composable RowScope.() -> Unit,
    color: Color,
    contentColor: Color,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clip(shape = CircleShape)
            .background(color = color)
            .clickable(onClick = onClick)
            .padding(start = 4.dp, top = 4.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        avatar()
        Text(text = title, color = contentColor, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun HomeFootprintItem(history: History, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ForumAvatar(
            data = history.avatar,
            size = Sizes.Medium,
        )
        Text(
            text = history.name,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HistoryRow(modifier: Modifier = Modifier, history: List<History>, onClick: (History) -> Unit) {
    var expandHistoryForum by rememberSaveable { mutableStateOf(true) }

    val degrees by animateFloatAsState(
        targetValue = if (expandHistoryForum) 90f else 0f,
        label = "ExpandRotateAnim"
    )
    Column(modifier = modifier.onCase(MainCardStyle.enabled) {
        padding(vertical = MainCardStyle.verticalSpacing)
            .clip(MainCardStyle.shape)
            .background(MainCardStyle.container)
    }) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickableNoIndication { expandHistoryForum = !expandHistoryForum }
                .padding(vertical = 12.dp)
                .padding(end = 16.dp)
        ) {
            Header(
                text = stringResource(id = R.string.title_home_footprints),
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.fillMaxSize())
                },
            )

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = stringResource(id = R.string.desc_show),
                modifier = Modifier.graphicsLayer {
                    rotationZ = degrees
                }
            )
        }

        AnimatedVisibility(visible = expandHistoryForum) {
            LazyRow(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = history, key = { it.id }) {
                    HomeFootprintItem(
                        history = it,
                        onClick = { onClick(it) }
                    )
                }
            }
        }
    }
}

@NonRestartableComposable
@Composable
private fun ForumItemContent(forum: LikedForum, showAvatar: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showAvatar) {
            ForumAvatar(
                data = forum.avatar,
                modifier = Modifier
                    .padding(end = 14.dp)
                    .size(FORUM_AVATAR_SIZE)
                    .localSharedBounds(key = ForumAvatarSharedBoundsKey(forum.name, null)),
            )
        }

        Box(modifier = Modifier.weight(1.0f)) {  // Boxing for transition animation
            Text(
                text = forum.name,
                modifier = Modifier.onCase(showAvatar) { // Enable transition on List Mode (showAvatar)
                    localSharedBounds(key = ForumTitleSharedBoundsKey(forum.name, null))
                },
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall
            )
        }

        Surface(
            modifier = Modifier.width(54.dp),
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.secondary,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = forum.level, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                if (forum.signed) {
                    Spacer(modifier = Modifier.width(4.dp))

                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(id = R.string.tip_signed),
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

private fun LazyGridScope.forumItems(
    forums: LazyPagingItems<LikedForum>,
    isTopPinnedForum: Boolean,
    showAvatar: Boolean,
    onClick: (forum: LikedForum) -> Unit,
    onUnfollow: (forum: LikedForum) -> Unit,
    onPinnedForumChanged: (forum: LikedForum, isTop: Boolean) -> Unit,
) {
    val contentType: (index: Int) -> Any? = forums.itemContentType {
        if (showAvatar) ForumType.ListItem else ForumType.GridItem
    }

    items(count = forums.itemCount, key = forums.itemKey { it.id }, contentType = contentType) {
        val context = LocalContext.current
        val forum = forums[it]
        if (forum != null) {
            LongClickMenu(
                menuContent = {
                    TextMenuItem(text = if (isTopPinnedForum) R.string.menu_top_del else R.string.menu_top) {
                        onPinnedForumChanged(forum, !isTopPinnedForum)
                    }
                    TextMenuItem(text = R.string.title_copy_forum_name) {
                        TiebaUtil.copyText(context, forum.name)
                    }
                    TextMenuItem(text = R.string.button_unfollow) {
                        onUnfollow(forum)
                    }
                },
                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                onClick = { onClick(forum) }
            ) {
                ForumItemContent(forum, showAvatar)
            }
        } else {
            ForumItemPlaceholder(showAvatar)
        }
    }
}

private val DefaultGridSpan: LazyGridItemSpanScope.() -> GridItemSpan = {
    GridItemSpan(maxLineSpan)
}

private sealed interface ForumType {
    object Header: ForumType
    object History: ForumType
    object Footer: ForumType
    object ListItem: ForumType
    object GridItem: ForumType
}

// Note: Obtain Root AnimatedVisibilityScope by LocalAnimatedVisibilityScope.current
@Composable
fun AnimatedVisibilityScope.HomePage(
    viewModel: HomeViewModel = hiltViewModel<HomeViewModel>(),
    onOpenExplore: () -> Unit = {},
) {
    val loggedIn = LocalAccount.current != null
    val hazeState = LocalHazeState.current
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val context = LocalContext.current
    val navigator = LocalNavController.current
    val gridState = rememberLazyGridState()
    val pageTitles = remember { listOf(R.string.title_home_my_forums, R.string.title_home_forum_square) }
    val pagerState = rememberPagerState { pageTitles.size }
    val forumActions: @Composable () -> Unit = {
        if (loggedIn) {
            val isSigning by viewModel.isOkSignWorkerRunning.collectAsStateWithLifecycle(true)
            HomeForumActions(
                isSigning = isSigning,
                onSign = { TiebaUtil.startSign(context) },
                onListModeChanged = viewModel::onListModeChanged,
            )
        }
    }

    var unfollowForum by remember { mutableStateOf<LikedForum?>(null) }
    val confirmUnfollowDialog = rememberDialogState()
    unfollowForum?.let {
        ConfirmDialog(
            dialogState = confirmUnfollowDialog,
            onConfirm = {
                viewModel.onDislikeForum(forum = it)
            },
            onDismiss = { unfollowForum = null },
            title = { Text(text = stringResource(R.string.button_unfollow)) }
        ) {
            Text(text = stringResource(R.string.title_dialog_unfollow_forum, it.name))
        }
    }

    MyScaffold(
        useMD2Layout = true,
        topBar = {
            val toolbarColor = MaterialTheme.colorScheme.surface.copy(alpha = 1f)
            CenterAlignedTopAppBar(
                expandedHeight = 56.dp,
                modifier = Modifier
                    .onNotNull(LocalAnimatedVisibilityScope.current, sharedTransitionScope) { (rootScope, sharedScope) ->
                        animateEnterExit(
                            zIndexInOverlay = 1.0f,
                            animatedVisibilityScope = rootScope,
                            sharedTransitionScope = sharedScope,
                        )
                    }
                    .mainTopBarDividers(),
                title = { MainPageTabs(pagerState = pagerState, titles = pageTitles) },
                navigationIcon = {
                    AccountNavIconIfCompact(onLoginClicked = { navigator.navigate(Destination.Login) })
                },
                actions = {
                    ActionItem(
                        icon = Icons.Rounded.Search,
                        contentDescription = R.string.title_search,
                        onClick = { navigator.navigateDebounced(route = Destination.Search) },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = toolbarColor,
                    scrolledContainerColor = toolbarColor,
                ),
            )
        },
        bottomBar = bottomNavigationPlaceholder, // MainPage BottomNavBar placeholder
        bottomBarAtop = calculateMainNavigationSuiteType().isFloatingNavigationBar,
    ) { contentPaddings ->
        val coroutineScope = rememberCoroutineScope()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val forums = viewModel.forums.collectAsLazyPagingItems()
        val pinnedForums = viewModel.pinnedForums.collectAsLazyPagingItems()
        val historyForums by viewModel.historyFlow.collectAsStateWithLifecycle()

        val isEmpty = pinnedForums.itemCount == 0 && forums.itemCount == 0
        val isPinnedNotEmpty = pinnedForums.itemCount > 0
        val isLoading = uiState.isLoading || historyForums == null
        val useCards = MainCardStyle.enabled
        val cardInset = if (useCards) MainCardStyle.horizontalSpacing else 0.dp
        val gridPadding = contentPaddings + PaddingValues(horizontal = cardInset)

        val listSingle = LocalUISettings.current.homeForumList
        val gridCells = remember(listSingle) {
            homeForumGridCells(singleColumn = listSingle)
        }

        // Initialize click listeners now
        val onForumClickedListener: (LikedForum) -> Unit = {
            navigator.navigateDebounced(route = Destination.Forum(forumName = it.name, avatar = it.avatar))
        }
        val onHistoryClickedListener: (History) -> Unit = {
            navigator.navigateDebounced(route = Destination.Forum(forumName = it.name))
        }

        val onUnfollow: (LikedForum) -> Unit = {
            unfollowForum = it
            confirmUnfollowDialog.show()
        }

        OnMainNavigationScrollTopEvent<MainDestination.Home>(
            coroutineScope = coroutineScope,
            gridState = gridState.takeIf { pagerState.currentPage == 0 },
            listState = { null }
        )

        HorizontalPager(
            state = pagerState,
            key = { pageTitles[it] },
            modifier = Modifier.fillMaxSize().onCase(useCards) { background(MainCardStyle.background) },
            // Keep My Forums' remembered UI state while the square is selected.
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
            flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f),
        ) { page ->
            if (page == 1) {
                ForumSquarePage(contentPadding = contentPaddings, isActive = pagerState.settledPage == 1)
                return@HorizontalPager
            }
            StateScreen(
                modifier = Modifier.onCase(useCards) { background(MainCardStyle.background) },
                isEmpty = isEmpty,
                isError = uiState.error != null,
                isLoading = uiState.isLoading,
                onReload = viewModel::onRefresh.takeIf { loggedIn },
                emptyScreen = {
                    HomeStateContent(showForumHeader = loggedIn, actions = forumActions) {
                        EmptyScreen(onExploreClicked = onOpenExplore)
                    }
                },
                loadingScreen = {
                    HomePageSkeletonScreen(listSingle = listSingle, gridCells = gridCells, actions = forumActions)
                },
                errorScreen = {
                    HomeStateContent(showForumHeader = loggedIn, actions = forumActions) {
                        if (uiState.error is TiebaNotLoggedInException) {
                            GuestScreen(onExploreClicked = onOpenExplore) {
                                navigator.navigateDebounced(Destination.Login)
                            }
                        } else  {
                            ErrorScreen(error = uiState.error)
                        }
                    }
                },
                screenPadding = contentPaddings
            ) {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = viewModel::onRefresh,
                    contentPadding = contentPaddings
                ) {
                    // Home handles scroll-to-top above, only for the currently selected page.
                    LazyVerticalGrid(
                        columns = gridCells,
                        modifier = Modifier
                            .fillMaxSize()
                            .onNotNull(hazeState) { hazeSource(state = it.state) }
                            .followedForumsCard(
                                state = gridState,
                                firstSectionIndex = if (!historyForums.isNullOrEmpty()) 1 else 0,
                                enabled = useCards,
                                contentPadding = gridPadding,
                            ),
                        state = gridState,
                        contentPadding = gridPadding,
                    ) {
                        historyForums?.takeUnless { it.isEmpty() }?.let {
                            item(key = ForumType.History.hashCode(), DefaultGridSpan, { ForumType.History }) {
                                HistoryRow(history = it, onClick = onHistoryClickedListener)
                            }
                        }

                        item(key = FollowedForumsHeaderKey, DefaultGridSpan, { ForumType.Header }) {
                            FollowedForumsHeader(
                                actions = forumActions,
                            )
                        }
                        if (isPinnedNotEmpty) {
                            item(key = R.string.title_top_forum, DefaultGridSpan, { ForumType.Header }) {
                                Header(
                                    text = stringResource(id = R.string.title_top_forum),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    invert = true
                                )
                            }
                            forumItems(
                                forums = pinnedForums,
                                isTopPinnedForum = true,
                                showAvatar = listSingle,
                                onClick = onForumClickedListener,
                                onUnfollow = onUnfollow,
                                onPinnedForumChanged = viewModel::onPinnedForumChanged,
                            )
                            if (forums.itemCount > 0) {
                                item(key = "home-pinned-divider", DefaultGridSpan, { ForumType.Header }) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        }

                        forumItems(
                            forums = forums,
                            isTopPinnedForum = false,
                            showAvatar = listSingle,
                            onClick = onForumClickedListener,
                            onUnfollow = onUnfollow,
                            onPinnedForumChanged = viewModel::onPinnedForumChanged,
                        )
                        item(key = FollowedForumsFooterKey, DefaultGridSpan, { ForumType.Footer }) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
        ReportDrawnWhen { pagerState.currentPage == 1 || !uiState.isLoading }
    }
}

@NonRestartableComposable
@Composable
private fun ExploreButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    PositiveButton(
        textRes = R.string.button_go_to_explore,
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(),
        onClick = onClick,
    )
}

@Composable
private fun HomePageSkeletonScreen(
    modifier: Modifier = Modifier,
    listSingle: Boolean,
    gridCells: GridCells,
    actions: @Composable () -> Unit = {},
) {
    val state = rememberLazyGridState()
    val useCards = MainCardStyle.enabled
    LazyVerticalGrid(
        columns = gridCells,
        modifier = modifier.followedForumsCard(state, firstSectionIndex = 0, enabled = useCards),
        state = state,
        contentPadding = PaddingValues(horizontal = if (useCards) MainCardStyle.horizontalSpacing else 0.dp),
        userScrollEnabled = false
    ) {
        item(key = FollowedForumsHeaderKey, DefaultGridSpan, { ForumType.Header }) {
            FollowedForumsHeader(
                actions = actions,
            )
        }
        items(24, key = { it }) {
            ForumItemPlaceholder(listSingle)
        }
        item(key = FollowedForumsFooterKey, DefaultGridSpan, { ForumType.Footer }) {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GuestScreen(
    modifier: Modifier = Modifier,
    onExploreClicked: () -> Unit,
    onLoginClicked: () -> Unit
) {
    TipScreen(
        title = {
            Text(text = stringResource(id = R.string.title_not_logged_in))
        },
        modifier = modifier,
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_astronaut))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        message = {
            Text(text = stringResource(R.string.home_empty_login), textAlign = TextAlign.Center)
        },
        actions = {
            PositiveButton(R.string.button_login, Modifier.fillMaxWidth(), onClick = onLoginClicked)

            ExploreButton(modifier = Modifier.fillMaxWidth(), onClick = onExploreClicked)
        }
    )
}

@Composable
private fun EmptyScreen(modifier: Modifier = Modifier, onExploreClicked: () -> Unit) {
    TipScreen(
        title = {
            Text(text = stringResource(id = R.string.title_empty))
        },
        modifier = modifier,
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_astronaut))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        actions = { ExploreButton(modifier = Modifier.fillMaxWidth(), onClick = onExploreClicked) },
    )
}

@Preview("HomePageSkeletonScreen")
@Composable
private fun HomePageSkeletonScreenPreview() = TiebaLiteTheme {
    Surface {
        HomePageSkeletonScreen(listSingle = true, gridCells = GridCells.Fixed(1))
    }
}

@Preview("ForumItemContent")
@Composable
private fun ForumItemContentPreview() = TiebaLiteTheme {
    val forums = (0..15).map { i ->
        LikedForum(id = i.toLong(), name = "Forum $i", level = "Lv.${Random.nextInt(1, 99)}")
    }
    Surface {
        Column {
            forums.forEach {
                ForumItemContent(it, showAvatar = false)
            }
        }
    }
}
