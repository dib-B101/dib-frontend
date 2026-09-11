package com.ssafy.dib.domain.auction

import com.ssafy.dib.core.network.ApiResult

data class BidDeposit(
    val bidDepositId: String?,
    val auctionId: String,
    val amount: Long?,
    val status: String,
    val paymentUrl: String? = null,
    val releasedAt: String? = null
)

interface BidDepositRepository {
    fun prepare(
        auctionId: String,
        firstBidAmount: Long,
        paymentMethod: String,
        idempotencyKey: String
    ): ApiResult<BidDeposit>

    fun confirm(
        bidDepositId: String,
        paymentKey: String,
        amount: Long,
        idempotencyKey: String
    ): ApiResult<BidDeposit>

    fun getMine(auctionId: String): ApiResult<BidDeposit>
}
