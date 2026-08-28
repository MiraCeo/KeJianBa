package com.huanchengfly.tieba.post.api

// The existing userpost response has no explicit has-more field.
internal const val USER_POST_PAGE_SIZE = 20

internal fun userPostHasMore(receivedCount: Int, addedNewItems: Boolean = true): Boolean =
    receivedCount >= USER_POST_PAGE_SIZE && addedNewItems
