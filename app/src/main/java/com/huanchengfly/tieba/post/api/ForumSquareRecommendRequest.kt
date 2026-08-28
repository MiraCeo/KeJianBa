package com.huanchengfly.tieba.post.api

import com.huanchengfly.tieba.post.api.models.protos.CommonRequest

// Endpoint-local compatibility version from the successful hybrid capture.
// Do not change the client version used by other APIs.
internal const val FORUM_SQUARE_HYBRID_VERSION = "22.10.1.0"

internal fun forumSquareRecommendParams(common: CommonRequest, page: Int): Map<String, String> {
    require(page >= 1)
    require(!common.BDUSS.isNullOrBlank())
    return buildMap {
        put("class_name", "推荐")
        put("second_class_name", "全部")
        put("pn", page.toString())
        put("rn", "30")
        put("subapp_type", "client_fe")
        put("_client_type", common._client_type.toString())
        put("_client_version", FORUM_SQUARE_HYBRID_VERSION)
        put("from", "tieba")
        put("package_version", "")
        put("BDUSS", common.BDUSS.orEmpty())
        put("stoken", common.stoken.orEmpty())
        put("tbs", common.tbs.orEmpty())
        put("_client_id", common._client_id)
        put("_timestamp", common._timestamp.toString())
        put("cuid", common.cuid)
        put("cuid_galaxy2", common.cuid_galaxy2)
        put("cuid_gid", common.cuid_gid.orEmpty())
        put("c3_aid", common.c3_aid)
        put("z_id", common.z_id.orEmpty())
        put("user_agent", common.user_agent)
        put("personalized_rec_switch", common.personalized_rec_switch.toString())
        put("applist", common.applist.orEmpty())
        put("pversion", common.pversion)
        put("lego_lib_version", common.lego_lib_version)
        put("net_type", common.net_type.toString())
        put("sample_id", common.sample_id.orEmpty())
        put("is_teenager", (common.is_teenager ?: 0).toString())
        put("q_type", (common.q_type ?: 0).toString())
        put("scr_h", common.scr_h.toString())
        put("scr_w", common.scr_w.toString())
        put("scr_dip", common.scr_dip.toString())
        put("sdk_ver", common.sdk_ver)
        put("framework_ver", common.framework_ver)
        put("naws_game_ver", common.swan_game_ver)
        put("active_timestamp", common.active_timestamp.toString())
        put("first_install_time", common.first_install_time.toString())
        put("last_update_time", common.last_update_time.toString())
        put("event_day", common.event_day)
        put("cmode", common.cmode.toString())
        put("start_type", common.start_type.toString())
        put("start_scheme", common.start_scheme.orEmpty())
        put("extra", common.extra.orEmpty())
        put("device_score", common.device_score)
    }
}

internal fun forumSquareRecommendCookie(savedCookie: String, bduss: String, stoken: String): String {
    // Preserve the account's other web cookies, but keep authentication consistent
    // with the same account snapshot used by the query parameters.
    val cookies = savedCookie.split(';').map { it.trim() }.filter { it.contains('=') }
        .filterNot { it.substringBefore('=').uppercase() in setOf("BDUSS", "STOKEN", "BDUSS_BFESS") }
    return (cookies + listOf("BDUSS=$bduss", "STOKEN=$stoken", "BDUSS_BFESS=$bduss"))
        .joinToString("; ")
}
