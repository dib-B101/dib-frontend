package com.ssafy.dib.data.remote.live

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class LiveRemoteDataSource(private val client: DibHttpClient) {
    fun getFeed(size: Int): ApiResult<LiveFeedResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/feed"
        val url = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 50).toString()).build()
        client.execute(client.requestBuilder(path).url(url).get().build(), LiveFeedResponse.serializer())
    }

    fun getDetail(liveBroadcastId: String): ApiResult<LiveBroadcastDetailResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId"
        client.execute(client.requestBuilder(path).get().build(), LiveBroadcastDetailResponse.serializer())
    }

    fun getMessages(liveBroadcastId: String, beforeLiveChattingId: String?, size: Int): ApiResult<LiveChatMessageListResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId/messages"
        val urlBuilder = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 100).toString())
        beforeLiveChattingId?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("beforeLiveChattingId", it) }
        client.execute(client.requestBuilder(path).url(urlBuilder.build()).get().build(), LiveChatMessageListResponse.serializer())
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (error: RuntimeException) {
        ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
    }
}
