package com.huanchengfly.tieba.post.ui.page.main.explore

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.isScrolling
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.navigateDebounced
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.common.animateEnterExit
import com.huanchengfly.tieba.post.ui.common.theme.compose.onNotNull
import com.huanchengfly.tieba.post.ui.common.theme.compose.withNonNull
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.Destination.HotTopicList
import com.huanchengfly.tieba.post.ui.page.Destination.Search
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.consumeResult
import com.huanchengfly.tieba.post.ui.page.main.MainDestination
import com.huanchengfly.tieba.post.ui.page.main.MainPageTabs
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationSuiteType.Companion.isFloatingNavigationBar
import com.huanchengfly.tieba.post.ui.page.main.OnMainNavigationScrollTopEvent
import com.huanchengfly.tieba.post.ui.page.main.bottomNavigationPlaceholder
import com.huanchengfly.tieba.post.ui.page.main.calculateMainNavigationSuiteType
import com.huanchengfly.tieba.post.ui.page.main.mainTopBarDividers
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernPage
import com.huanchengfly.tieba.post.ui.page.main.explore.ExploreFeedStyle.feedBackground
import com.huanchengfly.tieba.post.ui.page.main.explore.hot.HotPage
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedPage
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResult
import com.huanchengfly.tieba.post.ui.page.thread.ThreadResultKey
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStoreContent
import com.huanchengfly.tieba.post.ui.page.user.thread.UserThreadPage
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostPage
import com.huanchengfly.tieba.post.ui.utils.rememberScrollOrientationConnection
import com.huanchengfly.tieba.post.ui.widgets.compose.AccountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Container
import com.huanchengfly.tieba.post.ui.widgets.compose.CenterAlignedTopAppBar
import com.huanchengfly.tieba.post.ui.widgets.compose.DefaultBackToTopFAB
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TbHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.hazeSource
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberPagerListStates
import com.huanchengfly.tieba.post.utils.BooleanBitSet
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlinx.coroutines.launch

sealed class ExplorePageItem(val title: Int){
    object Concern : ExplorePageItem(R.string.title_concern)

    object Personalized : ExplorePageItem(R.string.title_personalized)

    object Hot : ExplorePageItem(R.string.title_hot)
}

// Navigation tabs are separate from feed sources: blank tabs must not create feed view models.
internal enum class ExploreTab(val title: Int, val feed: ExplorePageItem? = null) {
    Hot(R.string.title_hot, ExplorePageItem.Hot),
    Personalized(R.string.title_personalized, ExplorePageItem.Personalized),
    Discover(R.string.title_home_discover, ExplorePageItem.Personalized),
    MyPosts(R.string.title_my_posts),
    Concern(R.string.title_following_feed, ExplorePageItem.Concern),
    MyCollections(R.string.title_my_collect),
}

internal enum class MyPostsTab(val title: Int) {
    Threads(R.string.title_my_posts_threads),
    Replies(R.string.title_sub_posts_default),
}

internal fun explorePages(destination: MainDestination): List<ExploreTab> = when (destination) {
    MainDestination.Feed -> listOf(ExploreTab.Hot, ExploreTab.Personalized, ExploreTab.Discover)
    MainDestination.Explore -> listOf(ExploreTab.MyPosts, ExploreTab.Concern, ExploreTab.MyCollections)
    else -> error("Not a feed destination: $destination")
}

internal fun defaultExploreTab(destination: MainDestination): ExploreTab = when (destination) {
    MainDestination.Feed -> ExploreTab.Personalized
    MainDestination.Explore -> ExploreTab.Concern
    else -> error("Not a feed destination: $destination")
}

/**
 * Common [ThreadItem] onClick listeners for [ConcernPage], [PersonalizedPage] and [HotPage]
 * */
@Immutable
class ThreadClickListeners(
    val onClicked: (ThreadItem) -> Unit,
    val onReplyClicked: (ThreadItem) -> Unit,
    val onAuthorClicked: (ThreadItem) -> Unit,
    val onForumClicked: (ThreadItem) -> Unit,
    val onNavigateHotTopicList: () -> Unit // Not a thread click listener, place here just for convenience
)

fun createThreadClickListeners(
    onNavigate: (Destination, navOptions: NavOptions?, navigatorExtras: Navigator.Extras?) -> Unit
) = ThreadClickListeners(
    onClicked = { thread ->
        val (forumId, _, _) = thread.simpleForum
        onNavigate(Destination.Thread(threadId = thread.id, forumId), null, null)
    },
    onReplyClicked = { thread ->
        val (forumId, _, _) = thread.simpleForum
        onNavigate(Destination.Thread(threadId = thread.id, forumId, scrollToReply = true), null, null)
    },
    onAuthorClicked = { thread ->
        val route = thread.run {
            Destination.UserProfile(user = author, transitionKey = this.id.toString())
        }
        onNavigate(route, null, null)
    },
    onForumClicked = { thread ->
        val (_, forumName, forumAvatar) = thread.simpleForum
        val extraKey = thread.id.toString()
        onNavigate(Destination.Forum(forumName, forumAvatar, extraKey), null, null)
    },
    onNavigateHotTopicList = {
        onNavigate(HotTopicList, null, null)
    }
)

