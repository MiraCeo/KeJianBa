package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.ClientVersion
import com.huanchengfly.tieba.post.api.FORUM_SQUARE_HYBRID_VERSION
import com.huanchengfly.tieba.post.api.buildCommonRequest
import com.huanchengfly.tieba.post.api.buildProtobufRequestBody
import com.huanchengfly.tieba.post.api.forumSquareRecommendCookie
import com.huanchengfly.tieba.post.api.forumSquareRecommendParams
import com.huanchengfly.tieba.post.api.getUserAgent
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.protos.RecommendForumInfo
import com.huanchengfly.tieba.post.api.models.protos.forumSquare.ForumSquareRequest
import com.huanchengfly.tieba.post.api.models.protos.forumSquare.ForumSquareRequestData
import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.CancellationException
import java.io.IOException
import javax.inject.Inject
import com.huanchengfly.tieba.post.repository.source.network.ForumNetworkDataSource

class ForumSquareRepository @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    suspend fun load(category: String, page: Int, accountUid: Long?): ForumSquareResult {
        require(category.isNotBlank() && page >= 1)
        checkAccount(accountUid)
        if (category == "推荐") return loadRecommendation(page, accountUid)
        val version = ClientVersion.TIEBA_V12_POST
        val request = ForumSquareRequest(data_ = ForumSquareRequestData(
            common = buildCommonRequest(clientVersion = version),
            category = category, page = page, page_size = 20, unknown_flag = 0,
        ))
        val response = RetrofitTiebaApi.OFFICIAL_PROTOBUF_TIEBA_POST_API
            .forumSquareFlow(buildProtobufRequestBody(request, version))
            .firstOrThrow()
        checkAccount(accountUid)
        val error = response.error ?: throw IOException("吧广场响应缺少状态信息")
        if (error.error_code != 0) {
            throw TiebaApiException(CommonResponse(error.error_code, error.error_msg))
        }
        return (response.data_ ?: throw IOException("吧广场响应缺少数据")).toSquareResult()
    }

    private suspend fun loadRecommendation(page: Int, accountUid: Long?): ForumSquareResult {
        val account = AccountUtil.getLoginInfo() ?: throw ForumSquareRecommendationLoginRequired()
        if (account.uid != accountUid) throw CancellationException("Account changed")
        if (account.bduss.isBlank()) throw ForumSquareRecommendationLoginRequired()
        val userAgent = getUserAgent("tieba/$FORUM_SQUARE_HYBRID_VERSION skin/default")
        val common = buildCommonRequest(clientVersion = ClientVersion.TIEBA_V12_POST).copy(
            BDUSS = account.bduss, stoken = account.sToken, tbs = account.tbs,
            z_id = account.zid, user_agent = userAgent,
        )
        checkAccount(accountUid)
        val response = RetrofitTiebaApi.HYBRID_TIEBA_API.forumSquareRecommendFlow(
            params = forumSquareRecommendParams(common, page),
            cookie = forumSquareRecommendCookie(account.cookie, account.bduss, account.sToken),
            userAgent = userAgent,
        ).firstOrThrow()
        checkAccount(accountUid)
        return response.toSquareResult()
    }

    suspend fun toggleFollow(forum: RecommendForumInfo, accountUid: Long?): Boolean {
        val account = AccountUtil.getLoginInfo() ?: throw ForumSquareRecommendationLoginRequired()
        if (account.uid != accountUid || account.tbs.isBlank()) throw CancellationException("Account changed")
        return if (forum.is_like == 0) {
            ForumNetworkDataSource.like(forum.forum_id, forum.forum_name, account.tbs)
            homeRepository.refresh(cached = false)
            true
        } else {
            ForumNetworkDataSource.dislike(forum.forum_id, forum.forum_name, account.tbs)
            homeRepository.onDislikeForum(forum.forum_id)
            false
        }
    }

    private fun checkAccount(uid: Long?) {
        if (AccountUtil.getUid()?.toLongOrNull() != uid) {
            throw CancellationException("Account changed")
        }
    }
}
