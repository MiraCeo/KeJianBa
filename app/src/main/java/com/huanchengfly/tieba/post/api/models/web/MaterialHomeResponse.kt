package com.huanchengfly.tieba.post.api.models.web

import com.google.gson.annotations.SerializedName

data class MaterialHomeResponse(
    @SerializedName("error_code") val errorCode: Int = -1,
    @SerializedName("error_msg") val errorMessage: String = "",
    val data: MaterialHomeData? = null,
)

data class MaterialHomeData(
    @SerializedName("activity_info") val activityInfo: MaterialHomeActivityInfo? = null,
)

data class MaterialHomeActivityInfo(
    @SerializedName("thread_bang") val threadRank: MaterialHomeThreadRank? = null,
)

data class MaterialHomeThreadRank(
    @SerializedName("tab_list") val tabList: List<MaterialHomeThreadRankTab> = emptyList(),
)

data class MaterialHomeThreadRankTab(
    val name: String = "",
    @SerializedName("tab_code") val tabCode: String = "",
    @SerializedName("label_list") val labelList: List<String> = emptyList(),
    @SerializedName("more_text") val moreText: String = "",
    @SerializedName("sort_rule") val sortRule: String = "",
    @SerializedName("pic_url") val pictureUrl: String = "",
    @SerializedName("thread_list") val threadList: List<MaterialHomeRankThread> = emptyList(),
)

data class MaterialHomeRankThread(
    val id: Long = 0L,
    val title: String = "",
    @SerializedName("first_post_id") val firstPostId: Long = 0L,
    @SerializedName("abstract") val abstractContent: List<MaterialHomeRankAbstract> = emptyList(),
    val fid: Long = 0L,
    val fname: String = "",
    @SerializedName("agree_num") val agreeNum: Long = 0L,
    @SerializedName("view_num") val viewNum: Long = 0L,
    val agree: MaterialHomeRankAgree? = null,
    @SerializedName("reply_num") val replyNum: Long = 0L,
    @SerializedName("forum_info") val forumInfo: MaterialHomeForumInfo? = null,
    val media: List<MaterialHomeRankMedia> = emptyList(),
)

data class MaterialHomeRankAgree(
    @SerializedName("has_agree") val hasAgree: Int = 0,
)

data class MaterialHomeRankAbstract(
    val text: String = "",
    val type: Int = 0,
)

data class MaterialHomeForumInfo(
    val avatar: String = "",
)

data class MaterialHomeRankMedia(
    @SerializedName("big_pic") val bigPicture: String = "",
    @SerializedName("src_pic") val sourcePicture: String = "",
    val width: Int = 0,
    val height: Int = 0,
)
