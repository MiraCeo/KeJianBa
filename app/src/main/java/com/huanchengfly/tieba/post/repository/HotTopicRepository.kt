package com.huanchengfly.tieba.post.repository

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.huanchengfly.tieba.post.api.models.ThreadBean
import com.huanchengfly.tieba.post.api.models.ThreadInfoBean
import com.huanchengfly.tieba.post.api.models.TopicInfoBean
import com.huanchengfly.tieba.post.api.models.protos.Media
import com.huanchengfly.tieba.post.api.models.protos.topicList.NewTopicList
import com.huanchengfly.tieba.post.api.models.web.MaterialHomeThreadRankTab
import com.huanchengfly.tieba.post.repository.source.network.HotTopicNetworkDataSource
import com.huanchengfly.tieba.post.repository.user.Settings
import com.huanchengfly.tieba.post.repository.user.SettingsRepository
import com.huanchengfly.tieba.post.ui.models.Author
import com.huanchengfly.tieba.post.ui.models.Like
import com.huanchengfly.tieba.post.ui.models.SimpleForum
import com.huanchengfly.tieba.post.ui.models.ThreadItem
import com.huanchengfly.tieba.post.ui.models.settings.HabitSettings
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankCard
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankItem
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankMedia
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankPageData
import com.huanchengfly.tieba.post.ui.models.explore.MaterialThreadRankTab
import com.huanchengfly.tieba.post.ui.widgets.compose.buildThreadContent
import com.huanchengfly.tieba.post.utils.DateTimeUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HotTopicRepository @Inject constructor(
    private val networkDataSource: HotTopicNetworkDataSource,
    private val blockRepo: BlockRepository,
    private val threadRepo: PbPageRepository,
    private val settingsRepo: SettingsRepository
) {

    val habitSettings: Settings<HabitSettings>
        get() = settingsRepo.habitSettings

    suspend fun loadTopicList(): List<NewTopicList> {
        return networkDataSource.topicList().topic_list
    }

    suspend fun loadMaterialThreadRanks(): List<MaterialThreadRankCard> {
        val tabs = networkDataSource.materialHome()
            .activityInfo
            ?.threadRank
            ?.tabList
            .orEmpty()
            .associateBy { it.tabCode }
        return listOf(THREAD_RANK_AGREE, THREAD_RANK_VIEW).mapNotNull { tabCode ->
            val tab = tabs[tabCode] ?: return@mapNotNull null
            tab.toMaterialThreadRankCard(limit = 5)
        }.filter { it.threads.isNotEmpty() }
    }

    suspend fun loadMaterialThreadRankPage(
        tabCode: String,
        includeTabs: Boolean,
    ): MaterialThreadRankPageData {
        require(tabCode in MATERIAL_THREAD_RANK_TAB_CODES)
        val data = networkDataSource.materialThreadRank(tabCode, includeTabs)
        val currentTabCode = data.currentTabCode.ifEmpty { tabCode }
        val currentTab = data.tabList.firstOrNull {
            it.tabCode == currentTabCode && it.threadList.isNotEmpty()
        } ?: throw IllegalStateException("完整帖子榜响应缺少当前榜单：$currentTabCode")
        return MaterialThreadRankPageData(
            currentTabCode = currentTabCode,
            tabs = data.tabList
                .filter { it.tabCode in MATERIAL_THREAD_RANK_TAB_CODES }
                .map { tab ->
                    MaterialThreadRankTab(
                        tabCode = tab.tabCode,
                        title = materialThreadRankTitle(tab.tabCode, tab.name),
                        sortRule = tab.sortRule,
                    )
                },
            rankCard = currentTab.toMaterialThreadRankCard(),
            updatedAtMillis = data.currentTimestamp.toLongOrNull()?.times(1_000L),
        )
    }

    private fun MaterialHomeThreadRankTab.toMaterialThreadRankCard(
        limit: Int? = null,
    ): MaterialThreadRankCard {
        val displayedThreads = limit?.let(threadList::take) ?: threadList
        return MaterialThreadRankCard(
            tabCode = tabCode,
            title = materialThreadRankTitle(tabCode, name),
            moreText = moreText,
            sortRule = sortRule,
            artworkUrls = pictureUrl.split('|').filter(String::isNotBlank),
            threads = displayedThreads.mapIndexed { index, thread ->
                MaterialThreadRankItem(
                    threadId = thread.id,
                    firstPostId = thread.firstPostId,
                    forumId = thread.fid,
                    forumName = thread.fname,
                    title = thread.title,
                    abstractText = thread.abstractContent
                        .asSequence()
                        .filter { it.type == 0 }
                        .map { it.text.trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString("\n")
                        .takeUnless { it == thread.title.trim() }
                        .orEmpty(),
                    metricLabel = labelList.getOrNull(index).orEmpty(),
                    agreeNum = thread.agreeNum,
                    viewNum = thread.viewNum,
                    replyNum = thread.replyNum,
                    liked = thread.agree?.hasAgree == 1,
                    forumAvatarUrl = thread.forumInfo?.avatar.orEmpty(),
                    media = thread.media.mapNotNull { media ->
                        media.bigPicture.ifEmpty { media.sourcePicture }
                            .takeIf(String::isNotEmpty)
                            ?.let { url ->
                                MaterialThreadRankMedia(
                                    url = url,
                                    width = media.width,
                                    height = media.height,
                                )
                            }
                    },
                )
            },
        )
    }

    private fun materialThreadRankTitle(tabCode: String, fallback: String): String =
        when (tabCode) {
            THREAD_RANK_AGREE -> "点赞最多贴"
            THREAD_RANK_VIEW -> "浏览最多贴"
            else -> fallback
        }

    suspend fun loadTopicDetail(
        topicId: Long,
        topicName: String,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        offset: Int = 0,
        lastId: Long? = null,
    ): TopicDetailData {
        require(topicId > 0)
        require(offset >= 0)
        require(lastId == null || lastId > 0) {
            "The lastId is expected to be null or greater than zero, current: $lastId"
        }

        val start = System.currentTimeMillis()
        val data = networkDataSource.topicDetail(
            topicId = topicId,
            topicName = topicName,
            isNew = 1,
            isShare = 1,
            page = page,
            pageSize = pageSize,
            offset = offset,
            lastId = lastId?.toString().orEmpty()
        )
        val result = TopicDetailData(
            topicInfo = data.topicInfo,
            threads = mapRelateThreadToUiModel(threads = data.relateThread.threadList),
            hasMore = data.hasMore,
            page = data.wreq.page,
            pageSize = data.wreq.pageSize
        )
        val cost = System.currentTimeMillis() - start
        val size = data.relateThread.threadList.size
        Log.i(TAG, "onLoadTopicDetailInternal: Done, size: $size, ID: $topicId, page $page, cost ${cost}ms")
        return result
    }

    suspend fun loadTopicDetailMore(
        topicId: Long,
        topicName: String,
        page: Int,
        pageSize: Int = DEFAULT_PAGE_SIZE,
        lastId: Long,
    ): TopicDetailData {
        val offset = (page - 1) * pageSize
        return loadTopicDetail(topicId, topicName, page, pageSize, offset, lastId)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    suspend fun mapRelateThreadToUiModel(threads: List<ThreadBean>): List<ThreadItem> {
        if (threads.isEmpty()) return emptyList()

        return withContext(Dispatchers.Default) {
            val habit = habitSettings.snapshot()
            threads.map {
                it.threadInfo.mapUiModel(showBothName = habit.showBothName, isBlocked = blockRepo::isBlocked)
            }
        }
    }

    suspend fun onLikeThread(thread: ThreadItem) = threadRepo.requestLikeThread(thread)

    suspend fun setMaterialThreadLiked(threadId: Long, firstPostId: Long, liked: Boolean) {
        threadRepo.setThreadLiked(threadId = threadId, firstPostId = firstPostId, liked = liked)
    }

    companion object {
        private const val TAG = "HotTopicRepository"
        const val THREAD_RANK_AGREE = "thread_agree_num"
        const val THREAD_RANK_VIEW = "thread_uv_num"
        val MATERIAL_THREAD_RANK_TAB_CODES = setOf(THREAD_RANK_AGREE, THREAD_RANK_VIEW)

        const val DEFAULT_PAGE_SIZE: Int = 10

        class TopicDetailData(
            val topicInfo: TopicInfoBean,
            val threads: List<ThreadItem>,
            val hasMore: Boolean,
            val page: Int,
            val pageSize: Int,
        )

        /**
         * Convert ThreadInfoBean to UI Model.
         *
         * @param showBothName show both username and nickname
         * @param isBlocked check thread author, title or content is blocked
         * */
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        suspend fun ThreadInfoBean.mapUiModel(
            showBothName: Boolean,
            isBlocked: suspend (forumName: String, uid: Long, content: Array<String>) -> Boolean,
        ): ThreadItem {
            val author = with(author) {
                val nameShow = StringUtil.getUserNameString(showBothName, name ?: nameShow, showNickName)
                Author(id = this.id, name = nameShow, avatarUrl = StringUtil.getAvatarUrl(portrait))
            }
            val title = this.title.orEmpty()

            return ThreadItem(
                id = this.id,
                firstPostId = this.firstPostId,
                author = author,
                blocked = isBlocked(forumName, author.id, arrayOf(title, abstractText)),
                content = buildThreadContent(title, abstractText),
                title = title,
                lastTimeMill = DateTimeUtils.fixTimestamp(lastTimeInt),
                like = this.agree.run { Like(liked = hasAgree == 1, count = agreeNum.toLong()) },
                replyNum = this.replyNum,
                shareNum = this.shareNum,
                medias = this.media.map {
                    // Use Protobuf Media quality level: srcPic > bigPic
                    val bigPic = it.smallPic
                    val originPic = it.bigPic
                    Media(
                        type = it.type.toIntOrNull() ?: 0,
                        bigPic = bigPic,
                        srcPic = originPic,
                        originPic = originPic,
                        width = it.width.toIntOrNull() ?: 0,
                        height = it.height.toIntOrNull() ?: 0,
                        isLongPic = it.isLongPic
                    )
                },
                simpleForum = SimpleForum(forumId, forumName, null),
            )
        }
    }

    data class TopicListResult(
        val topics: List<NewTopicList>,
        val updatedAtMillis: Long?,
    )
}
