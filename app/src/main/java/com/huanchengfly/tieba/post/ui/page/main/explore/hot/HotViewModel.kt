package com.huanchengfly.tieba.post.ui.page.main.explore.hot

import android.util.SparseArray
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.util.forEach
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.repository.ExploreRepository
import com.huanchengfly.tieba.post.repository.HotTopicRepository
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.HOT_THREAD_TAB_ALL
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.explore.HotTab
import com.huanchengfly.tieba.post.ui.models.explore.HotRankTopic
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankCard
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankItem
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatus
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatusUiStateCommon
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject

private const val TAG = "HotViewModel"

@Immutable
data class HotUiState(
    val isRefreshing: Boolean = false,
    val selectedTab: HotTab,
    val topics: List<HotRankTopic> = emptyList(),
    val materialThreadRanks: List<MaterialThreadRankCard> = emptyList(),
    val tabs: List<HotTab> = emptyList(),
    val threads: List<ThreadItem>? = null, // Loading
    val error: Throwable? = null,
) : UiState {

    fun isTabSelected(tab: HotTab): Boolean = selectedTab.tabCode == tab.tabCode
}

@Stable
@HiltViewModel
class HotViewModel @Inject constructor(
    private val exploreRepo: ExploreRepository,
    private val hotTopicRepo: HotTopicRepository,
) : BaseStateViewModel<HotUiState>() {

    private val defaultTab = HotTab(name = "", tabCode = HOT_THREAD_TAB_ALL, isLoading = false)

    private val memCache = SparseArray<WeakReference<List<ThreadItem>>>()
    private val memCacheMutex = Mutex()

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        _uiState.update { it.copy(isRefreshing = false, error = e) }
    }

    init {
        refreshInternal(cached = false)
    }

    override fun createInitialState(): HotUiState {
        return HotUiState(isRefreshing = true, selectedTab = defaultTab)
    }

    // Save or update In-Memory cache
    private suspend fun updateCache(tab: HotTab, threads: List<ThreadItem>) = memCacheMutex.withLock {
        memCache.set(tab.tabCode.hashCode(), WeakReference(threads))
    }

    // Get from In-Memory cache
    private suspend fun getCached(tab: HotTab): List<ThreadItem>? = memCacheMutex.withLock {
        memCache[tab.tabCode.hashCode()]?.get()
    }

    private suspend fun clearCached() = memCacheMutex.withLock {
        memCache.forEach { k, v -> v.clear() }
        memCache.clear()
    }

    private fun refreshInternal(cached: Boolean): Unit = launchInVM {
        _uiState.update { it.copy(isRefreshing = true, selectedTab = defaultTab, error = null) }
        if (!cached) {
            memCache.clear() // force-refresh, clear in-memory cache
        }
        val (data, materialThreadRanks) = coroutineScope {
            val hotThreadsRequest = async { exploreRepo.loadHotTopic(cached) }
            val materialRanksRequest = async {
                try {
                    hotTopicRepo.loadMaterialThreadRanks()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load material thread ranks", e)
                    null
                }
            }
            hotThreadsRequest.await() to materialRanksRequest.await()
        }
        updateCache(defaultTab, data.threads)
        // defaultTab + tabs
        val tabs = listOf(defaultTab, *data.tabs.toTypedArray())
        defaultTab.isLoading = false
        _uiState.update {
            it.copy(
                isRefreshing = false,
                topics = data.topics,
                materialThreadRanks = materialThreadRanks ?: it.materialThreadRanks,
                tabs = tabs,
                threads = data.threads,
            )
        }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal(cached = false)
    }

    fun onTabSelected(tab: HotTab) {
        // Check/update tab selected, loading state
        if (!currentState.isTabSelected(tab)) _uiState.set { copy(selectedTab = tab, threads = null) } else return
        if (!tab.isLoading) tab.isLoading = true else return

        launchInVM {
            var topics: List<HotRankTopic>? = null
            var threads: List<ThreadItem>? = getCached(tab) // get from memory cache
            try {
                if (threads == null) {
                    val data = exploreRepo.loadHotThreads(tab.tabCode, cached = true)
                    threads = data.threads
                    topics = data.topics
                    updateCache(tab, threads)
                }
            } finally {
                withContext(Dispatchers.Main.immediate) { tab.isLoading = false }
            }

            _uiState.update {
                // Update threads if tab not switched
                threads = if (it.isTabSelected(tab)) threads else it.threads
                it.copy(topics = topics ?: it.topics, threads = threads)
            }
        }
    }

    fun onThreadLikeClicked(thread: ThreadItem) {
        launchInVM {
            val stateSnapshot = currentState
            val selectedTab = stateSnapshot.selectedTab
            val success = updateLikeStatusUiStateCommon(
                thread = thread,
                onRequestLikeThread = { exploreRepo.onLikeThread(it, ExplorePageItem.Hot, selectedTab) },
                onEvent = ::emitGlobalEventSuspend
            ) { threadId, liked, loading ->
                _uiState.update {
                    if (it.isTabSelected(selectedTab) && it.threads != null) {
                        it.copy(threads = it.threads.updateLikeStatus(threadId, liked, loading))
                    } else {
                        it // tab switched, skip UI state update
                    }
                }
            }

            if (success) { // update in-memery cache too
                val cached = getCached(selectedTab)
                if (cached != null) {
                    updateCache(selectedTab, cached.updateLikeStatus(thread.id, !thread.liked, loading = false))
                }
            }
        }
    }

    fun onMaterialThreadLikeClicked(thread: MaterialThreadRankItem) = launchInVM {
        if (thread.likeLoading) {
            emitGlobalEventSuspend(ThreadLikeUiEvent.Connecting)
            return@launchInVM
        }

        val liked = !thread.liked
        val agreeNum = (thread.agreeNum + if (liked) 1L else -1L).coerceAtLeast(0L)
        updateMaterialThreadLike(thread.threadId, liked, agreeNum, loading = true)

        try {
            hotTopicRepo.setMaterialThreadLiked(
                threadId = thread.threadId,
                firstPostId = thread.firstPostId,
                liked = liked,
            )
            updateMaterialThreadLike(thread.threadId, liked, agreeNum, loading = false)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            updateMaterialThreadLike(
                threadId = thread.threadId,
                liked = thread.liked,
                agreeNum = thread.agreeNum,
                loading = false,
            )
            emitGlobalEventSuspend(
                if (e is TiebaNotLoggedInException) {
                    ThreadLikeUiEvent.NotLoggedIn
                } else {
                    ThreadLikeUiEvent.Failed(e)
                }
            )
        }
    }

    private fun updateMaterialThreadLike(
        threadId: Long,
        liked: Boolean,
        agreeNum: Long,
        loading: Boolean,
    ) {
        _uiState.update { state ->
            state.copy(
                materialThreadRanks = state.materialThreadRanks.map { card ->
                    card.copy(
                        threads = card.threads.map { item ->
                            if (item.threadId == threadId) {
                                item.copy(liked = liked, agreeNum = agreeNum, likeLoading = loading)
                            } else {
                                item
                            }
                        }
                    )
                }
            )
        }
    }

    /**
     * Called when navigating back from thread page.
     *
     * @param threadId target thread ID
     * @param like like status of target thread
     * */
    fun onThreadResult(threadId: Long, like: Like) {
        launchInVM {
            val stateSnapshot = currentState
            val selectedTab = stateSnapshot.selectedTab
            val materialThread = stateSnapshot.materialThreadRanks
                .asSequence()
                .flatMap { it.threads.asSequence() }
                .firstOrNull { it.threadId == threadId }
            if (materialThread != null &&
                (materialThread.liked != like.liked || materialThread.agreeNum != like.count)
            ) {
                updateMaterialThreadLike(
                    threadId = threadId,
                    liked = like.liked,
                    agreeNum = like.count,
                    loading = false,
                )
            }
            val newThreads = stateSnapshot.threads?.updateLikeStatus(threadId, like)
            // Like data changed, update in-memory and local cache
            if (newThreads != null) {
                _uiState.update { it.copy(threads = newThreads) }
                updateCache(selectedTab, newThreads)
                exploreRepo.updateCachedThreadLike(threadId, like, from = ExplorePageItem.Hot, selectedTab)
            }
            // else: empty or no status changes
        }
    }

    override fun onCleared() {
        runBlocking(errorHandler) { clearCached() }
        super.onCleared()
    }
}