@Composable
private fun ExplorePageTab(
    pagerState: PagerState,
    pages: List<ExploreTab>,
    compactLabels: Boolean,
) {
    MainPageTabs(pagerState = pagerState, titles = remember(pages) { pages.map { it.title } }, compactLabels = compactLabels)
}

// Note: Obtain Root AnimatedVisibilityScope by LocalAnimatedVisibilityScope.current
@Composable
fun AnimatedVisibilityScope.ExplorePage(destination: MainDestination) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navigator = LocalNavController.current
    val navigationSuiteType = calculateMainNavigationSuiteType()
    // Hide FAB on FloatingNavigationBarCompact
    val isFloatingNavBarCompat = navigationSuiteType === MainNavigationSuiteType.FloatingNavigationBarCompact
    val hazeState = LocalHazeState.current
    val sharedTransitionScope = LocalSharedTransitionScope.current

    val pages = remember(destination) { explorePages(destination) }
    val pagerState = rememberPagerState(
        initialPage = pages.indexOf(defaultExploreTab(destination))
    ) { pages.size }
    val listStates = rememberPagerListStates(pages.size)
    // Keep secondary selection and independent scroll positions outside the outer pager.
    var selectedPostsTab by rememberSaveable { mutableStateOf(MyPostsTab.Threads) }
    val myPostsListStates = rememberPagerListStates(MyPostsTab.entries.size)
    val accountUid = LocalAccount.current?.uid
    fun currentListState(): LazyListState? = when (pages[pagerState.currentPage]) {
        ExploreTab.MyPosts -> myPostsListStates[selectedPostsTab.ordinal].takeIf { accountUid != null }
        ExploreTab.MyCollections -> listStates[pagerState.currentPage].takeIf { accountUid != null }
        else -> listStates.getOrNull(pagerState.currentPage).takeIf { pages[pagerState.currentPage].feed != null }
    }

    val scrollOrientationConnection = rememberScrollOrientationConnection()

    // FAB visibility of each page
    var fabHideStates by remember(pages) { mutableStateOf(BooleanBitSet()) }

    // Like event from explorePages
    onGlobalEvent<ThreadLikeUiEvent>(coroutineScope) {
        context.toastShort(it.toMessage(context))
    }

    if (destination === MainDestination.Feed) {
        OnMainNavigationScrollTopEvent<MainDestination.Feed>(
            coroutineScope = coroutineScope,
            listState = ::currentListState
        )
    } else {
        OnMainNavigationScrollTopEvent<MainDestination.Explore>(
            coroutineScope = coroutineScope,
            listState = ::currentListState
        )
    }

    MyScaffold(
        // Keep the list below the opaque, fixed toolbar, even when other UI uses blur.
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
                title = {
                    ExplorePageTab(pagerState, pages, compactLabels = destination === MainDestination.Explore)
                },
                navigationIcon = {
                    AccountNavIconIfCompact(onLoginClicked = { navigator.navigate(Destination.Login) })
                },
                actions = {
                    ActionItem(
                        icon = Icons.Rounded.Search,
                        contentDescription = R.string.title_search,
                        onClick = { navigator.navigateDebounced(route = Search) }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = toolbarColor,
                    scrolledContainerColor = toolbarColor,
                ),
            )
        },
        bottomBar = bottomNavigationPlaceholder, // MainPage BottomNavBar placeholder
        bottomBarAtop = navigationSuiteType.isFloatingNavigationBar,
        floatingActionButton = {
            if (isFloatingNavBarCompat) return@MyScaffold
            // FAB visibility: scrolling forward, pager !scrolling, list !scrolling, not refreshing
            val visible by remember {
                derivedStateOf {
                    pages[pagerState.currentPage].feed != null &&
                            !transition.isRunning && scrollOrientationConnection.isScrollingForward &&
                            !pagerState.isScrolling && !listStates[pagerState.currentPage].isScrollInProgress &&
                            !fabHideStates[pagerState.currentPage]
                }
            }
            DefaultBackToTopFAB(visible = visible) {
                coroutineScope.emitGlobalEvent(GlobalEvent.ScrollToTop(destination))
            }
        },
        floatingActionButtonPosition = if (isFloatingNavBarCompat) FabPosition.EndOverlay else FabPosition.End,
    ) { contentPadding ->
        Container(
            modifier = Modifier.onNotNull(hazeState) { hazeSource(state = it.state) }
        ) {
            HorizontalPager(
                state = pagerState,
                key = { pages[it].title },
                modifier = Modifier
                    .fillMaxSize()
                    .feedBackground()
                    .nestedScroll(scrollOrientationConnection),
                verticalAlignment = Alignment.Top,
                flingBehavior = PagerDefaults.flingBehavior(pagerState, snapPositionalThreshold = 0.75f)
            ) { index ->
                val onHideFab: (Boolean) -> Unit = { hideFab ->
                    fabHideStates = fabHideStates.set(index, hideFab)
                }
                val listState = listStates[index]

                when (pages[index].feed) {
                    ExplorePageItem.Concern -> {
                        ConcernPage(Modifier, contentPadding, listState, navigator, onHideFab,
                            isActive = pagerState.settledPage == index)
                    }

                    ExplorePageItem.Personalized -> {
                        PersonalizedPage(Modifier, contentPadding, listState, navigator, onHideFab,
                            followedOnly = pages[index] == ExploreTab.Personalized,
                            isActive = pagerState.settledPage == index)
                    }

                    ExplorePageItem.Hot -> {
                        HotPage(Modifier, contentPadding, listState, navigator, onHideFab)
                    }

                    null -> {
                        if (pages[index] == ExploreTab.MyPosts) {
                            MyPostsPage(
                                contentPadding, selectedPostsTab, myPostsListStates,
                                uid = accountUid, isActive = pagerState.settledPage == index,
                                onLogin = { navigator.navigateDebounced(Destination.Login) },
                                onSelect = { selectedPostsTab = it },
                            )
                        } else if (pages[index] == ExploreTab.MyCollections && pagerState.settledPage == index) {
                            ThreadStoreContent(navigator, contentPadding, listState, embedded = true)
                        } else {
                            Box(Modifier.fillMaxSize().padding(contentPadding))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyPostsPage(
    contentPadding: PaddingValues,
    selected: MyPostsTab,
    listStates: List<LazyListState>,
    uid: Long?,
    isActive: Boolean,
    onLogin: () -> Unit,
    onSelect: (MyPostsTab) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        Row(Modifier.padding(horizontal = 16.dp).selectableGroup()) {
            MyPostsTab.entries.forEach { tab ->
                val isSelected = selected == tab
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(tab.title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            if (uid == null) {
                TipScreen(
                    title = { Text(stringResource(R.string.tip_my_posts_login)) },
                    actions = {
                        FilledTonalButton(onClick = onLogin) { Text(stringResource(R.string.button_login)) }
                    },
                )
            } else if (isActive) {
                // Do not request private lists just because the outer pager precomposes this tab.
                when (selected) {
                    MyPostsTab.Threads -> UserThreadPage(uid, lazyListState = listStates[selected.ordinal])
                    MyPostsTab.Replies -> UserPostPage(uid, lazyListState = listStates[selected.ordinal])
                }
            }
        }
    }
}

context(mainAnimatedContentScope: AnimatedVisibilityScope)
fun Modifier.topAppBarBlurEffect(
    sharedTransitionScope: SharedTransitionScope?,
    rootAnimatedVisibilityScope: AnimatedVisibilityScope?,
    hazeState: TbHazeState?,
    blurEnabled: () -> Boolean,
): Modifier = this then Modifier
    .onNotNull(rootAnimatedVisibilityScope, sharedTransitionScope) { (rootAnimatedVisibilityScope, sharedTransitionScope) ->
        animateEnterExit(
            zIndexInOverlay = 1.0f,
            animatedVisibilityScope = rootAnimatedVisibilityScope,
            sharedTransitionScope = sharedTransitionScope
        )
    }
    .withNonNull(hazeState) {
        Modifier.defaultHazeEffect {
            // Disable background blur when MainNavHost transition is running
            this.blurEnabled = !mainAnimatedContentScope.transition.isRunning && blurEnabled()
        }
    }

@Composable
fun LaunchedFabStateEffect(
    listState: LazyListState,
    onHideFab: (Boolean) -> Unit,
    isRefreshing: Boolean,
    isError: Boolean
) {
    val noScrollBackward by remember { derivedStateOf { !listState.canScrollBackward } }

    LaunchedEffect(noScrollBackward, onHideFab, isRefreshing, isError) {
        onHideFab(noScrollBackward || isRefreshing || isError)
    }
}

@Composable
inline fun <reified Route : Any> ConsumeThreadPageResult(
    navigator: NavController,
    crossinline onThreadResult: (threadId: Long, Like) -> Unit
) {
    LaunchedEffect(Unit) {
        navigator.consumeResult<Route, ThreadResult>(ThreadResultKey)?.run {
            onThreadResult(threadId, Like(liked, likes))
        }
    }
}
