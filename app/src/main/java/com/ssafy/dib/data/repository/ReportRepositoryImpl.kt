package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.report.ReportDto
import com.ssafy.dib.data.remote.report.ReportRemoteDataSource
import com.ssafy.dib.domain.report.ReportRepository
import com.ssafy.dib.domain.report.ReportSummary
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class ReportRepositoryImpl(private val remote: ReportRemoteDataSource) : ReportRepository {
    override fun getMyReports(size: Int): ApiResult<List<ReportSummary>> =
        when (val result = remote.getMyReports(size)) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map(ReportDto::toDomain), result.status)
            is ApiResult.Failure -> result
        }

    override fun reportAuction(auctionId: String, content: String): ApiResult<String> =
        when (val result = remote.reportAuction(auctionId, content)) {
            is ApiResult.Success -> ApiResult.Success(result.value.reportId.idValue(), result.status)
            is ApiResult.Failure -> result
        }

    override fun reportMember(memberId: String, content: String): ApiResult<String> =
        when (val result = remote.reportMember(memberId, content)) {
            is ApiResult.Success -> ApiResult.Success(result.value.reportId.idValue(), result.status)
            is ApiResult.Failure -> result
        }
}

internal fun ReportDto.toDomain() = ReportSummary(
    reportId = reportId.idValue(),
    type = type,
    content = content,
    status = status,
    targetLabel = when {
        auctionId != null -> "경매 ${auctionId.idValue()}"
        targetMemberId != null -> "회원 ${targetMemberId.idValue()}"
        orderId != null -> "주문 ${orderId.idValue()}"
        chattingId != null -> "채팅 ${chattingId.idValue()}"
        else -> "신고 대상"
    },
    createdAt = createdAt
)

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
