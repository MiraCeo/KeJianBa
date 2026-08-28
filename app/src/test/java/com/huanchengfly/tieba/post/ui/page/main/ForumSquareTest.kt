package com.huanchengfly.tieba.post.ui.page.main

import androidx.lifecycle.ViewModelStore
import com.huanchengfly.tieba.post.api.models.protos.RecommendForumInfo
import com.huanchengfly.tieba.post.api.models.protos.forumSquare.ForumSquareRequest
import com.huanchengfly.tieba.post.api.models.protos.forumSquare.ForumSquareResponse
import com.huanchengfly.tieba.post.repository.ForumSquareRepository
import com.huanchengfly.tieba.post.repository.ForumSquareResult
import com.huanchengfly.tieba.post.repository.ForumSquareRecommendationLoginRequired
import com.huanchengfly.tieba.post.ui.page.main.home.ForumSquareViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ForumSquareTest {
    private data class Call(val category: String, val page: Int, val uid: Long?)

    private fun response(category: String, page: Int, ids: List<Long>, more: Boolean = true) =
        ForumSquareResult(
            categories = listOf("推荐", "热门", "游戏", "搞笑"), category = category,
            forums = ids.map { RecommendForumInfo(forum_id = it, forum_name = "测试$it") },
            serverCurrentPage = page, hasMore = more,
        )

    private fun scenario(
        fetch: suspend (Call) -> ForumSquareResult,
        block: suspend TestScope.(ForumSquareViewModel, MutableList<Call>) -> Unit,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val calls = mutableListOf<Call>()
            val repo = mockk<ForumSquareRepository>()
            coEvery { repo.load(any(), any(), any()) } coAnswers {
                val call = Call(firstArg(), secondArg(), thirdArg())
                calls += call
                fetch(call)
            }
            val vm = ForumSquareViewModel(repo)
            store.put("square", vm)
            block(vm, calls)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test fun firstActivationRequestsRecommendationOnly() = scenario(
        { response(it.category, it.page, listOf(1)) },
    ) { vm, calls ->
        assertEquals("推荐", vm.state.value.category)
        assertEquals("推荐", vm.state.value.categories.first())
        vm.activate(1, false)
        advanceUntilIdle()
        assertTrue(calls.isEmpty())
        vm.activate(1, true)
        advanceUntilIdle()
        assertEquals(listOf(Call("推荐", 1, 1)), calls)
    }

    @Test fun failedFirstRecommendationStillAllowsOtherCategories() = scenario(
        {
            if (it.category == "推荐") throw IOException("offline")
            response(it.category, it.page, listOf(2))
        },
    ) { vm, calls ->
        vm.activate(1, true)
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertTrue("游戏" in vm.state.value.categories)
        vm.selectCategory("游戏")
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals(listOf("推荐", "游戏"), calls.map { it.category })
        assertEquals(listOf(2L), vm.state.value.forums.map { it.forum_id })
    }

    @Test fun guestCanSwitchFromRecommendationLoginPromptToHot() = scenario(
        {
            if (it.category == "推荐") throw ForumSquareRecommendationLoginRequired()
            response(it.category, it.page, listOf(3))
        },
    ) { vm, calls ->
        vm.activate(null, true)
        advanceUntilIdle()
        assertTrue(vm.state.value.error is ForumSquareRecommendationLoginRequired)
        vm.selectCategory("热门")
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertEquals("热门", vm.state.value.category)
        assertEquals(listOf("推荐", "热门"), calls.map { it.category })
    }

    @Test fun accountResetReturnsToRecommendationButReentryKeepsSelection() = scenario(
        { response(it.category, it.page, listOf(1)) },
    ) { vm, calls ->
        vm.activate(1, true)
        advanceUntilIdle()
        vm.selectCategory("游戏")
        advanceUntilIdle()
        vm.activate(1, false)
        vm.activate(1, true)
        advanceUntilIdle()
        assertEquals("游戏", vm.state.value.category)
        assertEquals(2, calls.size)
        vm.activate(2, false)
        assertEquals("推荐", vm.state.value.category)
        assertTrue(vm.state.value.forums.isEmpty())
        vm.activate(2, true)
        advanceUntilIdle()
        assertEquals(Call("推荐", 1, 2), calls.last())
    }

    @Test fun serverCategoriesReplaceBootstrapList() = scenario(
        { response(it.category, it.page, listOf(1)).copy(categories = listOf("推荐", "游戏", "游戏", "")) },
    ) { vm, _ ->
        assertEquals(26, vm.state.value.categories.size)
        vm.activate(1, true)
        advanceUntilIdle()
        assertEquals(listOf("推荐", "游戏"), vm.state.value.categories)
    }

    @Test fun inactivePageDoesNotLoadAndReentryKeepsData() = scenario(
        { response(it.category, it.page, listOf(1)) },
    ) { vm, calls ->
        vm.activate(null, false)
        advanceUntilIdle()
        assertTrue(calls.isEmpty())
        vm.activate(null, true)
        advanceUntilIdle()
        vm.activate(null, false)
        vm.loadMore()
        vm.activate(null, true)
        advanceUntilIdle()
        assertEquals(1, calls.size)
    }

    @Test fun serverHasMoreControlsShortAndLongPagesAndDeduplication() = scenario(
        { if (it.page == 1) response(it.category, 1, listOf(1, 1, 2))
          else response(it.category, it.page, listOf(2L) + (3L..25L), more = false) },
    ) { vm, calls ->
        vm.activate(null, true)
        advanceUntilIdle()
        assertTrue(vm.state.value.hasMore)
        assertEquals(2, vm.state.value.forums.size)
        vm.loadMore()
        vm.loadMore()
        advanceUntilIdle()
        assertEquals((1L..25L).toList(), vm.state.value.forums.map { it.forum_id })
        assertFalse(vm.state.value.hasMore)
        vm.loadMore()
        assertEquals(listOf(1, 2), calls.map { it.page })
    }
    @Test fun categoryChangeStartsAtPageOneAndClearsOldRows() = scenario(
        { response(it.category, it.page, listOf(if (it.category == "推荐") 1 else 2)) },
    ) { vm, calls ->
        vm.activate(null, true)
        advanceUntilIdle()
        vm.selectCategory("游戏")
        assertTrue(vm.state.value.forums.isEmpty())
        advanceUntilIdle()
        assertEquals("游戏", vm.state.value.category)
        assertEquals(listOf(2L), vm.state.value.forums.map { it.forum_id })
        assertEquals(listOf("推荐", "游戏"), calls.map { it.category })
        assertTrue(calls.all { it.page == 1 })
    }

    @Test fun invalidAndRepeatedCategorySelectionsDoNotFetch() = scenario(
        { response(it.category, it.page, listOf(1)) },
    ) { vm, calls ->
        vm.activate(null, true)
        advanceUntilIdle()
        vm.selectCategory("推荐")
        vm.selectCategory("不存在")
        advanceUntilIdle()
        assertEquals(1, calls.size)
    }

    @Test fun duplicateOnlyPagePausesButCanContinueManually() = scenario(
        { response(it.category, it.page, listOf(if (it.page < 3) 1 else 2)) },
    ) { vm, calls ->
        vm.activate(null, true)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()
        assertTrue(vm.state.value.paused)
        assertTrue(vm.state.value.hasMore)
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(2, calls.size)
        vm.loadMore(manual = true)
        advanceUntilIdle()
        assertEquals(listOf(1L, 2L), vm.state.value.forums.map { it.forum_id })
        assertEquals(3, vm.state.value.page)
    }

    @Test fun emptyFirstPagePreservesServerMoreFlag() = scenario(
        { response(it.category, it.page, if (it.page == 1) emptyList() else listOf(2)) },
    ) { vm, calls ->
        vm.activate(null, true)
        advanceUntilIdle()
        assertTrue(vm.state.value.forums.isEmpty())
        assertTrue(vm.state.value.hasMore)
        assertTrue(vm.state.value.paused)
        vm.loadMore()
        assertEquals(1, calls.size)
        vm.loadMore(manual = true)
        advanceUntilIdle()
        assertEquals(2, vm.state.value.page)
    }

    @Test fun pageMismatchIsAnErrorAndDoesNotAdvance() = scenario(
        { response(it.category, 1, listOf(1)) },
    ) { vm, _ ->
        vm.activate(null, true)
        advanceUntilIdle()
        vm.loadMore()
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertEquals(1, vm.state.value.page)
        assertEquals(listOf(1L), vm.state.value.forums.map { it.forum_id })
    }

    @Test fun recommendationWithoutServerPageCanPaginateAndRefresh() = scenario(
        { response(it.category, it.page, listOf(it.page.toLong()), more = it.page < 2)
            .let { result -> if (it.category == "推荐") result.copy(serverCurrentPage = null) else result } },
    ) { vm, calls ->
        vm.activate(1, true)
        advanceUntilIdle()
        vm.selectCategory("推荐")
        advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertTrue(vm.state.value.hasMore)
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(2, vm.state.value.page)
        assertEquals(listOf(1L, 2L), vm.state.value.forums.map { it.forum_id })
        assertFalse(vm.state.value.hasMore)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(1, vm.state.value.page)
        assertEquals(listOf(1L), vm.state.value.forums.map { it.forum_id })
        assertEquals(listOf(1, 2, 1), calls.filter { it.category == "推荐" }.map { it.page })
    }

    @Test fun missingServerPageRemainsAnErrorForRegularCategories() = scenario(
        { response(it.category, it.page, listOf(1)).copy(serverCurrentPage = null) },
    ) { vm, _ ->
        vm.activate(null, true)
        advanceUntilIdle()
        vm.selectCategory("游戏")
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertEquals(0, vm.state.value.page)
    }

    @Test fun recommendationCategoryMismatchDoesNotPublishFallbackRows() = scenario(
        { response("热门", it.page, listOf(1)) },
    ) { vm, _ ->
        vm.activate(1, true)
        advanceUntilIdle()
        vm.selectCategory("推荐")
        advanceUntilIdle()
        assertNotNull(vm.state.value.error)
        assertTrue(vm.state.value.forums.isEmpty())
        assertEquals(0, vm.state.value.page)
    }
    @Test fun failedPageRetriesTheSamePage() {
        var failed = false
        scenario({
            if (it.page == 2 && !failed) {
                failed = true
                throw IOException("offline")
            }
            response(it.category, it.page, listOf(it.page.toLong()))
        }) { vm, calls ->
            vm.activate(null, true)
            advanceUntilIdle()
            vm.loadMore()
            advanceUntilIdle()
            assertEquals(1, vm.state.value.page)
            vm.loadMore()
            assertEquals(2, calls.size)
            vm.loadMore(manual = true)
            advanceUntilIdle()
            assertEquals(listOf(1, 2, 2), calls.map { it.page })
            assertNull(vm.state.value.error)
        }
    }
    @Test fun accountSwitchDiscardsLateResponseEvenIfTransportDoesNotCancel() {
        val gate = CompletableDeferred<Unit>()
        scenario({
            if (it.uid == 1L) withContext(NonCancellable) { gate.await() }
            response(it.category, it.page, listOf(it.uid ?: 0L))
        }) { vm, _ ->
            vm.activate(1, true)
            runCurrent()
            vm.activate(2, true)
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf(2L), vm.state.value.forums.map { it.forum_id })
        }
    }

    @Test fun refreshCancelsOldPagination() {
        val gate = CompletableDeferred<Unit>()
        var firstPages = 0
        scenario({
            if (it.page == 2) withContext(NonCancellable) { gate.await() }
            if (it.page == 1) firstPages++
            response(it.category, it.page, listOf(if (firstPages >= 2) 99L else it.page.toLong()))
        }) { vm, calls ->
            vm.activate(null, true)
            advanceUntilIdle()
            vm.loadMore()
            runCurrent()
            vm.refresh()
            runCurrent()
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf(1, 2, 1), calls.map { it.page })
            assertEquals(1, vm.state.value.page)
            assertEquals(listOf(99L), vm.state.value.forums.map { it.forum_id })
        }
    }

    @Test fun capturedFieldNumbersDecodeWithoutPrivateCaptureFiles() {
        fun hex(value: String) = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        // Synthetic fixtures retain only protocol structure and a public category label.
        val request = ForumSquareRequest.ADAPTER.decode(hex("0a100a001206e6b8b8e6888f180220142800"))
        assertEquals("游戏", request.data_?.category)
        assertEquals(2, request.data_?.page)
        assertEquals(20, request.data_?.page_size)
        assertEquals(0, request.data_?.unknown_flag)
        val response = ForumSquareResponse.ADAPTER.decode(hex("0a06080012001a00120c2206e6b8b8e6888f1a021802"))
        assertEquals(0, response.error?.error_code)
        assertEquals("游戏", response.data_?.category)
        assertEquals(2, response.data_?.page?.current_page)
    }
}
