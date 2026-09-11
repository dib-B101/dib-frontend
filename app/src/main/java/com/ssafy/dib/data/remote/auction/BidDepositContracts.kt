package com.ssafy.dib.data.remote.auction

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PrepareBidDepositRequest(val firstBidAmount: Long, val paymentMethod: String)

@Serializable
data class ConfirmBidDepositRequest(val paymentKey: String, val amount: Long)

@Serializable
data class BidDepositResponse(
    val bidDepositId: JsonElement? = null,
    val auctionId: JsonElement? = null,
    val amount: Long? = null,
    val status: String = "",
    val paymentRequest: JsonElement? = null,
    val releasedAt: String? = null
)
