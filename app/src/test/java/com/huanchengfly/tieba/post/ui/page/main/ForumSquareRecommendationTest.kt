package com.huanchengfly.tieba.post.ui.page.main

import com.google.gson.Gson
import com.huanchengfly.tieba.post.api.forumSquareRecommendCookie
import com.huanchengfly.tieba.post.api.forumSquareRecommendParams
import com.huanchengfly.tieba.post.api.models.ForumSquareRecommendResponse
import com.huanchengfly.tieba.post.api.models.protos.CommonRequest
import com.huanchengfly.tieba.post.api.models.protos.forumSquare.ForumSquareResponseData
import com.huanchengfly.tieba.post.api.retrofit.adapter.FlowCallAdapterFactory
import com.huanchengfly.tieba.post.api.retrofit.converter.gson.GsonConverterFactory
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.AddWebCookieInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.CommonParamInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.SortAndSignInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interfaces.AppHybridTiebaApi
import com.huanchengfly.tieba.post.repository.ForumSquareRecommendationUnavailable
import com.huanchengfly.tieba.post.repository.toSquareResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException

class ForumSquareRecommendationTest {
    // Entirely synthetic fixtures: no account or device values from private HARs.
    private fun decode(json: String) = Gson().fromJson(json, ForumSquareRecommendResponse::class.java)
    private val emptyPage = """{"error_code":"0","class_name":"推荐","forum_info":[],"page":{"has_more":"0"}}"""

    @Test fun flatJsonAcceptsQuotedNumbersAndHasMoreWithoutCurrentPage() {
        val result = decode("""{
            "error_code":"0","class_name":"推荐","page_structure":["推荐","热门"],
            "forum_info":[{"forum_id":"123","forum_name":"测试吧","avatar":"https://example.invalid/a.png",
                "is_like":"1","member_count":"42","thread_count":"9","slogan":"测试","recom_reason":"示例"}],
            "page":{"has_more":"1"},"second_class_list":[]
        }""").toSquareResult()
        assertEquals("推荐", result.category)
        assertEquals(listOf("推荐", "热门"), result.categories)
        assertTrue(result.hasMore)
        assertNull(result.serverCurrentPage)
        val forum = result.forums.single()
        assertEquals(123L, forum.forum_id)
        assertEquals("测试吧", forum.forum_name)
        assertEquals(1, forum.is_like)
        assertEquals(42, forum.member_count)
        assertEquals(9, forum.thread_count)
    }

    @Test fun numericFlagsAndEmptyFinalPageAreAccepted() {
        val result = decode(emptyPage.replace("\"0\"", "0")).toSquareResult()
        assertFalse(result.hasMore)
        assertTrue(result.forums.isEmpty())
    }

    @Test fun explicitServerPageIsNotDiscarded() {
        val result = decode(emptyPage.replace("\"has_more\":\"0\"", "\"has_more\":\"0\",\"current_page\":\"3\""))
            .toSquareResult()
        assertEquals(3, result.serverCurrentPage)
    }

    @Test fun hotFallbackIsNotPresentedAsRecommendation() {
        assertThrows(ForumSquareRecommendationUnavailable::class.java) {
            decode(emptyPage.replace("推荐", "热门")).toSquareResult()
        }
    }

    @Test fun missingOrInvalidPagingDoesNotSilentlyEndPagination() {
        for (page in listOf("null", "{}", "{\"has_more\":null}", "{\"has_more\":2}")) {
            assertThrows(IOException::class.java) {
                decode("""{"error_code":0,"class_name":"推荐","forum_info":[],"page":$page}""").toSquareResult()
            }
        }
    }

    @Test fun missingStatusOrForumListIsAnError() {
        assertThrows(IOException::class.java) { decode("{}").toSquareResult() }
        assertThrows(IOException::class.java) {
            decode(emptyPage.replace("\"forum_info\":[],", "")).toSquareResult()
        }
    }

