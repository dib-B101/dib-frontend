package com.ssafy.dib.data.remote.auction

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class AuctionRemoteDataSource(private val client: DibHttpClient) {
    fun getActiveGeneralAuctions(size: Int): ApiResult<AuctionListResponse> = configured {
        val url = client.urlBuilder(ApiRoutes.AUCTIONS)
            .addQueryParameter("scope", "GENERAL")
            .addQueryParameter("status", "ACTIVE")
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
            .build()
        client.execute(
            client.requestBuilder(ApiRoutes.AUCTIONS).url(url).get().build(),
            AuctionListResponse.serializer()
        )
    }

    fun getAuction(auctionId: String): ApiResult<AuctionDto> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId"
        client.execute(client.requestBuilder(path).get().build(), AuctionDto.serializer())
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
