package com.huanchengfly.tieba.post.ui.page.main

import android.util.Log
import androidx.lifecycle.ViewModelStore
import com.huanchengfly.tieba.post.api.USER_POST_PAGE_SIZE
import com.huanchengfly.tieba.post.api.userPostHasMore
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.user.PostContent
import com.huanchengfly.tieba.post.ui.models.user.PostListItem
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostViewModel
import com.huanchengfly.tieba.post.ui.page.user.thread.UserThreadViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyPostsTest {
    private data class State(
        val ids: List<Long>, val page: Int, val more: Boolean,
        val refreshing: Boolean, val loading: Boolean, val error: Throwable?,
    )

    private class Lists(
        val states: Flow<State>, val refresh: () -> Unit, val loadMore: () -> Unit,
    ) {
        suspend fun idle() = states.first { !it.refreshing && !it.loading }
    }

    // Run the same regressions against both existing personal-profile list implementations.
    private fun bothLists(block: suspend TestScope.(Boolean, ViewModelStore) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            for (threads in listOf(true, false)) {
                val store = ViewModelStore()
                try { block(threads, store) } finally { store.clear() }
            }
        } finally { Dispatchers.resetMain() }
    }

    private fun create(
        threads: Boolean, store: ViewModelStore,
        response: suspend (Int, Boolean) -> List<Long>,
    ): Lists {
        val repo = mockk<UserProfileRepository>()
        val uid = 42L
        val author = Author(uid, "本人", "")
        return if (threads) {
            coEvery { repo.loadUserThread(uid, any(), any()) } coAnswers {
                response(secondArg(), thirdArg()).map {
                    ThreadItem(id = it, author = author, title = "主帖$it", lastTimeMill = 0,
                        simpleForum = Triple(10L, "测试吧", null))
                }
            }
            val vm = UserThreadViewModel(uid, repo)
            store.put("threads", vm)
            Lists(vm.uiState.map {
                State(it.data.map { item -> item.id }, it.currentPage, it.hasMore,
                    it.isRefreshing, it.isLoadingMore, it.error)
            }, vm::onRefresh, vm::onLoadMore)
        } else {
            coEvery { repo.loadUserPost(uid, any(), any()) } coAnswers {
                response(secondArg(), thirdArg()).map {
                    PostListItem(author, listOf(PostContent(it, "回复$it", "刚刚", false)),
                        "原帖", 10L, it, false)
                }
            }
            val vm = UserPostViewModel(uid, repo)
            store.put("replies", vm)
            Lists(vm.uiState.map {
                State(it.data.map { item -> item.threadId }, it.currentPage, it.hasMore,
                    it.isRefreshing, it.isLoadingMore, it.error)
            }, vm::onRefresh, vm::onLoadMore)
        }
    }

    @Test
    fun pageSizeMatchesExistingRequestAndDuplicateOnlyPagesStop() {
        assertEquals(20, USER_POST_PAGE_SIZE)
        assertFalse(userPostHasMore(0))
        assertFalse(userPostHasMore(19))
        assertTrue(userPostHasMore(20))
        assertFalse(userPostHasMore(20, addedNewItems = false))
    }

    @Test
    fun fullFirstPageLoadsNextAndRetainsTheFinalPartialPage() = bothLists { threads, store ->
        val calls = mutableListOf<Pair<Int, Boolean>>()
        val lists = create(threads, store) { page, cached ->
            calls += page to cached
            if (page == 1) (1L..20L).toList() else listOf(20L, 21L, 22L)
        }
        assertTrue(lists.idle().more)
        lists.loadMore()
        lists.loadMore()
        val final = lists.idle()
        assertEquals((1L..22L).toList(), final.ids)
        assertEquals(2, final.page)
        assertFalse(final.more)
        lists.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(1 to true, 2 to true), calls)
    }

    @Test
    fun repeatedPageDoesNotCreateDuplicatesOrKeepLoading() = bothLists { threads, store ->
        val lists = create(threads, store) { _, _ -> (1L..20L).toList() }
        lists.idle()
        lists.loadMore()
        val final = lists.idle()
        assertEquals((1L..20L).toList(), final.ids)
        assertFalse(final.more)
    }

    @Test
    fun emptyNextPageRetainsExistingItems() = bothLists { threads, store ->
        val lists = create(threads, store) { page, _ ->
            if (page == 1) (1L..20L).toList() else emptyList()
        }
        lists.idle()
        lists.loadMore()
        val final = lists.idle()
        assertEquals(20, final.ids.size)
        assertFalse(final.more)
    }

    @Test
    fun refreshCancelsPaginationAndKeepsOldItemsUntilReplacementArrives() = bothLists { threads, store ->
        val latePage = CompletableDeferred<List<Long>>()
        val refreshed = CompletableDeferred<List<Long>>()
        var cancelled = false
        var refreshCalls = 0
        val lists = create(threads, store) { page, cached ->
            when {
                !cached -> { refreshCalls++; refreshed.await() }
                page == 1 -> (1L..20L).toList()
                else -> try { latePage.await() } finally { cancelled = true }
            }
        }
        lists.idle()
        lists.loadMore()
        advanceUntilIdle()
        lists.refresh()
        lists.refresh()
        advanceUntilIdle()
        assertTrue(cancelled)
        assertEquals(1, refreshCalls)
        assertEquals(20, lists.states.first().ids.size)
        refreshed.complete(listOf(100L))
        assertEquals(listOf(100L), lists.idle().ids)
        latePage.complete(listOf(999L))
        advanceUntilIdle()
        assertEquals(listOf(100L), lists.idle().ids)
        assertEquals(1, lists.idle().page)
    }

    @Test
    fun loadFailureClearsBusyStateAndRetriesTheSamePage() = bothLists { threads, store ->
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
        try {
            var attempts = 0
            val lists = create(threads, store) { page, _ ->
                if (page == 1) (1L..20L).toList()
                else {
                    assertEquals(2, page)
                    attempts++
                    if (attempts == 1) error("模拟失败") else listOf(21L)
                }
            }
            lists.idle()
            lists.loadMore()
            val failed = lists.idle()
            assertNotNull(failed.error)
            assertEquals(1, failed.page)
            assertTrue(failed.more)
            lists.loadMore()
            val recovered = lists.idle()
            assertNull(recovered.error)
            assertEquals((1L..21L).toList(), recovered.ids)
            assertEquals(2, attempts)
        } finally { unmockkStatic(Log::class) }
    }
}
