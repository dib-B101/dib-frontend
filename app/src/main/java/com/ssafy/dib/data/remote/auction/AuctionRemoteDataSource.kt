package com.ssafy.dib.data.remote.auction

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes
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
        client.execute(client.requestBuilder(ApiRoutes.AUCTIONS).url(url).get().build(), AuctionListResponse.serializer())
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
        client.execute(
            client.requestBuilder(ApiRoutes.AUCTIONS).url(url).get().build(),
            AuctionListResponse.serializer()
        )
    }

    fun getAuction(auctionId: String): ApiResult<AuctionDto> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId"
        client.execute(client.requestBuilder(path).get().build(), AuctionDto.serializer())
    }

    fun getRecommendations(size: Int): ApiResult<AuctionRecommendationResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/recommendation"
        val url = client.urlBuilder(path).addQueryParameter("size", size.coerceIn(1, 100).toString()).build()
        client.execute(client.requestBuilder(path).url(url).get().build(), AuctionRecommendationResponse.serializer())
    }

    fun getBookmarks(cursor: String?, size: Int): ApiResult<AuctionListResponse> = configured {
        val path = "${ApiRoutes.MEMBERS_ME}/bookmarks"
        val urlBuilder = client.urlBuilder(path)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        val url = urlBuilder.build()
        client.execute(client.requestBuilder(path).url(url).get().build(), AuctionListResponse.serializer())
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

    fun setBookmark(auctionId: String, bookmarked: Boolean, idempotencyKey: String): ApiResult<BookmarkResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId/bookmark"
        val builder = client.requestBuilder(path).header("Idempotency-Key", idempotencyKey)
        val request = if (bookmarked) builder.put(RequestBody.EMPTY).build() else builder.delete().build()
        client.execute(request, BookmarkResponse.serializer())
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
