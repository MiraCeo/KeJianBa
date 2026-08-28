package com.huanchengfly.tieba.post.ui.page.main

import android.util.Log
import androidx.lifecycle.ViewModelStore
import com.huanchengfly.tieba.post.repository.ThreadStoreRepository
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.ThreadStore
import com.huanchengfly.tieba.post.ui.page.threadstore.ThreadStoreViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyCollectionsTest {
    private class Account : Settings<Long>(MutableStateFlow(42L)) {
        override fun set(new: Long) { (flow as MutableStateFlow<Long>).value = new }
        override fun save(transform: (Long) -> Long) { set(transform((flow as MutableStateFlow<Long>).value)) }
    }

    private fun item(id: Long) = ThreadStore(id, "收藏$id", "测试吧", false,
        maxPid = id * 10, markPid = id * 10, postNo = 10, count = 1,
        author = Author(42, "作者", ""))

    private fun testVm(block: suspend TestScope.(ThreadStoreViewModel, ThreadStoreRepository, Account) -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val account = Account()
            val settings = mockk<SettingsRepository>()
            every { settings.accountUid } returns account
            val repo = mockk<ThreadStoreRepository>()
            coEvery { repo.load(0, 20, 42) } returns (1L..20L).map(::item)
            val vm = ThreadStoreViewModel(42, repo, settings)
            store.put("collections", vm)
            advanceUntilIdle()
            block(vm, repo, account)
        } finally {
            store.clear()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun pagesStartAtZeroWithoutSkippingAndRetainPartialTail() = testVm { vm, repo, _ ->
        assertEquals(0, vm.currentState.currentPage)
        coEvery { repo.load(1, 20, 42) } returns (21L..40L).map(::item)
        coEvery { repo.load(2, 20, 42) } returns listOf(item(40), item(41))
        vm.onLoadMore()
        vm.onLoadMore()
        advanceUntilIdle()
        assertEquals(1, vm.currentState.currentPage)
        assertEquals(40, vm.currentState.data.size)
        vm.onLoadMore()
        advanceUntilIdle()
        assertEquals((1L..41L).toList(), vm.currentState.data.map { it.id })
        assertFalse(vm.currentState.hasMore)
        vm.onLoadMore()
        advanceUntilIdle()
        coVerify(exactly = 1) { repo.load(0, 20, 42) }
        coVerify(exactly = 1) { repo.load(1, 20, 42) }
        coVerify(exactly = 1) { repo.load(2, 20, 42) }
    }

    @Test
    fun repeatedAndEmptyPagesStopWithoutDuplicatingItems() = testVm { vm, repo, _ ->
        coEvery { repo.load(1, 20, 42) } returns (1L..20L).map(::item)
        vm.onLoadMore()
        advanceUntilIdle()
        assertEquals(20, vm.currentState.data.size)
        assertFalse(vm.currentState.hasMore)
        vm.onRefresh()
        advanceUntilIdle()
        coEvery { repo.load(1, 20, 42) } returns emptyList()
        vm.onLoadMore()
        advanceUntilIdle()
        assertEquals(20, vm.currentState.data.size)
        assertFalse(vm.currentState.hasMore)
    }

    @Test
    fun refreshCancelsOldPaginationAndBlocksConcurrentLoads() = testVm { vm, repo, _ ->
        val oldPage = CompletableDeferred<List<ThreadStore>>()
        val refreshed = CompletableDeferred<List<ThreadStore>>()
        var cancelled = false
        coEvery { repo.load(1, 20, 42) } coAnswers { try { oldPage.await() } finally { cancelled = true } }
        coEvery { repo.load(0, 20, 42) } coAnswers { refreshed.await() }
        vm.onLoadMore()
        advanceUntilIdle()
        vm.onRefresh()
        vm.onRefresh()
        vm.onLoadMore()
        advanceUntilIdle()
        assertTrue(cancelled)
        assertEquals(20, vm.currentState.data.size)
        refreshed.complete(listOf(item(100)))
        oldPage.complete(listOf(item(999)))
        advanceUntilIdle()
        assertEquals(listOf(100L), vm.currentState.data.map { it.id })
        coVerify(exactly = 2) { repo.load(0, 20, 42) }
        coVerify(exactly = 1) { repo.load(1, 20, 42) }
    }

    @Test
    fun accountChangeClearsDataAndRejectsLateResults() = testVm { vm, repo, account ->
        val oldPage = CompletableDeferred<List<ThreadStore>>()
        coEvery { repo.load(1, 20, 42) } coAnswers { oldPage.await() }
        vm.onLoadMore()
        advanceUntilIdle()
        account.set(99)
        advanceUntilIdle()
        oldPage.complete(listOf(item(999)))
        advanceUntilIdle()
        assertTrue(vm.currentState.isEmpty)
        assertFalse(vm.currentState.isLoadingMore)
        account.set(42)
        advanceUntilIdle()
        vm.onRefresh()
        advanceUntilIdle()
        assertEquals(20, vm.currentState.data.size)
    }

    @Test
    fun threadReturnUpdatesMarkAndRemovedItemStaysRemovedDuringPaging() = testVm { vm, repo, _ ->
        val page = CompletableDeferred<List<ThreadStore>>()
        coEvery { repo.load(1, 20, 42) } coAnswers { page.await() }
        vm.onLoadMore()
        advanceUntilIdle()
        vm.onThreadResult(1, 1234)
        vm.onThreadResult(2, null)
        page.complete(listOf(item(1), item(2), item(21)))
        advanceUntilIdle()
        assertEquals(1234L, vm.currentState.data.first { it.id == 1L }.markPid)
        assertFalse(vm.currentState.data.any { it.id == 2L })
        assertTrue(vm.currentState.data.any { it.id == 21L })
    }

    @Test
    fun deleteFailureRestoresOnlyDeletedItemWithoutDiscardingNewPages() = testVm { vm, repo, _ ->
        val removal = CompletableDeferred<Result<Unit>>()
        coEvery { repo.remove(any<ThreadStore>(), 42) } coAnswers { removal.await() }
        coEvery { repo.load(1, 20, 42) } returns listOf(item(21))
        vm.onDelete(item(2))
        vm.onDelete(item(2))
        advanceUntilIdle()
        assertFalse(vm.currentState.data.any { it.id == 2L })
        vm.onLoadMore()
        advanceUntilIdle()
        removal.complete(Result.failure(IllegalStateException("模拟失败")))
        advanceUntilIdle()
        assertEquals((1L..21L).toList(), vm.currentState.data.map { it.id })
        coVerify(exactly = 1) { repo.remove(any<ThreadStore>(), 42) }
    }

    @Test
    fun deleteSuccessRemovesItemAndRefreshCanShowARecollectedItem() = testVm { vm, repo, _ ->
        coEvery { repo.remove(any<ThreadStore>(), 42) } returns Result.success(Unit)
        vm.onDelete(item(2))
        advanceUntilIdle()
        assertFalse(vm.currentState.data.any { it.id == 2L })
        vm.onRefresh()
        advanceUntilIdle()
        assertTrue(vm.currentState.data.any { it.id == 2L })
    }

    @Test
    fun loadErrorReleasesBusyStateAndRetryUsesSameCursor() = testVm { vm, repo, _ ->
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any<Throwable>()) } returns 0
        try {
            coEvery { repo.load(1, 20, 42) } throws IllegalStateException("模拟失败")
            vm.onLoadMore()
            advanceUntilIdle()
            assertNotNull(vm.currentState.error)
            assertFalse(vm.currentState.isLoadingMore)
            assertEquals(0, vm.currentState.currentPage)
            coEvery { repo.load(1, 20, 42) } returns listOf(item(21))
            vm.onLoadMore()
            advanceUntilIdle()
            assertNull(vm.currentState.error)
            assertEquals(21, vm.currentState.data.size)
        } finally { unmockkStatic(Log::class) }
    }
}
