package com.huanchengfly.tieba.post.ui.page.user.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.userPostHasMore
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.ExploreRepository.Companion.distinctById
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.page.user.thread.UserThreadViewModel.Companion.UserThreadVmFactory
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive

data class UserThreadUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val data: List<ThreadItem> = emptyList(),
) : UiState {

    val isEmpty: Boolean
        get() = data.isEmpty()
}

@HiltViewModel(assistedFactory = UserThreadVmFactory::class)
class UserThreadViewModel @AssistedInject constructor(
    @Assisted val uid: Long,
    private val userProfileRepo: UserProfileRepository,
) : ViewModel() {

    private val handler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    private val _uiState = MutableStateFlow(UserThreadUiState(isRefreshing = true))
    val uiState: StateFlow<UserThreadUiState> = _uiState.asStateFlow()
    private var loadMoreJob: Job? = null

    init {
        refreshInternal(cached = true)
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch(handler) {
        loadMoreJob?.cancel()
        _uiState.update { it.copy(isRefreshing = true, isLoadingMore = false, error = null) }
        val data = userProfileRepo.loadUserThread(uid, page = 1, cached)
        val unique = data.distinctById()
        ensureActive()
        _uiState.update {
            UserThreadUiState(isRefreshing = false, data = unique, currentPage = 1, hasMore = userPostHasMore(data.size))
        }
    }

    fun onRefresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        refreshInternal(cached = false)
    }

    fun onLoadMore() {
        val oldState = _uiState.value
        if (oldState.isLoadingMore || oldState.isRefreshing || !oldState.hasMore) return
        _uiState.set { copy(isLoadingMore = true, error = null) }

        loadMoreJob = viewModelScope.launch(handler) {
            val page = oldState.currentPage + 1
            val data = userProfileRepo.loadUserThread(uid, page, cached = true)
            val newData = if (data.isNotEmpty()) (oldState.data + data).distinctById() else null
            val hasMore = userPostHasMore(data.size, newData != null && newData.size > oldState.data.size)
            ensureActive()

            _uiState.update {
                it.copy(isLoadingMore = false, currentPage = page, data = newData ?: it.data, hasMore = hasMore)
            }
        }
    }

    companion object {
        private const val TAG = "UserThreadViewModel"

        @AssistedFactory
        interface UserThreadVmFactory {
            fun create(uid: Long): UserThreadViewModel
        }
    }
}