    @Test fun serverErrorIsNotTreatedAsAnEmptySuccess() {
        assertThrows(TiebaApiException::class.java) {
            decode("""{"error_code":"123","error_msg":"测试错误"}""").toSquareResult()
        }
    }

    @Test fun regularCategoriesStillRequireProtobufPaging() {
        assertThrows(IOException::class.java) { ForumSquareResponseData(category = "游戏").toSquareResult() }
    }

    @Test fun paramsUseCurrentSnapshotAndIndependentPageSize() {
        val common = CommonRequest(BDUSS = "test-session", stoken = "test-token", tbs = "test-tbs",
            _client_type = 2, _client_id = "test-device", _timestamp = 1234, user_agent = "test-agent")
        val params = forumSquareRecommendParams(common, 2)
        assertEquals("推荐", params["class_name"])
        assertEquals("全部", params["second_class_name"])
        assertEquals("2", params["pn"])
        assertEquals("30", params["rn"])
        assertEquals("client_fe", params["subapp_type"])
        assertEquals("22.10.1.0", params["_client_version"])
        assertEquals("test-session", params["BDUSS"])
        assertEquals("test-token", params["stoken"])
        assertEquals("test-tbs", params["tbs"])
        assertEquals("test-device", params["_client_id"])
        assertEquals("1234", params["_timestamp"])
        assertFalse(params.containsKey("cmd"))
        assertFalse(params.containsKey("format"))
        assertThrows(IllegalArgumentException::class.java) { forumSquareRecommendParams(common, 0) }
        assertThrows(IllegalArgumentException::class.java) { forumSquareRecommendParams(CommonRequest(), 1) }
    }

    @Test fun cookiesKeepAuthenticationConsistentWithQuerySnapshot() {
        val cookie = forumSquareRecommendCookie(
            "BAIDUID=test-id; BDUSS=stale; STOKEN=stale-token; BDUSS_BFESS=stale;", "test-session", "test-token")
        assertEquals("BAIDUID=test-id; BDUSS=test-session; STOKEN=test-token; BDUSS_BFESS=test-session", cookie)
    }

    @Test fun retrofitBuildsSignedGetWithoutReplacingSnapshotCredentials() = runTest {
        var request: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(AddWebCookieInterceptor)
            .addInterceptor(CommonParamInterceptor(
                "BDUSS" to { "other-account-session" },
                "stoken" to { "other-account-token" },
            ))
            .addInterceptor(SortAndSignInterceptor("tiebaclient!!!"))
            .addInterceptor { chain ->
                request = chain.request()
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(200).message("OK")
                    .body(emptyPage.toResponseBody("application/x-javascript;charset=utf-8".toMediaType())).build()
            }.build()
        val api = Retrofit.Builder().baseUrl("https://tieba.baidu.com/")
            .addCallAdapterFactory(FlowCallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(client).build().create(AppHybridTiebaApi::class.java)
        val result = api.forumSquareRecommendFlow(
            forumSquareRecommendParams(CommonRequest(BDUSS = "test-session", stoken = "test-token"), 2),
            "BDUSS=test-session; STOKEN=test-token", "test-agent",
        ).first().toSquareResult()
        assertFalse(result.hasMore)
        val sent = requireNotNull(request)
        assertEquals("GET", sent.method)
        assertEquals("/c/f/forum/getForumSquare", sent.url.encodedPath)
        assertEquals("推荐", sent.url.queryParameter("class_name"))
        assertEquals("2", sent.url.queryParameter("pn"))
        assertEquals("test-session", sent.url.queryParameter("BDUSS"))
        assertEquals(1, sent.url.queryParameterValues("BDUSS").size)
        assertEquals("BDUSS=test-session; STOKEN=test-token", sent.header("Cookie"))
        assertEquals("test-agent", sent.header("User-Agent"))
        assertEquals(32, sent.url.queryParameter("sign")?.length)
        assertNull(sent.header("add_cookie"))
        assertNull(sent.header("no_common_params"))
        assertNull(sent.body)
    }
}
