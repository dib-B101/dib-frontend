package com.ssafy.dib.data.remote.auction

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.RequestBody

class AuctionRemoteDataSource(private val client: DibHttpClient) {
    fun getGeneralAuctions(
        size: Int,
        categoryId: String? = null,
        status: String = "ACTIVE",
        minPrice: Long? = null,
        maxPrice: Long? = null,
        sort: String? = null
    ): ApiResult<AuctionListResponse> = configured {
        val urlBuilder = client.urlBuilder(ApiRoutes.AUCTIONS)
            .addQueryParameter("scope", "GENERAL")
            .addQueryParameter("status", status)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        categoryId?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("categoryId", it) }
        minPrice?.let { urlBuilder.addQueryParameter("minPrice", it.coerceAtLeast(0).toString()) }
        maxPrice?.let { urlBuilder.addQueryParameter("maxPrice", it.coerceAtLeast(0).toString()) }
        sort?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("sort", it) }
        val url = urlBuilder.build()
        client.execute(
            client.requestBuilder(ApiRoutes.AUCTIONS).url(url).get().build(),
            AuctionListResponse.serializer()
        )
    }

    fun getAuction(auctionId: String): ApiResult<AuctionDto> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId"
        client.execute(client.requestBuilder(path).get().build(), AuctionDto.serializer())
    }

    fun createAuction(productId: String, startPrice: Long, auctionTime: Long, idempotencyKey: String): ApiResult<AuctionCommandResponse> = configured {
        val id = productId.toLongOrNull()?.let(::JsonPrimitive) ?: JsonPrimitive(productId)
        val body = CreateAuctionRequest(id, startPrice, auctionTime)
        client.execute(client.requestBuilder(ApiRoutes.AUCTIONS).header("Idempotency-Key", idempotencyKey).post(client.jsonBody(body, CreateAuctionRequest.serializer())).build(), AuctionCommandResponse.serializer())
    }

    fun updateAuction(auctionId: String, startPrice: Long, auctionTime: Long): ApiResult<AuctionCommandResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId"
        val body = UpdateAuctionRequest(startPrice, auctionTime)
        client.execute(client.requestBuilder(path).patch(client.jsonBody(body, UpdateAuctionRequest.serializer())).build(), AuctionCommandResponse.serializer())
    }

    fun cancelAuction(auctionId: String, idempotencyKey: String): ApiResult<Unit> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId"
        client.executeUnit(client.requestBuilder(path).header("Idempotency-Key", idempotencyKey).delete().build())
    }

    fun startAuction(auctionId: String, idempotencyKey: String): ApiResult<StartAuctionResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId/start"
        client.execute(client.requestBuilder(path).header("Idempotency-Key", idempotencyKey).patch(RequestBody.EMPTY).build(), StartAuctionResponse.serializer())
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (error: RuntimeException) {
            ApiResult.Failure(
                ApiFailure(
                    status = null,
                    code = ApiErrorCodes.CLIENT_NOT_CONFIGURED,
                    message = error.message ?: "네트워크 주소 설정을 확인해주세요.",
                    cause = error
                )
            )
        }
}
