package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.auction.BidDepositRemoteDataSource
import com.ssafy.dib.data.remote.auction.BidDepositResponse
import com.ssafy.dib.data.remote.auction.ConfirmBidDepositRequest
import com.ssafy.dib.data.remote.auction.PrepareBidDepositRequest
import com.ssafy.dib.domain.auction.BidDeposit
import com.ssafy.dib.domain.auction.BidDepositRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class BidDepositRepositoryImpl(private val remote: BidDepositRemoteDataSource) : BidDepositRepository {
    override fun prepare(auctionId: String, firstBidAmount: Long, paymentMethod: String, idempotencyKey: String) =
        remote.prepare(auctionId, PrepareBidDepositRequest(firstBidAmount, paymentMethod), idempotencyKey)
            .mapDeposit(auctionId)

    override fun confirm(bidDepositId: String, paymentKey: String, amount: Long, idempotencyKey: String) =
        remote.confirm(bidDepositId, ConfirmBidDepositRequest(paymentKey, amount), idempotencyKey)
            .mapDeposit("")

    override fun getMine(auctionId: String) = remote.getMine(auctionId).mapDeposit(auctionId)
}

private fun ApiResult<BidDepositResponse>.mapDeposit(fallbackAuctionId: String): ApiResult<BidDeposit> = when (this) {
    is ApiResult.Success -> ApiResult.Success(value.toDomain(fallbackAuctionId), status)
    is ApiResult.Failure -> this
}

internal fun BidDepositResponse.toDomain(fallbackAuctionId: String = "") = BidDeposit(
    bidDepositId = bidDepositId.idValueOrNull(),
    auctionId = auctionId.idValueOrNull() ?: fallbackAuctionId,
    amount = amount,
    status = status,
    paymentUrl = paymentRequest.findPaymentUrl(),
    releasedAt = releasedAt
)

private fun JsonElement?.idValueOrNull(): String? =
    (this as? JsonPrimitive)?.contentOrNull ?: this?.toString()?.trim('"')

private fun JsonElement?.findPaymentUrl(): String? = when (this) {
    is JsonPrimitive -> contentOrNull?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    is JsonObject -> listOf("paymentUrl", "checkoutUrl", "redirectUrl", "url")
        .firstNotNullOfOrNull { key -> this[key].findPaymentUrl() }
        ?: values.firstNotNullOfOrNull(JsonElement?::findPaymentUrl)
    is JsonArray -> firstNotNullOfOrNull(JsonElement?::findPaymentUrl)
    else -> null
}
