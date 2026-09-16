package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.report.ReportDto
import com.ssafy.dib.data.remote.report.ReportRemoteDataSource
import com.ssafy.dib.domain.report.ReportRepository
import com.ssafy.dib.domain.report.ReportSummary
import com.ssafy.dib.domain.report.ReportPage
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class ReportRepositoryImpl(private val remote: ReportRemoteDataSource) : ReportRepository {
    override fun getMyReports(cursor: String?, size: Int): ApiResult<ReportPage> =
        when (val result = remote.getMyReports(cursor, size)) {
            is ApiResult.Success -> ApiResult.Success(
                ReportPage(
                    result.value.items.map(ReportDto::toDomain),
                    result.value.nextCursor,
                    result.value.hasNext || !result.value.nextCursor.isNullOrBlank()
                ),
                result.status
            )
            is ApiResult.Failure -> result
        }

    override fun reportAuction(auctionId: String, content: String, idempotencyKey: String): ApiResult<String> =
        when (val result = remote.reportAuction(auctionId, content, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(result.value.reportId.idValue(), result.status)
            is ApiResult.Failure -> result
        }

    override fun reportMember(memberId: String, content: String, idempotencyKey: String): ApiResult<String> =
        when (val result = remote.reportMember(memberId, content, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(result.value.reportId.idValue(), result.status)
            is ApiResult.Failure -> result
        }

    override fun reportLiveParticipant(liveBroadcastId: String, memberId: String, content: String, idempotencyKey: String): ApiResult<String> =
        when (val result = remote.reportLiveParticipant(liveBroadcastId, memberId, content, idempotencyKey)) {
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
        reportTargetId != null -> when (type.uppercase()) {
            "MEMBER" -> "회원 ${reportTargetId.idValue()}"
            "AUCTION" -> "경매 ${reportTargetId.idValue()}"
            "ORDER", "CHATTING" -> "주문 ${reportTargetId.idValue()}"
            else -> "신고 대상 ${reportTargetId.idValue()}"
        }
        else -> "신고 대상"
    },
    createdAt = createdAt
)

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
