package com.ssafy.dib.domain.settlement

import com.ssafy.dib.core.network.ApiResult

data class SettlementSummary(
    val settlementId: String,
    val orderId: String,
    val grossAmount: Long,
    val commissionFee: Long,
    val netAmount: Long,
    val payoutAt: String?
)

data class SettlementDetail(
    val settlementId: String,
    val orderId: String,
    val grossAmount: Long,
    val commissionFee: Long,
    val netAmount: Long,
    val bankName: String?,
    val maskedAccountNumber: String?,
    val payoutAt: String?
)

data class SettlementPage(
    val items: List<SettlementSummary>,
    val nextCursor: String?,
    val hasNext: Boolean
)

interface SettlementRepository {
    fun getSettlements(cursor: String? = null, size: Int = 30): ApiResult<SettlementPage>
    fun getSettlement(settlementId: String): ApiResult<SettlementDetail>
}
