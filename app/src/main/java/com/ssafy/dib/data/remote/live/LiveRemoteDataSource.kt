package com.ssafy.dib.data.remote.live

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.builtins.ListSerializer
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

    fun getMine(memberId: String, status: String?, cursor: String?, size: Int): ApiResult<LiveBroadcastListResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/members/$memberId"
        when (val result = client.execute(client.requestBuilder(path).get().build(), ListSerializer(LiveBroadcastDto.serializer()))) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(
                LiveBroadcastListResponse(
                    items = result.value
                        .filter { status.isNullOrBlank() || it.status == status }
                        .take(size.coerceIn(1, 100))
                ),
                result.status
            )
        }
    }

    fun create(title: String, description: String?, scheduledAt: String, streamUrl: String?, idempotencyKey: String): ApiResult<CreateLiveBroadcastResponse> = configured {
        val body = CreateLiveBroadcastRequest(title, description?.takeIf(String::isNotBlank), scheduledAt.takeIf(String::isNotBlank))
        client.execute(
            client.requestBuilder(ApiRoutes.LIVE_BROADCASTS)
                .header("Idempotency-Key", idempotencyKey)
                .post(client.jsonBody(body, CreateLiveBroadcastRequest.serializer()))
                .build(),
            CreateLiveBroadcastResponse.serializer()
        )
    }

    fun setItems(liveBroadcastId: String, auctionIds: List<String>): ApiResult<SetLiveItemsResponse> = configured {
        unsupported("라이브 상품 편성 API가 백엔드에 아직 없습니다.")
    }

    fun prepareStream(liveBroadcastId: String, idempotencyKey: String): ApiResult<LiveStreamSessionResponse> = postCommand(
        path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId/token",
        idempotencyKey = idempotencyKey,
        serializer = LiveStreamSessionResponse.serializer()
    )

    fun start(liveBroadcastId: String, idempotencyKey: String): ApiResult<StartLiveBroadcastResponse> = postCommand(
        path = "",
        idempotencyKey = idempotencyKey,
        serializer = StartLiveBroadcastResponse.serializer(),
        unsupportedMessage = "라이브 시작 API가 백엔드에 아직 없습니다."
    )

    fun startAuction(liveBroadcastId: String, auctionId: String, idempotencyKey: String): ApiResult<StartLiveAuctionResponse> = postCommand(
        path = "",
        idempotencyKey = idempotencyKey,
        serializer = StartLiveAuctionResponse.serializer(),
        unsupportedMessage = "라이브 경매 시작 API가 백엔드에 아직 없습니다."
    )

    fun end(liveBroadcastId: String, idempotencyKey: String): ApiResult<EndLiveBroadcastResponse> = postCommand(
        path = "",
        idempotencyKey = idempotencyKey,
        serializer = EndLiveBroadcastResponse.serializer(),
        unsupportedMessage = "라이브 종료 API가 백엔드에 아직 없습니다."
    )

    private fun <T> postCommand(
        path: String,
        idempotencyKey: String,
        serializer: kotlinx.serialization.DeserializationStrategy<T>,
        unsupportedMessage: String? = null
    ): ApiResult<T> = configured {
        if (unsupportedMessage != null) return@configured unsupported(unsupportedMessage)
        client.execute(
            client.requestBuilder(path).header("Idempotency-Key", idempotencyKey).post(RequestBody.EMPTY).build(),
            serializer
        )
    }

    fun update(liveBroadcastId: String, title: String, description: String?, scheduledAt: String, streamUrl: String?): ApiResult<UpdateLiveBroadcastResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId"
        val body = UpdateLiveBroadcastRequest(title, description?.takeIf(String::isNotBlank), scheduledAt.takeIf(String::isNotBlank))
        client.execute(
            client.requestBuilder(path).patch(client.jsonBody(body, UpdateLiveBroadcastRequest.serializer())).build(),
            UpdateLiveBroadcastResponse.serializer()
        )
    }

    fun getMessages(liveBroadcastId: String, beforeLiveChattingId: String?, size: Int): ApiResult<LiveChatMessageListResponse> = configured {
        val path = "${ApiRoutes.LIVE_BROADCASTS}/$liveBroadcastId/chats"
        when (val result = client.execute(client.requestBuilder(path).get().build(), ListSerializer(LiveChatMessageDto.serializer()))) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> ApiResult.Success(
                LiveChatMessageListResponse(
                    items = result.value
                        .filter { message -> beforeLiveChattingId?.toLongOrNull()?.let { before -> message.liveChattingId.toString().trim('"').toLongOrNull()?.let { it < before } } ?: true }
                        .takeLast(size.coerceIn(1, 100)),
                    hasMore = false
                ),
                result.status
            )
        }
    }

    private fun <T> unsupported(message: String): ApiResult<T> =
        ApiResult.Failure(ApiFailure(501, ApiErrorCodes.BACKEND_NOT_IMPLEMENTED, message))

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (error: RuntimeException) {
        ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
    }
}
