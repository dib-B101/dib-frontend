package com.ssafy.dib.data.remote.auction

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes
import com.ssafy.dib.core.network.DibJson
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.RequestBody

class AuctionRemoteDataSource(private val client: DibHttpClient) {
    fun getAuctions(scope: String, status: String, cursor: String?, size: Int): ApiResult<AuctionListResponse> = configured {
        val urlBuilder = client.urlBuilder(ApiRoutes.AUCTIONS)
            .addQueryParameter("scope", scope)
            .addQueryParameter("status", status)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        val url = urlBuilder.build()
        client.execute(client.requestBuilder(ApiRoutes.AUCTIONS).url(url).get().build(), JsonElement.serializer())
            .decodeAuctionPayload(::decodeAuctionList)
    }

    fun getMySales(auctionStatus: String?, cursor: String?, size: Int): ApiResult<SaleHistoryResponse> = configured {
        val urlBuilder = client.urlBuilder(ApiRoutes.MEMBER_SALES)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        auctionStatus?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("auctionStatus", it) }
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        client.execute(
            client.requestBuilder(ApiRoutes.MEMBER_SALES).url(urlBuilder.build()).get().build(),
            SaleHistoryResponse.serializer()
        )
    }

    fun getGeneralAuctions(
        size: Int,
        categoryId: String? = null,
        status: String = "ACTIVE",
        minPrice: Long? = null,
        maxPrice: Long? = null,
        sort: String? = null,
        cursor: String? = null
    ): ApiResult<AuctionListResponse> = configured {
        val urlBuilder = client.urlBuilder(ApiRoutes.AUCTIONS)
            .addQueryParameter("scope", "GENERAL")
            .addQueryParameter("status", status)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        categoryId?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("categoryId", it) }
        minPrice?.let { urlBuilder.addQueryParameter("minPrice", it.coerceAtLeast(0).toString()) }
        maxPrice?.let { urlBuilder.addQueryParameter("maxPrice", it.coerceAtLeast(0).toString()) }
        sort?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("sort", it) }
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        val url = urlBuilder.build()
        client.execute(client.requestBuilder(ApiRoutes.AUCTIONS).url(url).get().build(), JsonElement.serializer())
            .decodeAuctionPayload(::decodeAuctionList)
    }

    fun getSellerAuctions(sellerId: String): ApiResult<List<SellerAuctionDto>> = configured {
        val path = "${ApiRoutes.AUCTIONS}/sellers/$sellerId"
        client.execute(client.requestBuilder(path).get().build(), ListSerializer(SellerAuctionDto.serializer()))
    }

    fun getAuction(auctionId: String): ApiResult<AuctionDto> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId"
        client.execute(client.requestBuilder(path).get().build(), AuctionDto.serializer())
    }

    fun getRecommendations(size: Int): ApiResult<AuctionRecommendationResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/recommendation"
        val url = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 100).toString()).build()
        client.execute(client.requestBuilder(path).url(url).get().build(), JsonElement.serializer())
            .decodeAuctionPayload(::decodeAuctionRecommendations)
    }

    // 서버가 찜 상품의 경매 카드를 페이지로 내려주므로 최근 경매 목록과 다시 교집합하지 않는다.
    fun getBookmarks(cursor: String?, size: Int): ApiResult<AuctionListResponse> = configured {
        val path = "${ApiRoutes.BOOKMARKS}/me"
        val urlBuilder = client.urlBuilder(path)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        client.execute(client.requestBuilder(path).url(urlBuilder.build()).get().build(), JsonElement.serializer())
            .decodeAuctionPayload(::decodeAuctionList)
    }

    fun getMyBids(cursor: String?, size: Int): ApiResult<BidHistoryListResponse> = configured {
        val path = "${ApiRoutes.MEMBERS_ME}/bids"
        val urlBuilder = client.urlBuilder(path)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        val url = urlBuilder.build()
        client.execute(client.requestBuilder(path).url(url).get().build(), BidHistoryListResponse.serializer())
    }

    fun getBidHistory(auctionId: String, cursor: String?, size: Int): ApiResult<AuctionBidHistoryListResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId/bids"
        val urlBuilder = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        client.execute(
            client.requestBuilder(path).url(urlBuilder.build()).get().build(),
            AuctionBidHistoryListResponse.serializer()
        )
    }

    fun getBidSnapshot(auctionId: String): ApiResult<AuctionBidSnapshotResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId/bid-snapshot"
        client.execute(client.requestBuilder(path).get().build(), AuctionBidSnapshotResponse.serializer())
    }

    fun setBookmark(productId: String, bookmarked: Boolean, idempotencyKey: String): ApiResult<Unit> = configured {
        val path = "${ApiRoutes.PRODUCTS}/$productId/bookmark"
        val builder = client.requestBuilder(path).header("Idempotency-Key", idempotencyKey)
        val request = if (bookmarked) builder.post(RequestBody.EMPTY).build() else builder.delete().build()
        client.executeUnit(request)
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

    fun startAuction(auctionId: String, idempotencyKey: String, startPrice: Long? = null, auctionTime: Long? = null): ApiResult<StartAuctionResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId/start"
        // 본문을 생략하면 서버가 DB 에 저장된 시작가/경매 시간을 쓴다
        val body = if (startPrice == null && auctionTime == null) RequestBody.EMPTY
        else client.jsonBody(StartAuctionRequest(startPrice, auctionTime), StartAuctionRequest.serializer())
        client.execute(client.requestBuilder(path).header("Idempotency-Key", idempotencyKey).patch(body).build(), StartAuctionResponse.serializer())
    }

    fun relistAuction(auctionId: String, idempotencyKey: String): ApiResult<AuctionCommandResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId/relist"
        client.execute(
            client.requestBuilder(path).header("Idempotency-Key", idempotencyKey).patch(RequestBody.EMPTY).build(),
            AuctionCommandResponse.serializer()
        )
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

internal fun decodeAuctionList(payload: JsonElement): AuctionListResponse = when (payload) {
    is JsonArray -> AuctionListResponse(DibJson.instance.decodeFromJsonElement(ListSerializer(AuctionDto.serializer()), payload))
    else -> DibJson.instance.decodeFromJsonElement(AuctionListResponse.serializer(), payload)
}

internal fun decodeAuctionRecommendations(payload: JsonElement): AuctionRecommendationResponse = when (payload) {
    is JsonArray -> AuctionRecommendationResponse(generalItems = DibJson.instance.decodeFromJsonElement(ListSerializer(AuctionDto.serializer()), payload))
    else -> DibJson.instance.decodeFromJsonElement(AuctionRecommendationResponse.serializer(), payload)
}

private inline fun <T, R> ApiResult<T>.decodeAuctionPayload(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value), status)
    is ApiResult.Failure -> this
}
