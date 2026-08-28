package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.ComposeNavigatorDestinationBuilder
import androidx.navigation.compose.composable
import androidx.navigation.get
import com.huanchengfly.tieba.post.ui.common.LocalAnimatedVisibilityScope
import com.huanchengfly.tieba.post.ui.common.LocalSharedTransitionScope
import com.huanchengfly.tieba.post.ui.page.LocalNavController
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.user.UserPage
import com.huanchengfly.tieba.post.ui.widgets.compose.LocalHazeState
import com.huanchengfly.tieba.post.ui.widgets.compose.TbHazeState
import com.huanchengfly.tieba.post.utils.LocalAccount
import kotlinx.serialization.Serializable
import kotlin.reflect.KType

@Serializable
sealed interface MainDestination {

    @Serializable
    object Feed: MainDestination

    // Keep the original forum route identity for saved navigation state.
    @Serializable
    object Home: MainDestination

    @Serializable
    object Explore: MainDestination

    @Serializable
    object Notification: MainDestination

    @Serializable
    object User: MainDestination
}

internal fun mainDestinations(loggedIn: Boolean, hideExplore: Boolean): List<MainDestination> =
    listOfNotNull(
        MainDestination.Feed,
        MainDestination.Explore.takeUnless { hideExplore },
        MainDestination.Home,
        MainDestination.Notification.takeIf { loggedIn },
        MainDestination.User,
    )

fun NavGraphBuilder.mainNavGraph(
    navController: NavController,
    nestedNavController: NavController,
    hazeState: TbHazeState? = null,
    parentAnimatedVisibilityScope: AnimatedVisibilityScope? = null,
    parentSharedTransitionScope: SharedTransitionScope? = null,
    startDestination: MainDestination = MainDestination.Feed,
) {
    animatedMainComposable<MainDestination.Feed>(
        hazeState = hazeState,
        navController = navController,
        parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
        parentSharedTransitionScope = parentSharedTransitionScope,
    ) {
        val accountUid = LocalAccount.current?.uid
        key(accountUid) {
            ExplorePage(destination = MainDestination.Feed)
        }
    }

    animatedMainComposable<MainDestination.Home>(
        hazeState = hazeState,
        navController = navController,
        parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
        parentSharedTransitionScope = parentSharedTransitionScope,
    ) {
        HomePage(
            onOpenExplore = {
                nestedNavController.navigate(route = MainDestination.Feed) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(startDestination) {
                        saveState = true
                    }
                }
            },
        )
    }

    animatedMainComposable<MainDestination.Explore>(
        hazeState = hazeState,
        navController = navController,
        parentAnimatedVisibilityScope = parentAnimatedVisibilityScope,
        parentSharedTransitionScope = parentSharedTransitionScope,
    ) {
        val accountUid = LocalAccount.current?.uid
        key(accountUid) { // Recreate per account, including switches between logged-in accounts.
            ExplorePage(destination = MainDestination.Explore)
        }
    }

    composable<MainDestination.Notification> { backStackEntry ->
        NotificationsPage(fromHome = true, navigator = navController)
    }

    composable<MainDestination.User> {
        ProvideNavigator(navigator = navController) {
            UserPage()
        }
    }
}

private inline fun <reified T : Any> NavGraphBuilder.animatedMainComposable(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    hazeState: TbHazeState?,
    navController: NavController,
    parentAnimatedVisibilityScope: AnimatedVisibilityScope?,
    parentSharedTransitionScope: SharedTransitionScope?,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    destination(
        ComposeNavigatorDestinationBuilder(
            provider[ComposeNavigator::class],
            T::class,
            typeMap
        ) {
            CompositionLocalProvider(
                LocalHazeState provides hazeState,
                LocalNavController provides navController,
                LocalAnimatedVisibilityScope provides parentAnimatedVisibilityScope,
                LocalSharedTransitionScope provides parentSharedTransitionScope,
            ) {
                content(it)
            }
        }
        .apply {
            deepLinks.forEach { deepLink -> deepLink(deepLink) }
        }
    )
}
