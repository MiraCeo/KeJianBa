package com.huanchengfly.tieba.post.ui.page.main

import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.page.main.explore.ExploreTab
import com.huanchengfly.tieba.post.ui.page.main.explore.defaultExploreTab
import com.huanchengfly.tieba.post.ui.page.main.explore.explorePages
import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationTest {
    @Test
    fun loggedInNavigationSeparatesFeedsAndForumsInFiveTabOrder() {
        assertEquals(
            listOf(MainDestination.Feed, MainDestination.Explore, MainDestination.Home,
                MainDestination.Notification, MainDestination.User),
            mainDestinations(loggedIn = true, hideExplore = false),
        )
    }

    @Test
    fun hidingDynamicsKeepsHomepageAndForumsAccessible() {
        assertEquals(
            listOf(MainDestination.Feed, MainDestination.Home, MainDestination.Notification, MainDestination.User),
            mainDestinations(loggedIn = true, hideExplore = true),
        )
    }

    @Test
    fun guestNavigationPreservesExistingMessageVisibilityRule() {
        assertEquals(
            listOf(MainDestination.Feed, MainDestination.Explore, MainDestination.Home, MainDestination.User),
            mainDestinations(loggedIn = false, hideExplore = false),
        )
    }

    @Test
    fun recommendationAndFollowingFeedsHaveSeparateDestinations() {
        assertEquals(
            listOf(ExploreTab.Hot, ExploreTab.Personalized, ExploreTab.Discover),
            explorePages(MainDestination.Feed),
        )
        assertEquals(
            listOf(ExploreTab.MyPosts, ExploreTab.Concern, ExploreTab.MyCollections),
            explorePages(MainDestination.Explore),
        )
    }

    @Test
    fun defaultPagesStillOpenTheExistingFeedsAfterAddingPlaceholders() {
        for ((destination, feed) in listOf(
            MainDestination.Feed to ExplorePageItem.Personalized,
            MainDestination.Explore to ExplorePageItem.Concern,
        )) {
            val defaultTab = defaultExploreTab(destination)
            assertEquals(1, explorePages(destination).indexOf(defaultTab))
            assertEquals(feed, defaultTab.feed)
        }
    }

    @Test
    fun personalContentTabsDoNotUseFeedSources() {
        assertEquals(
            listOf(ExploreTab.MyPosts, ExploreTab.MyCollections),
            ExploreTab.entries.filter { it.feed == null },
        )
    }

    @Test
    fun discoveryReusesTheOriginalPersonalizedSource() {
        assertEquals(ExplorePageItem.Personalized, ExploreTab.Discover.feed)
    }
}
