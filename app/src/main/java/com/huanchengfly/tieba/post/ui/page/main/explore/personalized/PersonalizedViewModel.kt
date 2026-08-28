package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import androidx.collection.ArraySet
import androidx.collection.MutableScatterSet
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import com.huanchengfly.tieba.post.arch.BaseStateViewModel
import com.huanchengfly.tieba.post.arch.CommonUiEvent
import com.huanchengfly.tieba.post.arch.TbLiteExceptionHandler
import com.huanchengfly.tieba.post.arch.UiEvent
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.stateInViewModel
import com.huanchengfly.tieba.post.models.database.BlockForum
import com.huanchengfly.tieba.post.models.database.BlockUser
import com.huanchengfly.tieba.post.repository.BlockRepository
import com.huanchengfly.tieba.post.repository.ExploreRepository
import com.huanchengfly.tieba.post.repository.HomeRepository
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.explore.Dislike
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePageItem
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatus
import com.huanchengfly.tieba.post.ui.page.main.explore.concern.ConcernViewModel.Companion.updateLikeStatusUiStateCommon
import com.huanchengfly.tieba.post.utils.extension.set
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.util.Collections

enum class PersonalizedEmptyReason { None, LoginRequired, NoForums, NoMatches }

@Immutable
data class PersonalizedUiState(
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: Throwable? = null,
    val currentPage: Int = 1,
    val data: List<ThreadItem> = emptyList(),
    val emptyReason: PersonalizedEmptyReason = PersonalizedEmptyReason.None,
    val manualContinuation: Boolean = false,
): UiState {

    val isEmpty: Boolean
        get() = data.isEmpty()
}

