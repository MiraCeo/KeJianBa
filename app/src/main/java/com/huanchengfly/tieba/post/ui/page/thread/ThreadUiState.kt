package com.huanchengfly.tieba.post.ui.page.thread

import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.arch.UiState
import com.huanchengfly.tieba.post.repository.PageData
import com.huanchengfly.tieba.post.ui.models.PostData
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadInfoData
import com.huanchengfly.tieba.post.ui.models.UserData

@Immutable
data class ThreadUiState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadingLatestReply: Boolean = false,
    val error: Throwable? = null,
    val seeLz: Boolean = false,
    @ThreadSortType val sortType: Int = ThreadSortType.DEFAULT,
    val user: UserData? = null,
    val firstPost: PostData? = null,
    val thread: ThreadInfoData? = null,
    val tbs: String? = null,
    val data: List<PostData> = emptyList(),
    val latestPosts: List<PostData>? = null,
    val pageData: PageData = PageData(),
    val pageAnchors: List<PageAnchor> = emptyList(),
) : UiState {

    val lz: UserData?
        get() = firstPost?.author

    val forum: SimpleForum?
        get() = thread?.simpleForum

    /**
     * Which page the given floor sits on.
     *
     * [data] holds every page loaded so far, so [PageData.current] only tells us how far
     * loading has reached - it goes stale as soon as the reader scrolls back up. Anchors are
     * stored in display order, which makes one comparison work for both sort directions.
     */
    fun pageOfFloor(floor: Int): Int {
        if (pageAnchors.isEmpty()) return pageData.current
        val descending = sortType == ThreadSortType.BY_DESC
        var page = pageAnchors.first().page
        for (anchor in pageAnchors) {
            val reached = if (descending) floor <= anchor.firstFloor else floor >= anchor.firstFloor
            if (!reached) break
            page = anchor.page
        }
        return page
    }
}

/**
 * Start of one loaded page. Keyed by floor rather than list index so that prepending an
 * earlier page does not invalidate the anchors already recorded.
 */
@Immutable
data class PageAnchor(val firstFloor: Int, val page: Int)
