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

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (error: RuntimeException) {
        ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
    }
}
