package com.huanchengfly.tieba.post.api.models.web

import com.google.gson.annotations.SerializedName

data class MaterialThreadRankResponse(
    @SerializedName("error_code") val errorCode: Int = -1,
    @SerializedName("error_msg") val errorMessage: String = "",
    val data: MaterialThreadRankData? = null,
)

data class MaterialThreadRankData(
    @SerializedName("current_tab_code") val currentTabCode: String = "",
    @SerializedName("current_timestamp") val currentTimestamp: String = "",
    @SerializedName("tab_list") val tabList: List<MaterialHomeThreadRankTab> = emptyList(),
)
