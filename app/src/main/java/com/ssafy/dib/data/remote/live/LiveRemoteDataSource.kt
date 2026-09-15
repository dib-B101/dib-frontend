package com.ssafy.dib.data.remote.live

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.RequestBody

class LiveRemoteDataSource(private val client: DibHttpClient) {
    fun getFeed(cursor: String?, size: Int): ApiResult<LiveFeedResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/feed"
        val urlBuilder = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 50).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        val url = urlBuilder.build()
        client.execute(client.requestBuilder(path).url(url).get().build(), LiveFeedResponse.serializer())
    }

    fun getDetail(liveBroadcastId: String): ApiResult<LiveBroadcastDetailResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId"
        client.execute(client.requestBuilder(path).get().build(), LiveBroadcastDetailResponse.serializer())
    }

    fun getMine(status: String?, cursor: String?, size: Int): ApiResult<LiveBroadcastListResponse> = configured {
        val path = "${ApiRoutes.MEMBERS_ME}/live-broadcasts"
        val url = client.urlBuilder(path).apply {
            status?.takeIf(String::isNotBlank)?.let { addQueryParameter("status", it) }
            cursor?.takeIf(String::isNotBlank)?.let { addQueryParameter("cursor", it) }
            addQueryParameter("size", size.coerceIn(1, 100).toString())
        }.build()
        client.execute(client.requestBuilder(path).url(url).get().build(), LiveBroadcastListResponse.serializer())
    }

    fun create(title: String, description: String?, scheduledAt: String, streamUrl: String?, idempotencyKey: String): ApiResult<CreateLiveBroadcastResponse> = configured {
        val body = CreateLiveBroadcastRequest(title, description?.takeIf(String::isNotBlank), scheduledAt, streamUrl?.takeIf(String::isNotBlank))
        client.execute(
            client.requestBuilder(ApiRoutes.LIVE_BROADCASTS)
                .header("Idempotency-Key", idempotencyKey)
                .post(client.jsonBody(body, CreateLiveBroadcastRequest.serializer()))
                .build(),
            CreateLiveBroadcastResponse.serializer()
        )
    }

    fun setItems(liveBroadcastId: String, auctionIds: List<String>): ApiResult<SetLiveItemsResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId/items"
        val body = SetLiveItemsRequest(auctionIds.map { id -> id.toLongOrNull()?.let(::JsonPrimitive) ?: JsonPrimitive(id) })
        client.execute(
            client.requestBuilder(path).put(client.jsonBody(body, SetLiveItemsRequest.serializer())).build(),
            SetLiveItemsResponse.serializer()
        )
    }

    fun prepareStream(liveBroadcastId: String, idempotencyKey: String): ApiResult<LiveStreamSessionResponse> = postCommand(
        path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId/stream-session",
        idempotencyKey = idempotencyKey,
        serializer = LiveStreamSessionResponse.serializer()
    )

    fun start(liveBroadcastId: String, idempotencyKey: String): ApiResult<StartLiveBroadcastResponse> = postCommand(
        path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId/start",
        idempotencyKey = idempotencyKey,
        serializer = StartLiveBroadcastResponse.serializer()
    )

    fun startAuction(liveBroadcastId: String, auctionId: String, idempotencyKey: String): ApiResult<StartLiveAuctionResponse> = postCommand(
        path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId/auctions/$auctionId/start",
        idempotencyKey = idempotencyKey,
        serializer = StartLiveAuctionResponse.serializer()
    )

    fun end(liveBroadcastId: String, idempotencyKey: String): ApiResult<EndLiveBroadcastResponse> = postCommand(
        path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId/end",
        idempotencyKey = idempotencyKey,
        serializer = EndLiveBroadcastResponse.serializer()
    )

    private fun <T> postCommand(path: String, idempotencyKey: String, serializer: kotlinx.serialization.DeserializationStrategy<T>): ApiResult<T> = configured {
        client.execute(
            client.requestBuilder(path).header("Idempotency-Key", idempotencyKey).post(RequestBody.EMPTY).build(),
            serializer
        )
    }

    fun update(liveBroadcastId: String, title: String, description: String?, scheduledAt: String, streamUrl: String?): ApiResult<UpdateLiveBroadcastResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId"
        val body = UpdateLiveBroadcastRequest(title, description?.takeIf(String::isNotBlank), scheduledAt, streamUrl?.takeIf(String::isNotBlank))
        client.execute(
            client.requestBuilder(path).patch(client.jsonBody(body, UpdateLiveBroadcastRequest.serializer())).build(),
            UpdateLiveBroadcastResponse.serializer()
        )
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
