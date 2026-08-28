package com.huanchengfly.tieba.post.ui.page.threadstore

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.ThreadStoreRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.ThreadStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class ThreadStoreUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
    // The existing API uses offset = page * limit, so pages start at zero.
    val currentPage: Int = 0,
    val data: List<ThreadStore> = emptyList(),
    val error: Throwable? = null
) : UiState {
    val isEmpty: Boolean get() = data.isEmpty()
}

@HiltViewModel(assistedFactory = ThreadStoreViewModel.Factory::class)
class ThreadStoreViewModel @AssistedInject constructor(
    @Assisted val accountUid: Long,
    private val threadStoreRepo: ThreadStoreRepository,
    private val settings: SettingsRepository,
) : BaseStateViewModel<ThreadStoreUiState>() {
    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        if (suppressed && !currentState.isEmpty) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
        }
    }

    private val requests = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]))
    private var loadMoreJob: Job? = null
    private val removedIds = mutableSetOf<Long>()
    private val deletingIds = mutableSetOf<Long>()

    init {
        viewModelScope.launch {
            settings.accountUid.collect { uid ->
                if (uid != accountUid) {
                    requests.coroutineContext.cancelChildren()
                    removedIds.clear()
                    deletingIds.clear()
                    _uiState.value = ThreadStoreUiState(hasMore = false)
                }
            }
        }
        refreshInternal()
    }

    override fun createInitialState() = ThreadStoreUiState()

    private suspend fun ensureAccount() {
        currentCoroutineContext().ensureActive()
        if (settings.accountUid.first() != accountUid) throw CancellationException("Collection account changed")
    }

    private fun refreshInternal() {
        loadMoreJob?.cancel()
        removedIds.retainAll(deletingIds)
        _uiState.update { it.copy(isRefreshing = true, isLoadingMore = false, error = null) }
        requests.launch(errorHandler) {
            ensureAccount()
            val data = threadStoreRepo.load(page = 0, expectedUid = accountUid)
            ensureAccount()
            _uiState.update {
                ThreadStoreUiState(data = data.distinctBy { item -> item.id }.filterNot { item -> item.id in removedIds },
                    hasMore = data.size >= ThreadStoreRepository.LOAD_LIMIT)
            }
        }
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal()
    }

    fun onLoadMore() {
        val old = currentState
        if (old.isRefreshing || old.isLoadingMore || !old.hasMore) return
        _uiState.update { it.copy(isLoadingMore = true, error = null) }
        loadMoreJob = requests.launch(errorHandler) {
            ensureAccount()
            val nextPage = old.currentPage + 1
            val data = threadStoreRepo.load(page = nextPage, expectedUid = accountUid)
            ensureAccount()
            _uiState.update { state ->
                val merged = (state.data + data).distinctBy { it.id }.filterNot { it.id in removedIds }
                state.copy(isLoadingMore = false, currentPage = nextPage, data = merged,
                    hasMore = data.size >= ThreadStoreRepository.LOAD_LIMIT && merged.size > state.data.size)
            }
        }
    }

    fun onDelete(thread: ThreadStore) {
        if (!deletingIds.add(thread.id)) return
        requests.launch(errorHandler) {
            try {
                ensureAccount()
                val oldIndex = currentState.data.indexOfFirst { it.id == thread.id }
                removedIds.add(thread.id)
                _uiState.update { it.copy(data = it.data.filterNot { item -> item.id == thread.id }) }
                val result = threadStoreRepo.remove(thread, expectedUid = accountUid)
                ensureAccount()
                result.onFailure { e ->
                    if (e is CancellationException) throw e
                    removedIds.remove(thread.id)
                    // Restore this item only: do not overwrite pages loaded during the request.
                    _uiState.update { state ->
                        if (state.data.any { it.id == thread.id }) state else state.copy(
                            data = state.data.toMutableList().apply { add(oldIndex.coerceIn(0, size), thread) })
                    }
                    emitUiEvent(ThreadStoreUiEvent.Delete.Failure(e.getErrorMessage()))
                }.onSuccess { emitUiEvent(ThreadStoreUiEvent.Delete.Success) }
            } finally {
                deletingIds.remove(thread.id)
            }
        }
    }

    fun onThreadResult(threadId: Long, markedPostId: Long?) {
        if (markedPostId == null) removedIds.add(threadId) else removedIds.remove(threadId)
        _uiState.update { state ->
            state.copy(data = if (markedPostId == null) state.data.filterNot { it.id == threadId }
                else state.data.map { if (it.id == threadId) it.copy(markPid = markedPostId) else it })
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(accountUid: Long): ThreadStoreViewModel
    }

    companion object {
        private const val TAG = "ThreadStoreViewModel"
    }
}
