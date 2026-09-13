package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.settlement.SettlementDetailResponse
import com.ssafy.dib.data.remote.settlement.SettlementRemoteDataSource
import com.ssafy.dib.data.remote.settlement.SettlementSummaryDto
import com.ssafy.dib.domain.settlement.SettlementDetail
import com.ssafy.dib.domain.settlement.SettlementRepository
import com.ssafy.dib.domain.settlement.SettlementPage
import com.ssafy.dib.domain.settlement.SettlementSummary
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class SettlementRepositoryImpl(private val remote: SettlementRemoteDataSource) : SettlementRepository {
    override fun getSettlements(cursor: String?, size: Int): ApiResult<SettlementPage> =
        when (val result = remote.getSettlements(cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                SettlementPage(
                    items = result.value.items.map(SettlementSummaryDto::toDomain),
                    nextCursor = result.value.nextCursor,
                    hasNext = result.value.hasNext
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun getSettlement(settlementId: String): ApiResult<SettlementDetail> =
        when (val result = remote.getSettlement(settlementId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }
}

internal fun SettlementSummaryDto.toDomain() = SettlementSummary(
    settlementId = settlementId.idValue(),
    orderId = orderId.idValue(),
    grossAmount = grossAmount,
    commissionFee = commisionFee,
    netAmount = netAmount,
    payoutAt = payoutAt
)

internal fun SettlementDetailResponse.toDomain() = SettlementDetail(
    settlementId = settlementId.idValue(),
    orderId = orderId.idValue(),
    grossAmount = grossAmount,
    commissionFee = commisionFee,
    netAmount = netAmount,
    bankName = bankName,
    maskedAccountNumber = maskedAccountNumber,
    payoutAt = payoutAt
)

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
