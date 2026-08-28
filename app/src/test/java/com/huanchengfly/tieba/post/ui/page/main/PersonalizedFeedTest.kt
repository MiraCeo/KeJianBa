package com.huanchengfly.tieba.post.ui.page.main

import androidx.lifecycle.ViewModelStore
import com.huanchengfly.tieba.post.models.database.LocalLikedForum
import com.huanchengfly.tieba.post.repository.BlockRepository
import com.huanchengfly.tieba.post.repository.ExploreRepository
import com.huanchengfly.tieba.post.repository.FollowedForumSnapshot
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.repository.source.local.personalizedCachePrefix
import com.huanchengfly.tieba.post.repository.source.local.personalizedAccountCachePrefix
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.settings.BlockSettings
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.FollowedForumFilter
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedEmptyReason
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.PersonalizedViewModel
import com.huanchengfly.tieba.post.ui.page.main.explore.personalized.loadFollowedBatch
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalizedFeedTest {
    private fun forum(id: Long, uid: Long = 1, name: String = "吧$id") =
        LocalLikedForum(id = id, uid = uid, name = name, level = 1, signInTimestamp = -1)

    private fun thread(id: Long, forumId: Long = 10, name: String = "吧$forumId") = ThreadItem(
        id = id, author = Author(100, "作者", ""), title = "帖子$id", lastTimeMill = 0,
        simpleForum = Triple(forumId, name, null),
    )

    @Test
    fun membershipIsAccountScopedAndPrefersIdsOverNames() {
        val filter = FollowedForumFilter.from(1, listOf(forum(10), forum(20, uid = 2), forum(0, name = "无ID")))
        assertTrue(filter.includes(thread(1, 10, "改名")))
        assertFalse(filter.includes(thread(2, 20)))
        assertFalse(filter.includes(thread(3, 99, "吧10")))
        assertTrue(filter.includes(thread(4, 0, "吧10")))
        assertTrue(filter.includes(thread(5, 42, "无ID")))
        assertFalse(filter.includes(thread(6, 0, "吧1")))
        assertTrue(FollowedForumFilter.from(-1, listOf(forum(10))).isEmpty)
    }

    @Test
    fun fullSnapshotIncludesForumsBeyondTheFirstUiPage() {
        val filter = FollowedForumFilter.from(1, (1L..100L).map { forum(it) })
        assertTrue(filter.includes(thread(1, 100)))
    }

    @Test
    fun skipsNonmatchingAndDuplicatePagesWithoutReordering() = runTest {
        val calls = mutableListOf<Int>()
        val batch = loadFollowedBatch(2, setOf(7L), accepts = { it.simpleForum.first == 10L }) { page ->
            calls += page
            when (page) {
                2 -> listOf(thread(1, 20))
                3 -> listOf(thread(7))
                else -> listOf(thread(9), thread(8), thread(9), thread(10, 20))
            }
        }
        assertEquals(listOf(2, 3, 4), calls)
        assertEquals(listOf(9L, 8L), batch.threads.map { it.id })
        assertEquals(4, batch.lastPage)
        assertFalse(batch.manualContinuation)
    }

    @Test
    fun threeEmptyFilteredPagesPauseButKeepTheNextCursor() = runTest {
        val calls = mutableListOf<Int>()
        val batch = loadFollowedBatch(1, emptySet(), accepts = { false }) {
            calls += it
            listOf(thread(it.toLong()))
        }
        assertEquals(listOf(1, 2, 3), calls)
        assertTrue(batch.manualContinuation)
        assertEquals(3, batch.lastPage)
        val next = loadFollowedBatch(batch.lastPage + 1, emptySet(), accepts = { true }) {
            assertEquals(4, it)
            listOf(thread(4))
        }
        assertEquals(listOf(4L), next.threads.map { it.id })
    }

    @Test
    fun emptyServerResponseDoesNotTriggerUnboundedRequests() = runTest {
        var calls = 0
        val batch = loadFollowedBatch(8, emptySet(), accepts = { true }) {
            calls++
            emptyList()
        }
        assertEquals(1, calls)
        assertEquals(8, batch.lastPage)
        assertTrue(batch.manualContinuation)
    }

    @Test
    fun cachePrefixesSeparateBothAccountsAndModes() {
        val followed = personalizedCachePrefix("1_followed")
        assertFalse(personalizedCachePrefix("1_discover").startsWith(followed))
        assertFalse(personalizedCachePrefix("10_followed").startsWith(followed))
        assertTrue(personalizedCachePrefix("1_discover").startsWith(personalizedAccountCachePrefix(1)))
        assertFalse(personalizedCachePrefix("10_discover").startsWith(personalizedAccountCachePrefix(1)))
        assertThrows(IllegalArgumentException::class.java) { personalizedCachePrefix("../other") }
    }

    private class TestSettings<T>(val state: MutableStateFlow<T>) : Settings<T>(state) {
        override fun set(new: T) { state.value = new }
        override fun save(transform: (T) -> T) { state.value = transform(state.value) }
    }

    @Test
    fun discoveryStaysUnfilteredAndUnfollowRemovesOnlyRecommendationItems() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val account = TestSettings(MutableStateFlow(1L))
            val settings = mockk<SettingsRepository>()
            every { settings.accountUid } returns account
            every { settings.blockSettings } returns TestSettings(MutableStateFlow(BlockSettings()))
            val forums = MutableStateFlow(listOf(forum(10), forum(30)))
            val home = mockk<HomeRepository>()
            every { home.observeFollowedForums(1) } returns forums
            coEvery { home.followedForumsForFeed(1, any()) } answers { FollowedForumSnapshot(forums.value, false) }
            val explore = mockk<ExploreRepository>()
            coEvery { explore.loadPersonalized(any(), any(), any(), any()) } returns listOf(thread(1, 10), thread(2, 20))
            val block = mockk<BlockRepository>()
            val followed = PersonalizedViewModel(true, 1, explore, block, home, settings)
            val discover = PersonalizedViewModel(false, 1, explore, block, home, settings)
            store.put("followed", followed)
            store.put("discover", discover)
            advanceUntilIdle()
            followed.uiState.first { !it.isRefreshing }
            discover.uiState.first { !it.isRefreshing }
            assertEquals(listOf(1L), followed.currentState.data.map { it.id })
            assertEquals(listOf(1L, 2L), discover.currentState.data.map { it.id })
            coVerify { explore.loadPersonalized(1, true, "1_followed", 1L) }
            coVerify { explore.loadPersonalized(1, true, "1_discover", 1L) }
            forums.value = listOf(forum(30))
            advanceUntilIdle()
            assertTrue(followed.currentState.data.isEmpty())
            assertEquals(2, discover.currentState.data.size)
            account.set(2)
            advanceUntilIdle()
            assertTrue(followed.currentState.data.isEmpty())
            assertTrue(discover.currentState.data.isEmpty())
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun noFollowedForumsDoesNotFetchRecommendationPages() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val settings = mockk<SettingsRepository>()
            every { settings.accountUid } returns TestSettings(MutableStateFlow(1L))
            every { settings.blockSettings } returns TestSettings(MutableStateFlow(BlockSettings()))
            val home = mockk<HomeRepository>()
            every { home.observeFollowedForums(1) } returns MutableStateFlow(emptyList())
            coEvery { home.followedForumsForFeed(1, any()) } returns FollowedForumSnapshot(emptyList(), false)
            val explore = mockk<ExploreRepository>()
            val vm = PersonalizedViewModel(true, 1, explore, mockk(), home, settings)
            store.put("vm", vm)
            advanceUntilIdle()
            assertEquals(PersonalizedEmptyReason.NoForums, vm.currentState.emptyReason)
            assertFalse(vm.currentState.isRefreshing)
            coVerify(exactly = 0) { explore.loadPersonalized(any(), any(), any(), any()) }
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun emptyBatchCanContinueAndLateResponsesRespectUnfollow() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val settings = mockk<SettingsRepository>()
            every { settings.accountUid } returns TestSettings(MutableStateFlow(1L))
            every { settings.blockSettings } returns TestSettings(MutableStateFlow(BlockSettings()))
            val forums = MutableStateFlow(listOf(forum(10), forum(30)))
            val home = mockk<HomeRepository>()
            every { home.observeFollowedForums(1) } returns forums
            coEvery { home.followedForumsForFeed(1, any()) } answers { FollowedForumSnapshot(forums.value, false) }
            val explore = mockk<ExploreRepository>()
            val latePage = CompletableDeferred<List<ThreadItem>>()
            coEvery { explore.loadPersonalized(any(), any(), "1_followed", 1) } coAnswers {
                if (firstArg<Int>() <= 3) listOf(thread(firstArg<Int>().toLong(), 20)) else latePage.await()
            }
            val vm = PersonalizedViewModel(true, 1, explore, mockk(), home, settings)
            store.put("vm", vm)
            advanceUntilIdle()
            vm.uiState.first { !it.isRefreshing }
            assertEquals(3, vm.currentState.currentPage)
            assertEquals(PersonalizedEmptyReason.NoMatches, vm.currentState.emptyReason)
            assertTrue(vm.currentState.manualContinuation)
            vm.onLoadMore()
            advanceUntilIdle()
            forums.value = listOf(forum(30))
            advanceUntilIdle()
            latePage.complete(listOf(thread(4, 10), thread(5, 30)))
            advanceUntilIdle()
            vm.uiState.first { !it.isLoadingMore }
            assertEquals(listOf(5L), vm.currentState.data.map { it.id })
            assertEquals(4, vm.currentState.currentPage)
            assertFalse(vm.currentState.manualContinuation)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun guestSeesLoginStateWithoutRequestingForumsOrRecommendations() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val settings = mockk<SettingsRepository>()
            every { settings.accountUid } returns TestSettings(MutableStateFlow(-1L))
            every { settings.blockSettings } returns TestSettings(MutableStateFlow(BlockSettings()))
            val home = mockk<HomeRepository>()
            val explore = mockk<ExploreRepository>()
            val vm = PersonalizedViewModel(true, -1, explore, mockk(), home, settings)
            store.put("vm", vm)
            advanceUntilIdle()
            assertEquals(PersonalizedEmptyReason.LoginRequired, vm.currentState.emptyReason)
            coVerify(exactly = 0) { home.followedForumsForFeed(any(), any()) }
            coVerify(exactly = 0) { explore.loadPersonalized(any(), any(), any(), any()) }
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }
}
