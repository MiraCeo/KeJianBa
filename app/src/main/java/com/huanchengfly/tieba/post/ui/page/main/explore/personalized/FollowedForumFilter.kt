package com.huanchengfly.tieba.post.ui.page.main.explore.personalized

import com.huanchengfly.tieba.post.models.database.LocalLikedForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

internal data class FollowedForumFilter(val ids: Set<Long>, val names: Set<String>, val namesWithoutId: Set<String>) {
    val isEmpty: Boolean get() = ids.isEmpty() && names.isEmpty()

    fun includes(thread: ThreadItem): Boolean {
        val (id, name) = thread.simpleForum
        return if (id > 0) id in ids || name in namesWithoutId else name.isNotBlank() && name in names
    }

    companion object {
        fun from(uid: Long, forums: List<LocalLikedForum>): FollowedForumFilter {
            val own = forums.filter { it.uid == uid && uid > 0 }
            return FollowedForumFilter(
                own.mapNotNull { it.id.takeIf { id -> id > 0 } }.toSet(),
                own.map { it.name }.filter { it.isNotBlank() }.toSet(),
                own.filter { it.id <= 0 }.map { it.name }.filter { it.isNotBlank() }.toSet(),
            )
        }
    }
}

internal data class FollowedBatch(val threads: List<ThreadItem>, val lastPage: Int, val manualContinuation: Boolean)

// A filtered-empty page is not end-of-feed. Bound the work, then let the user continue.
internal suspend fun loadFollowedBatch(
    startPage: Int,
    existingIds: Set<Long>,
    accepts: (ThreadItem) -> Boolean,
    loadPage: suspend (Int) -> List<ThreadItem>,
): FollowedBatch {
    var lastPage = startPage
    repeat(3) { offset ->
        val page = startPage + offset
        val raw = loadPage(page)
        coroutineContext.ensureActive()
        lastPage = page
        val matches = raw.filter { it.id !in existingIds && accepts(it) }.distinctBy { it.id }
        if (matches.isNotEmpty()) return FollowedBatch(matches, page, manualContinuation = false)
        if (raw.isEmpty()) return FollowedBatch(emptyList(), page, manualContinuation = true)
    }
    return FollowedBatch(emptyList(), lastPage, manualContinuation = true)
}
