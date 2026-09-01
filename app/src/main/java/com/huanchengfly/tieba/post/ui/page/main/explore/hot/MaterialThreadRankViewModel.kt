package com.huanchengfly.tieba.post.ui.page.main.explore.hot

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.repository.HotTopicRepository
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankCard
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankItem
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankTab
import com.huanchengfly.tieba.post.ui.page.Destination
import com.huanchengfly.tieba.post.ui.page.thread.ThreadLikeUiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Immutable
data class MaterialThreadRankUiState(
    val isRefreshing: Boolean = true,
    val selectedTabCode: String,
    val tabs: List<MaterialThreadRankTab> = emptyList(),
    val rankCard: MaterialThreadRankCard? = null,
    val updatedAtMillis: Long? = null,
    val error: Throwable? = null,
) : UiState {
    val isEmpty: Boolean get() = rankCard == null || rankCard.threads.isEmpty()
}

@Stable
@HiltViewModel
class MaterialThreadRankViewModel @Inject constructor(
    private val repository: HotTopicRepository,
    savedStateHandle: SavedStateHandle,
) : BaseStateViewModel<MaterialThreadRankUiState>() {

    private val route = savedStateHandle.toRoute<Destination.MaterialThreadRankList>()

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, error, suppressed ->
        _uiState.update { state ->
            if (suppressed && !state.isEmpty) {
                state.copy(isRefreshing = false, error = null)
            } else {
                state.copy(isRefreshing = false, error = error)
            }
        }
    }

    init {
        loadRank(route.initialTabCode)
    }

    override fun createInitialState(): MaterialThreadRankUiState =
        MaterialThreadRankUiState(selectedTabCode = route.initialTabCode)

    fun onRefresh() {
        if (!currentState.isRefreshing) {
            loadRank(currentState.selectedTabCode)
        }
    }

    private fun loadRank(tabCode: String) {
        _uiState.update { it.copy(isRefreshing = true, selectedTabCode = tabCode, error = null) }
        launchInVM {
            val data = repository.loadMaterialThreadRankPage(tabCode, includeTabs = false)
            _uiState.update { state ->
                state.copy(
                    isRefreshing = false,
                    selectedTabCode = data.currentTabCode,
                    tabs = data.tabs.ifEmpty { state.tabs },
                    rankCard = data.rankCard,
                    updatedAtMillis = data.updatedAtMillis,
                    error = null,
                )
            }
        }
    }

    fun onLikeClicked(thread: MaterialThreadRankItem) = launchInVM {
        if (thread.likeLoading) {
            emitGlobalEventSuspend(ThreadLikeUiEvent.Connecting)
            return@launchInVM
        }

        val liked = !thread.liked
        val agreeNum = (thread.agreeNum + if (liked) 1L else -1L).coerceAtLeast(0L)
        updateLike(thread.threadId, liked, agreeNum, loading = true)
        try {
            repository.setMaterialThreadLiked(thread.threadId, thread.firstPostId, liked)
            updateLike(thread.threadId, liked, agreeNum, loading = false)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            updateLike(thread.threadId, thread.liked, thread.agreeNum, loading = false)
            emitGlobalEventSuspend(
                if (error is TiebaNotLoggedInException) {
                    ThreadLikeUiEvent.NotLoggedIn
                } else {
                    ThreadLikeUiEvent.Failed(error)
                }
            )
        }
    }

    fun onThreadResult(threadId: Long, like: Like) {
        val thread = currentState.rankCard?.threads?.firstOrNull { it.threadId == threadId } ?: return
        if (thread.liked != like.liked || thread.agreeNum != like.count) {
            updateLike(threadId, like.liked, like.count, loading = false)
        }
    }

    private fun updateLike(threadId: Long, liked: Boolean, agreeNum: Long, loading: Boolean) {
        _uiState.update { state ->
            state.copy(
                rankCard = state.rankCard?.copy(
                    threads = state.rankCard.threads.map { item ->
                        if (item.threadId == threadId) {
                            item.copy(liked = liked, agreeNum = agreeNum, likeLoading = loading)
                        } else {
                            item
                        }
                    }
                )
            )
        }
    }

    companion object {
        private const val TAG = "MaterialThreadRankViewModel"
    }
}
