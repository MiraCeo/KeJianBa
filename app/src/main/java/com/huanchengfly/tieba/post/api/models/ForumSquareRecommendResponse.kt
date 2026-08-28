package com.huanchengfly.tieba.post.api.models

import com.google.gson.annotations.SerializedName

// The hybrid endpoint returns a flat JSON object, not the protobuf data envelope.
// Nullable numeric fields also accept quoted numbers through Gson.
data class ForumSquareRecommendResponse(
    @SerializedName("error_code") val errorCode: Int? = null,
    @SerializedName("error_msg") val errorMessage: String? = null,
    @SerializedName("page_structure") val categories: List<String>? = null,
    @SerializedName("class_name") val category: String? = null,
    @SerializedName("forum_info") val forums: List<Forum>? = null,
    val page: Paging? = null,
) {
    data class Paging(
        @SerializedName("has_more") val hasMore: Int? = null,
        @SerializedName("current_page") val currentPage: Int? = null,
    )

    data class Forum(
        @SerializedName("forum_id") val id: Long? = null,
        @SerializedName("forum_name") val name: String? = null,
        val avatar: String? = null,
        @SerializedName("is_like") val isLike: Int? = null,
        @SerializedName("member_count") val memberCount: Int? = null,
        @SerializedName("thread_count") val threadCount: Int? = null,
        val slogan: String? = null,
        @SerializedName("recom_reason") val reason: String? = null,
    )
}
