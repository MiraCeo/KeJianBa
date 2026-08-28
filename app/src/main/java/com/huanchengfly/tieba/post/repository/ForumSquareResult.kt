package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.ForumSquareRecommendResponse
import com.huanchengfly.tieba.post.api.models.protos.RecommendForumInfo
import com.huanchengfly.tieba.post.api.models.protos.forumSquare.ForumSquareResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import java.io.IOException

data class ForumSquareResult(
    val categories: List<String>,
    val category: String,
    val forums: List<RecommendForumInfo>,
    // Only the hybrid recommendation endpoint may omit this; never synthesize it.
    val serverCurrentPage: Int?,
    val hasMore: Boolean,
)

class ForumSquareRecommendationLoginRequired : IOException("吧广场推荐需要登录")
class ForumSquareRecommendationUnavailable : IOException("服务端未返回推荐分类")

internal fun ForumSquareResponseData.toSquareResult(): ForumSquareResult {
    val paging = page ?: throw IOException("吧广场响应缺少分页信息")
    return ForumSquareResult(categories, category, forums, paging.current_page, paging.has_more != 0)
}

internal fun ForumSquareRecommendResponse.toSquareResult(): ForumSquareResult {
    val code = errorCode ?: throw IOException("吧广场推荐响应缺少状态信息")
    if (code != 0) throw TiebaApiException(CommonResponse(code, errorMessage.orEmpty()))
    if (category != "推荐") throw ForumSquareRecommendationUnavailable()
    val paging = page ?: throw IOException("吧广场推荐响应缺少分页信息")
    val more = paging.hasMore ?: throw IOException("吧广场推荐响应缺少后续页标记")
    if (more !in 0..1) throw IOException("吧广场推荐分页标记无效")
    val items = forums ?: throw IOException("吧广场推荐响应缺少贴吧列表")
    return ForumSquareResult(
        categories = categories.orEmpty(), category = "推荐",
        forums = items.map { forum ->
            RecommendForumInfo(
                forum_id = forum.id ?: 0L, forum_name = forum.name.orEmpty(),
                avatar = forum.avatar.orEmpty(), is_like = forum.isLike ?: 0,
                member_count = forum.memberCount ?: 0, thread_count = forum.threadCount ?: 0,
                slogan = forum.slogan.orEmpty(), recom_reason = forum.reason.orEmpty(),
            )
        },
        serverCurrentPage = paging.currentPage, hasMore = more == 1,
    )
}
