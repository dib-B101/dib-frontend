package com.ssafy.dib.data.remote.auction

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class BidDepositRemoteDataSource(private val client: DibHttpClient) {
    fun prepare(
        auctionId: String,
        request: PrepareBidDepositRequest,
        idempotencyKey: String
    ): ApiResult<BidDepositResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId/deposits/prepare"
        client.execute(
            client.requestBuilder(path)
                .header("Idempotency-Key", idempotencyKey)
                .post(client.jsonBody(request, PrepareBidDepositRequest.serializer()))
                .build(),
            BidDepositResponse.serializer()
        )
    }

    fun confirm(
        bidDepositId: String,
        request: ConfirmBidDepositRequest,
        idempotencyKey: String
    ): ApiResult<BidDepositResponse> = configured {
        val path = "${ApiRoutes.BID_DEPOSITS}/$bidDepositId/confirm"
        client.execute(
            client.requestBuilder(path)
                .header("Idempotency-Key", idempotencyKey)
                .post(client.jsonBody(request, ConfirmBidDepositRequest.serializer()))
                .build(),
            BidDepositResponse.serializer()
        )
    }

    fun getMine(auctionId: String): ApiResult<BidDepositResponse> = configured {
        val path = "${ApiRoutes.AUCTIONS}/$auctionId/deposits/me"
        client.execute(client.requestBuilder(path).get().build(), BidDepositResponse.serializer())
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
