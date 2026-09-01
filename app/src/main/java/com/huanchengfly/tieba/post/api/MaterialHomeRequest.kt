package com.huanchengfly.tieba.post.api

import com.huanchengfly.tieba.post.api.models.protos.CommonRequest

internal const val MATERIAL_HOME_HYBRID_VERSION = "22.10.1.0"

internal fun materialHomeParams(common: CommonRequest): Map<String, String> = buildMap {
    put("subapp_type", "tieba")
    put("_client_type", common._client_type.toString())
    put("_client_version", MATERIAL_HOME_HYBRID_VERSION)
    put("_client_id", common._client_id)
    put("from", "tieba")
    put("cuid", common.cuid)
    put("cuid_galaxy2", common.cuid_galaxy2)
    put("c3_aid", common.c3_aid)
    put("cuid_gid", common.cuid_gid.orEmpty())
    put("_timestamp", common._timestamp.toString())
    put("user_agent", common.user_agent)
    put("BDUSS", common.BDUSS.orEmpty())
    put("stoken", common.stoken.orEmpty())
    put("tbs", common.tbs.orEmpty())
    put("applist", common.applist.orEmpty())
    put("pversion", common.pversion)
    put("lego_lib_version", common.lego_lib_version)
    put("z_id", common.z_id.orEmpty())
    put("net_type", common.net_type.toString())
    put("sample_id", common.sample_id.orEmpty())
    put("is_teenager", (common.is_teenager ?: 0).toString())
    put("sdk_ver", common.sdk_ver)
    put("framework_ver", common.framework_ver)
    put("naws_game_ver", common.swan_game_ver)
    put("q_type", (common.q_type ?: 0).toString())
    put("scr_h", common.scr_h.toString())
    put("scr_w", common.scr_w.toString())
    put("scr_dip", common.scr_dip.toString())
    put("active_timestamp", common.active_timestamp.toString())
    put("first_install_time", common.first_install_time.toString())
    put("last_update_time", common.last_update_time.toString())
    put("event_day", common.event_day)
    put("cmode", common.cmode.toString())
    put("start_type", common.start_type.toString())
    put("start_scheme", common.start_scheme.orEmpty())
    put("extra", common.extra.orEmpty())
    put("personalized_rec_switch", common.personalized_rec_switch.toString())
    put("device_score", common.device_score)
    put("package_version", "hybrid-main-pb_1.0.375.2")
}

internal fun materialThreadRankParams(common: CommonRequest): Map<String, String> =
    materialHomeParams(common).toMutableMap().apply {
        put("subapp_type", "hybrid")
    }

internal fun materialHomeCookie(
    savedCookie: String,
    bduss: String,
    stoken: String,
): String = forumSquareRecommendCookie(savedCookie, bduss, stoken)
