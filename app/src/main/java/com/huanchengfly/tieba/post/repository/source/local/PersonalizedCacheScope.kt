package com.huanchengfly.tieba.post.repository.source.local

internal fun personalizedAccountCachePrefix(uid: Long?): String = if (uid == null) "p_" else "p_${uid}_"

internal fun personalizedCachePrefix(scope: String?): String {
    require(scope == null || scope.matches(Regex("-?[0-9]+_(followed|discover)")))
    return if (scope == null) "p_" else "p_${scope}_"
}
