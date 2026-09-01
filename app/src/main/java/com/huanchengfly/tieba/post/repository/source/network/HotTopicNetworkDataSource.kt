package com.huanchengfly.tieba.post.repository.source.network

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.ClientVersion
import com.huanchengfly.tieba.post.api.MATERIAL_HOME_HYBRID_VERSION
import com.huanchengfly.tieba.post.api.buildCommonRequest
import com.huanchengfly.tieba.post.api.getUserAgent
import com.huanchengfly.tieba.post.api.materialHomeCookie
import com.huanchengfly.tieba.post.api.materialHomeParams
import com.huanchengfly.tieba.post.api.materialThreadRankParams
import com.huanchengfly.tieba.post.api.models.CommonResponse
import com.huanchengfly.tieba.post.api.models.TopicDetailDataBean
import com.huanchengfly.tieba.post.api.models.protos.topicList.TopicListResponseData
import com.huanchengfly.tieba.post.api.models.web.MaterialHomeData
import com.huanchengfly.tieba.post.api.models.web.MaterialThreadRankData
import com.huanchengfly.tieba.post.api.retrofit.RetrofitTiebaApi
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.arch.firstOrThrow
import com.huanchengfly.tieba.post.repository.source.network.ExploreNetworkDataSource.commonResponse
import com.huanchengfly.tieba.post.utils.AccountUtil
import javax.inject.Inject

/**
 * Main entry point for accessing topic data from the network.
 */
interface HotTopicNetworkDataSource {

    /**
     * 话题榜
     */
    suspend fun topicList(): TopicListResponseData

    suspend fun materialHome(): MaterialHomeData

    suspend fun materialThreadRank(tabCode: String, includeTabs: Boolean): MaterialThreadRankData

    /**
     * 话题详情
     *
     * @param topicId 话题id
     * @param topicName 话题名
     * @param isNew
     * @param isShare
     * @param page 分页页码(初始为1)
     * @param pageSize 分页大小
     * @param offset （分页页码-1）* 分页大小
     * @param lastId 上次返回的最后一个feedid，初次请求留空
     */
    suspend fun topicDetail(
        topicId: Long,
        topicName: String,
        isNew: Int,
        isShare: Int,
        page: Int,
        pageSize: Int,
        offset: Int,
        lastId: String,
    ): TopicDetailDataBean
}

class HotTopicNetworkDataSourceImpl @Inject constructor(): HotTopicNetworkDataSource {

    override suspend fun topicList(): TopicListResponseData {
        return TiebaApi.getInstance()
            .topicListFlow()
            .firstOrThrow()
            .run {
                data_ ?: throw TiebaApiException(commonResponse = this.error.commonResponse)
            }
    }

    override suspend fun materialHome(): MaterialHomeData {
        val account = AccountUtil.getLoginInfo()
        val userAgent = getUserAgent("tieba/$MATERIAL_HOME_HYBRID_VERSION skin/default")
        val common = buildCommonRequest(
            clientVersion = ClientVersion.TIEBA_V12_POST,
            tbs = account?.tbs,
        ).copy(
            BDUSS = account?.bduss.orEmpty(),
            stoken = account?.sToken.orEmpty(),
            tbs = account?.tbs.orEmpty(),
            user_agent = userAgent,
        )
        val response = RetrofitTiebaApi.HYBRID_TIEBA_API
            .materialHomeFlow(
                params = materialHomeParams(common),
                cookie = materialHomeCookie(
                    savedCookie = account?.cookie.orEmpty(),
                    bduss = account?.bduss.orEmpty(),
                    stoken = account?.sToken.orEmpty(),
                ),
                userAgent = userAgent,
            )
            .firstOrThrow()
        if (response.errorCode != 0) {
            throw TiebaApiException(CommonResponse(response.errorCode, response.errorMessage))
        }
        return response.data
            ?: throw TiebaApiException(CommonResponse(-1, "有料首页响应缺少数据"))
    }

    override suspend fun materialThreadRank(
        tabCode: String,
        includeTabs: Boolean,
    ): MaterialThreadRankData {
        val account = AccountUtil.getLoginInfo()
        val userAgent = getUserAgent("tieba/$MATERIAL_HOME_HYBRID_VERSION skin/default")
        val common = buildCommonRequest(
            clientVersion = ClientVersion.TIEBA_V12_POST,
            tbs = account?.tbs,
        ).copy(
            BDUSS = account?.bduss.orEmpty(),
            stoken = account?.sToken.orEmpty(),
            tbs = account?.tbs.orEmpty(),
            user_agent = userAgent,
        )
        val response = RetrofitTiebaApi.HYBRID_TIEBA_API
            .materialThreadRankFlow(
                params = materialThreadRankParams(common),
                tabCode = tabCode,
                includeTabs = if (includeTabs) 1 else 0,
                cookie = materialHomeCookie(
                    savedCookie = account?.cookie.orEmpty(),
                    bduss = account?.bduss.orEmpty(),
                    stoken = account?.sToken.orEmpty(),
                ),
                userAgent = userAgent,
            )
            .firstOrThrow()
        if (response.errorCode != 0) {
            throw TiebaApiException(CommonResponse(response.errorCode, response.errorMessage))
        }
        return response.data
            ?: throw TiebaApiException(CommonResponse(-1, "完整帖子榜响应缺少数据"))
    }

    override suspend fun topicDetail(
        topicId: Long,
        topicName: String,
        isNew: Int,
        isShare: Int,
        page: Int,
        pageSize: Int,
        offset: Int,
        lastId: String,
    ): TopicDetailDataBean {
        return TiebaApi.getInstance()
            .topicDetailFlow(
                topicId = topicId.toString(),
                topicName = topicName,
                isNew = isNew,
                isShare = isShare,
                page = page,
                pageSize = pageSize,
                offset = offset,
                lastId = lastId
            )
            .firstOrThrow()
            .run {
                if (errorCode == 0) data else throw TiebaApiException(CommonResponse(errorCode, errorMsg))
            }
    }
}
