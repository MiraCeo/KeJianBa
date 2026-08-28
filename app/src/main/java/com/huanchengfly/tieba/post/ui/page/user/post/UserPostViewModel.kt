package com.huanchengfly.tieba.post.ui.page.user.post

import androidx.compose.ui.util.fastDistinctBy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.userPostHasMore
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.UserProfileRepository
import com.huanchengfly.tieba.post.ui.models.user.PostListItem
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostViewModel.Companion.UserPostVmFactory
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive

data class UserPostUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,

    val currentPage: Int = 1,
    val hasMore: Boolean = false,
    val data: List<PostListItem> = emptyList(),
) : UiState {

    val isEmpty: Boolean
        get() = data.isEmpty()
}

@HiltViewModel(assistedFactory = UserPostVmFactory::class)
class UserPostViewModel @AssistedInject constructor(
    @Assisted val uid: Long,
    private val userProfileRepo: UserProfileRepository,
) : ViewModel() {

    private val handler = TbLiteExceptionHandler(TAG) { _, e, _ ->
        _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
    }

    private val _uiState = MutableStateFlow(UserPostUiState(isRefreshing = true))
    val uiState: StateFlow<UserPostUiState> = _uiState.asStateFlow()
    private var loadMoreJob: Job? = null

    init {
        refreshInternal(cached = true)
    }

    private fun refreshInternal(cached: Boolean) = viewModelScope.launch(handler) {
        loadMoreJob?.cancel()
        _uiState.update { it.copy(isRefreshing = true, isLoadingMore = false, error = null) }
        val data = userProfileRepo.loadUserPost(uid, page = 1, cached)
        ensureActive()
        _uiState.update {
            UserPostUiState(isRefreshing = false, data = data.fastDistinctBy { it.lazyListKey },
                currentPage = 1, hasMore = userPostHasMore(data.size))
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
            val data = userProfileRepo.loadUserPost(uid, page, cached = true)
            val newData = if (data.isNotEmpty()) {
                withContext(Dispatchers.Default) {
                    (oldState.data + data).fastDistinctBy { p -> p.lazyListKey }
                }
            } else {
                null
            }
            val hasMore = userPostHasMore(data.size, newData != null && newData.size > oldState.data.size)
            ensureActive()

            _uiState.update {
                it.copy(isLoadingMore = false, currentPage = page, data = newData ?: it.data, hasMore = hasMore)
            }
        }
    }

    companion object {
        private const val TAG = "UserPostViewModel"

        @AssistedFactory
        interface UserPostVmFactory {
            fun create(uid: Long): UserPostViewModel
        }
    }
}
