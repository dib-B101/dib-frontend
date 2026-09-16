package com.ssafy.dib.data.remote.settlement

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SettlementListResponse(
    val items: List<SettlementSummaryDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
)

@Serializable
data class SettlementSummaryDto(
    val settlementId: JsonElement,
    val orderId: JsonElement,
    val sellerId: JsonElement? = null,
    val grossAmount: Long,
    val commisionFee: Long,
    val netAmount: Long,
    val payoutAt: String? = null
)

@Serializable
data class SettlementDetailResponse(
    val settlementId: JsonElement,
    val orderId: JsonElement,
    val sellerId: JsonElement? = null,
    val grossAmount: Long,
    val commisionFee: Long,
    val netAmount: Long,
    val bankName: String? = null,
    val maskedAccountNumber: String? = null,
    val payoutAt: String? = null
)
