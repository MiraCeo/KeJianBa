package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.huanchengfly.tieba.post.api.models.protos.RecommendForumInfo
import com.huanchengfly.tieba.post.repository.ForumSquareRepository
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

// Bootstrap public categories; successful responses replace this list.
private val initialForumSquareCategories = listOf(
    "推荐", "热门", "黑马", "游戏", "地区", "校园", "娱乐", "动漫", "数码", "体育",
    "情感", "小说", "影视综", "历史", "行业", "汽车", "音乐", "科学", "时尚", "搞笑",
    "财经", "摄影", "家居", "艺术", "旅游", "星座",
)

data class ForumSquareState(
    val categories: List<String> = initialForumSquareCategories,
    val category: String = "推荐",
    val forums: List<RecommendForumInfo> = emptyList(),
    val page: Int = 0,
    val initialized: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val paused: Boolean = false,
    val error: Throwable? = null,
    val errorIsRefresh: Boolean = false,
    val followBusy: Set<Long> = emptySet(),
)

@HiltViewModel
class ForumSquareViewModel @Inject constructor(
    private val repository: ForumSquareRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ForumSquareState())
    val state = mutableState.asStateFlow()
    private val mutableUiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val uiEvent = mutableUiEvent.asSharedFlow()
    private var accountUid: Long? = null
    private var active = false
    private var generation = 0
    private var job: Job? = null

    // Precomposition must not fetch. Reset account-specific results even while offscreen.
    fun activate(uid: Long?, isActive: Boolean) {
        if (uid != accountUid) {
            generation++
            job?.cancel()
            followJobs.values.forEach { it.cancel() }
            followJobs.clear()
            accountUid = uid
            mutableState.value = ForumSquareState()
        }
        active = isActive
        val current = state.value
        if (active && !current.initialized && !current.refreshing) refresh()
    }

    fun selectCategory(category: String) {
        if (category == state.value.category || category !in state.value.categories) return
        job?.cancel()
        generation++
        mutableState.value = ForumSquareState(categories = state.value.categories, category = category)
        refresh()
    }

    fun deactivate() { active = false }

    private val followJobs = mutableMapOf<Long, Job>()

    fun toggleFollow(forum: RecommendForumInfo) {
        val id = forum.forum_id
        if (id <= 0L || id in state.value.followBusy) return
        mutableState.value = state.value.copy(followBusy = state.value.followBusy + id)
        followJobs[id] = viewModelScope.launch {
            try {
                val followed = repository.toggleFollow(forum, accountUid)
                ensureActive()
                mutableState.value = state.value.copy(
                    forums = state.value.forums.map {
                        if (it.forum_id == id) it.copy(is_like = if (followed) 1 else 0) else it
                    },
                    followBusy = state.value.followBusy - id,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.value = state.value.copy(followBusy = state.value.followBusy - id)
                mutableUiEvent.emit(CommonUiEvent.ToastError(error))
            } finally {
                followJobs.remove(id)
            }
        }
    }

    fun refresh() {
        if (!active) return
        job?.cancel()
        generation++
        load(firstPage = true)
    }

    fun loadMore(manual: Boolean = false) {
        val current = state.value
        if (!active || current.refreshing || current.loadingMore || !current.hasMore) return
        if (!manual && (current.paused || current.error != null)) return
        load(firstPage = false)
    }

    private fun load(firstPage: Boolean) {
        val before = state.value
        val requestedPage = if (firstPage) 1 else before.page + 1
        val token = generation
        val uid = accountUid
        mutableState.value = before.copy(
            refreshing = firstPage, loadingMore = !firstPage, error = null, paused = false,
        )
        job = viewModelScope.launch {
            try {
                val response = repository.load(before.category, requestedPage, uid)
                ensureActive()
                if (generation != token) return@launch
                if ((response.serverCurrentPage != null && response.serverCurrentPage != requestedPage) ||
                    (response.serverCurrentPage == null && before.category != "推荐") ||
                    response.category != before.category) {
                    throw IOException("吧广场返回的分类或页码不一致")
                }
                val existing = if (firstPage) emptyList() else before.forums
                val forums = (existing + response.forums)
                    .filter { it.forum_id > 0 && it.forum_name.isNotBlank() }
                    .distinctBy { it.forum_id }
                val categories = response.categories.filter { it.isNotBlank() }.distinct()
                    .ifEmpty { before.categories }
                mutableState.value = before.copy(
                    categories = categories, forums = forums, page = requestedPage,
                    initialized = true, refreshing = false, loadingMore = false,
                    hasMore = response.hasMore,
                    // Empty/duplicate-only pages may still have more: pause automation,
                    // retain the server flag, and let the user explicitly continue.
                    paused = response.hasMore && forums.size == existing.size,
                    error = null,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == token) {
                    mutableState.value = before.copy(
                        initialized = true, refreshing = false, loadingMore = false, error = error,
                        errorIsRefresh = firstPage,
                    )
                }
            }
        }
    }
}
