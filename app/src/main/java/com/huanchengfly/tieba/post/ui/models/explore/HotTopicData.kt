package com.huanchengfly.tieba.post.ui.models.explore

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Immutable
import com.huanchengfly.tieba.post.ui.models.ThreadItem

data class HotRankTopic(
    val topicId: Long,
    val topicName: String,
    val tag: Int,
    val discussNum: Long,
)

@Immutable
data class MaterialThreadRankCard(
    val tabCode: String,
    val title: String,
    val moreText: String,
    val sortRule: String,
    val artworkUrls: List<String>,
    val threads: List<MaterialThreadRankItem>,
)

@Immutable
data class MaterialThreadRankItem(
    val threadId: Long,
    val firstPostId: Long,
    val forumId: Long,
    val forumName: String,
    val title: String,
    val abstractText: String,
    val metricLabel: String,
    val agreeNum: Long,
    val viewNum: Long,
    val replyNum: Long,
    val liked: Boolean,
    val likeLoading: Boolean = false,
    val forumAvatarUrl: String,
    val media: List<MaterialThreadRankMedia>,
)

@Immutable
data class MaterialThreadRankMedia(
    val url: String,
    val width: Int,
    val height: Int,
)

@Immutable
data class MaterialThreadRankTab(
    val tabCode: String,
    val title: String,
    val sortRule: String,
)

@Immutable
data class MaterialThreadRankPageData(
    val currentTabCode: String,
    val tabs: List<MaterialThreadRankTab>,
    val rankCard: MaterialThreadRankCard,
    val updatedAtMillis: Long?,
)

@Stable
data class HotTab(val name: String, val tabCode: String, var isLoading: Boolean = false)

class HotTopicData(
    val topics: List<HotRankTopic>,
    val tabs: List<HotTab>,
    val threads: List<ThreadItem>,
)
