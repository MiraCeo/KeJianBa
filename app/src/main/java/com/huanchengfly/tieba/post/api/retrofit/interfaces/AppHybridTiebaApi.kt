package com.huanchengfly.tieba.post.api.retrofit.interfaces

import com.huanchengfly.tieba.post.api.Param
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.ForumSquareRecommendResponse
import com.huanchengfly.tieba.post.api.models.SearchForumBean
import com.huanchengfly.tieba.post.api.models.SearchThreadBean
import com.huanchengfly.tieba.post.api.models.SearchUserBean
import com.huanchengfly.tieba.post.api.models.TopicDetailBean
import com.huanchengfly.tieba.post.api.models.web.MaterialHomeResponse
import com.huanchengfly.tieba.post.api.models.web.MaterialThreadRankResponse
import com.huanchengfly.tieba.post.api.urlEncode
import kotlinx.coroutines.flow.Flow
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.QueryMap
import com.huanchengfly.tieba.post.api.Header as TiebaHeaders

interface AppHybridTiebaApi {
    @Headers(
        "${TiebaHeaders.ADD_WEB_COOKIE}: ${TiebaHeaders.ADD_WEB_COOKIE_FALSE}",
        "${TiebaHeaders.NO_COMMON_PARAMS}: ${Param.BDUSS},${Param.STOKEN}",
    )
    @GET("/c/f/material/home")
    fun materialHomeFlow(
        @QueryMap params: Map<String, String>,
        @Header("Cookie") cookie: String,
        @Header("User-Agent") userAgent: String,
    ): Flow<MaterialHomeResponse>

    @Headers(
        "${TiebaHeaders.ADD_WEB_COOKIE}: ${TiebaHeaders.ADD_WEB_COOKIE_FALSE}",
        "${TiebaHeaders.NO_COMMON_PARAMS}: ${Param.BDUSS},${Param.STOKEN}",
    )
    @GET("/c/f/material/threadbang")
    fun materialThreadRankFlow(
        @QueryMap params: Map<String, String>,
        @Query("tab_code") tabCode: String,
        @Query("is_need_tab") includeTabs: Int,
        @Header("Cookie") cookie: String,
        @Header("User-Agent") userAgent: String,
    ): Flow<MaterialThreadRankResponse>

    @Headers(
        "${TiebaHeaders.ADD_WEB_COOKIE}: ${TiebaHeaders.ADD_WEB_COOKIE_FALSE}",
        "${TiebaHeaders.NO_COMMON_PARAMS}: ${Param.BDUSS},${Param.STOKEN}",
        "Referer: https://tieba.baidu.com/mo/q/hybrid-main-bawu/forumConcern/hybrid",
    )
    @GET("/c/f/forum/getForumSquare")
    fun forumSquareRecommendFlow(
        @QueryMap params: Map<String, String>,
        @Header("Cookie") cookie: String,
        @Header("User-Agent") userAgent: String,
    ): Flow<ForumSquareRecommendResponse>

    @Headers(
        "${TiebaHeaders.NO_ST_PARAMS}: ${TiebaHeaders.NO_ST_PARAMS_TRUE}",
        "${TiebaHeaders.NO_COMMON_PARAMS}: ${Param.BDUSS},${Param.STOKEN}",
    )
    @GET("/mo/q/search/forum")
    fun searchForumFlow(
        @Query("word") keyword: String,
        @Header("Referer") referer: String = "https://tieba.baidu.com/mo/q/hybrid/search?keyword=$keyword&_webview_time=${System.currentTimeMillis()}".urlEncode(),
    ): Flow<SearchForumBean>

    @Headers(
        "${TiebaHeaders.NO_ST_PARAMS}: ${TiebaHeaders.NO_ST_PARAMS_TRUE}",
        "${TiebaHeaders.NO_COMMON_PARAMS}: ${Param.BDUSS},${Param.STOKEN}",
    )
    @GET("/mo/q/search/thread")
    fun searchThreadFlow(
        @Query("word") keyword: String,
        @Query("pn") page: Int,
        @Query("st") sort: Int,
        @Query("tt") filter: Int = 1,
        @Query("rn") pageSize: Int? = null,
        @Query("fname") forumName: String? = null,
        @Query("ct") ct: Int = 1,
        @Query("is_use_zonghe") isUseZonghe: Int? = 1,
        @Query("cv") clientVersion: String = "99.9.101",
        @Header("Referer") referer: String = "https://tieba.baidu.com/mo/q/hybrid/search?keyword=$keyword&_webview_time=${System.currentTimeMillis()}".urlEncode(),
    ): Flow<SearchThreadBean>

    @Headers(
        "${TiebaHeaders.NO_ST_PARAMS}: ${TiebaHeaders.NO_ST_PARAMS_TRUE}",
        "${TiebaHeaders.NO_COMMON_PARAMS}: ${Param.BDUSS},${Param.STOKEN}",
    )
    @GET("/mo/q/search/user")
    fun searchUserFlow(
        @Query("word") keyword: String,
        @Header("Referer") referer: String = "https://tieba.baidu.com/mo/q/hybrid/search?keyword=$keyword&_webview_time=${System.currentTimeMillis()}".urlEncode(),
    ): Flow<SearchUserBean>

    @GET("/mo/q/newtopic/topicDetail")
    fun topicDetailFlow(
        @Query("topic_id") topicId: String,
        @Query("topic_name") topicName: String,
        @Query("is_new") isNew: Int = 1,
        @Query("is_share") isShare: Int = 1,
        @Query("pn") page: Int = 1,
        @Query("rn") pageSize: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("last_id") lastId: String = "",
        @Query("derivative_to_pic_id") derivativeToPicId: String = "",
    ): Flow<TopicDetailBean>

    @GET("/c/c/post/addPollPost")
    fun addPollPost(
        @Query("forum_id") forumId: Long?,
        @Query("thread_id") threadId: Long,
        @Query("options") options: String,
    ): Flow<CommonResponse>
}