@Stable
@HiltViewModel(assistedFactory = PersonalizedViewModel.Factory::class)
class PersonalizedViewModel @AssistedInject constructor(
    @Assisted val followedOnly: Boolean,
    @Assisted val accountUid: Long,
    private val exploreRepo: ExploreRepository,
    private val blockRepo: BlockRepository,
    private val homeRepo: HomeRepository,
    private val settingsRepository: SettingsRepository,
) : BaseStateViewModel<PersonalizedUiState>() {

    @AssistedFactory
    interface Factory {
        fun create(followedOnly: Boolean, accountUid: Long): PersonalizedViewModel
    }

    companion object {
        private const val TAG = "PersonalizedViewModel"

        private suspend fun List<ThreadItem>.distinctById(blockedIds: Set<Long>): List<ThreadItem> {
            return withContext(Dispatchers.Default) {
                val set = MutableScatterSet<Long>(size)
                val result = mutableListOf<ThreadItem>()
                fastForEach {
                    // Check blocked and distinct
                    if (it.id !in blockedIds && set.add(it.id)) result += it
                }
                return@withContext result
            }
        }
    }

    override val errorHandler = TbLiteExceptionHandler(TAG) { _, e, suppressed ->
        // Allow user browse existing content on suppressed exceptions
        if (suppressed && !currentState.isEmpty) {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = null) }
            sendUiEvent(CommonUiEvent.ToastError(e))
        } else {
            _uiState.update { it.copy(isRefreshing = false, isLoadingMore = false, error = e) }
        }
    }

    private val blockedIds: MutableSet<Long> = Collections.synchronizedSet(ArraySet())

    val hideBlockedContent: StateFlow<Boolean> = settingsRepository.blockSettings
        .map { it.hideBlocked }
        .stateInViewModel(initialValue = false)

    private var loadMoreJob: Job? = null
    private var refreshJob: Job? = null
    private var followedForums = FollowedForumFilter.from(accountUid, emptyList())
    private val cacheScope = "${accountUid}_${if (followedOnly) "followed" else "discover"}"

    init {
        if (followedOnly && accountUid > 0) launchInVM {
            homeRepo.observeFollowedForums(accountUid)
                .map { FollowedForumFilter.from(accountUid, it) }
                .distinctUntilChanged()
                .collect { filter ->
                    followedForums = filter
                    _uiState.update { state ->
                        val data = state.data.filter(filter::includes)
                        state.copy(
                            data = data,
                            emptyReason = when {
                                filter.isEmpty -> PersonalizedEmptyReason.NoForums
                                data.isEmpty() -> PersonalizedEmptyReason.NoMatches
                                else -> PersonalizedEmptyReason.None
                            },
                        )
                    }
                }
        }
        launchInVM {
            settingsRepository.accountUid.distinctUntilChanged().collectLatest { uid ->
                refreshJob?.cancelAndJoin()
                loadMoreJob?.cancelAndJoin()
                blockedIds.clear()
                _uiState.value = PersonalizedUiState(isRefreshing = uid == accountUid)
                if (uid == accountUid) refreshInternal(cached = true)
            }
        }
    }

    override fun createInitialState(): PersonalizedUiState = PersonalizedUiState()

    private fun refreshInternal(cached: Boolean) {
        refreshJob?.cancel()
        refreshJob = launchJobInVM {
            var showTip = false
            loadMoreJob?.cancelAndJoin()
            _uiState.update {
                showTip = !it.isEmpty
                // Allow browsing existing content while refreshing.
                it.copy(isRefreshing = true, isLoadingMore = true, error = null)
            }
            requireCurrentAccount()
            if (followedOnly && accountUid <= 0) {
                _uiState.value = PersonalizedUiState(isRefreshing = false, emptyReason = PersonalizedEmptyReason.LoginRequired)
                return@launchJobInVM
            }
            if (followedOnly) {
                // Refresh membership on entry/pull-to-refresh; do not inherit the forum page's seven-day TTL.
                val snapshot = homeRepo.followedForumsForFeed(accountUid, cached = false)
                requireCurrentAccount()
                followedForums = FollowedForumFilter.from(accountUid, snapshot.forums)
                if (snapshot.usedCachedSnapshot) sendUiEvent(PersonalizedUiEvent.UsingCachedForums)
                if (followedForums.isEmpty) {
                    _uiState.value = PersonalizedUiState(isRefreshing = false, emptyReason = PersonalizedEmptyReason.NoForums)
                    return@launchJobInVM
                }
            }
            val batch = loadBatch(1, cached, emptySet())
            val candidates = batch.threads.distinctById(blockedIds)
            requireCurrentAccount()
            // Re-read membership after suspending work so an unfollow cannot be undone by a late response.
            val data = visibleData(candidates)
            _uiState.set {
                PersonalizedUiState(
                    isRefreshing = false, data = data, currentPage = batch.lastPage,
                    manualContinuation = batch.manualContinuation,
                    emptyReason = emptyReasonFor(data),
                )
            }
            if (showTip) sendUiEvent(PersonalizedUiEvent.RefreshSuccess(data.size))
        }
    }

    private suspend fun requireCurrentAccount() {
        if (settingsRepository.accountUid.snapshot() != accountUid) throw CancellationException("Account changed")
    }

    private fun visibleData(data: List<ThreadItem>): List<ThreadItem> =
        if (followedOnly) data.filter(followedForums::includes) else data

    private fun emptyReasonFor(data: List<ThreadItem>): PersonalizedEmptyReason = when {
        !followedOnly || data.isNotEmpty() -> PersonalizedEmptyReason.None
        accountUid <= 0 -> PersonalizedEmptyReason.LoginRequired
        followedForums.isEmpty -> PersonalizedEmptyReason.NoForums
        else -> PersonalizedEmptyReason.NoMatches
    }

    private suspend fun loadBatch(startPage: Int, cached: Boolean, existingIds: Set<Long>): FollowedBatch {
        suspend fun load(page: Int): List<ThreadItem> {
            requireCurrentAccount()
            val data = exploreRepo.loadPersonalized(page, cached = if (page == 1) cached else true,
                cacheScope = cacheScope, expectedUid = accountUid)
            requireCurrentAccount()
            return data
        }
        if (!followedOnly) return FollowedBatch(load(startPage), startPage, manualContinuation = false)
        val hideBlocked = settingsRepository.blockSettings.snapshot().hideBlocked
        return loadFollowedBatch(
            startPage, existingIds,
            accepts = { followedForums.includes(it) && it.id !in blockedIds && (!hideBlocked || !it.blocked) },
            loadPage = ::load,
        )
    }

    fun onRefresh() {
        if (!currentState.isRefreshing) refreshInternal(cached = false)
    }

    fun onLoadMore() {
        val oldState = currentState
        if (oldState.isLoadingMore || oldState.isRefreshing) return
        if (followedOnly && (accountUid <= 0 || followedForums.isEmpty)) return

        _uiState.update { it.copy(isLoadingMore = true) }
        loadMoreJob?.cancel()
        loadMoreJob = launchJobInVM {
            val page = oldState.currentPage + 1
            val batch = loadBatch(page, cached = true, oldState.data.map { it.id }.toSet())
            val candidates = batch.threads.distinctById(blockedIds)
            ensureActive()
            requireCurrentAccount()
            _uiState.update {
                val newData = visibleData(it.data + candidates).filter { thread -> thread.id !in blockedIds }.distinctBy { thread -> thread.id }
                it.copy(isLoadingMore = false, currentPage = batch.lastPage, data = newData,
                    manualContinuation = batch.manualContinuation,
                    emptyReason = emptyReasonFor(newData))
            }
        }
    }

    fun onThreadLikeClicked(thread: ThreadItem): Unit = launchInVM {
        updateLikeStatusUiStateCommon(
            thread = thread,
            onRequestLikeThread = { exploreRepo.onLikeThread(it, ExplorePageItem.Personalized) },
            onEvent = ::emitGlobalEventSuspend
        ) { threadId, liked, loading ->
            val newData = currentState.data.updateLikeStatus(threadId, liked, loading)
            _uiState.update { it.copy(data = newData) }
        }
    }

    fun onThreadDislike(thread: ThreadItem, reasons: List<Dislike>) {
        if (!blockedIds.add(thread.id)) return

        launchInVM {
            _uiState.update { it.copy(data = it.data.distinctById(blockedIds)) }
            runCatching {
                exploreRepo.onDislikeThread(thread, reasons)
            }
            .onFailure { // ignore errors and keep data changes
                sendUiEvent(PersonalizedUiEvent.DislikeFailed(it))
            }
            // Update local block rule (blacklist)
            var updated = false
            for (reason in reasons) {
                when (reason.id) {
                    Dislike.TYPE_ID_USER -> {
                        updated = true
                        blockRepo.upsertUser(
                            thread.author.run { BlockUser(uid = id, name, whitelisted = false) }
                        )
                    }

                    Dislike.TYPE_ID_FORUM -> {
                        updated = true
                        blockRepo.upsertForum(BlockForum(name = thread.simpleForum.second))
                    }
                }
            }
            if (updated) {
                val newData = withContext(Dispatchers.Default) {
                    currentState.data.fastMap { thread ->
                        val forumName = thread.simpleForum.second
                        val content = thread.content?.text.orEmpty()
                        val blocked = blockRepo.isBlocked(forumName, uid = thread.author.id, content)
                        if (blocked xor thread.blocked) thread.copy(blocked = blocked) else thread
                    }
                }
                _uiState.update { it.copy(data = newData) }
                sendUiEvent(PersonalizedUiEvent.BlockRuleUpdated)
            }
        }
    }

    /**
     * Called when navigating back from thread page.
     *
     * @param threadId target thread ID
     * @param like latest thread like status
     * */
    fun onThreadResult(threadId: Long, like: Like): Unit = launchInVM {
        val newData = currentState.data.updateLikeStatus(threadId, like)
        if (newData != null) {
            _uiState.update { it.copy(data = newData) }
            exploreRepo.updateCachedThreadLike(threadId, like, from = ExplorePageItem.Personalized)
        }
        // else -> empty or no status changes
    }
}

sealed interface PersonalizedUiEvent : UiEvent {
    object UsingCachedForums: PersonalizedUiEvent
    class RefreshSuccess(val count: Int) : PersonalizedUiEvent

    object BlockRuleUpdated: PersonalizedUiEvent

    class DislikeFailed(val e: Throwable): PersonalizedUiEvent
}
