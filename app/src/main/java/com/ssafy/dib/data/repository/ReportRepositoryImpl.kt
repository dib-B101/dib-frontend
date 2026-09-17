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
    targetLabel = targetLabel(),
    createdAt = createdAt,
    processedAt = processedAt
)

// 대상 표기는 서버가 내려준 type을 기준으로 고른다. id 필드가 채워진 순서로 고르면 회원 신고가 "주문 N"으로 표시된다.
private fun ReportDto.targetLabel(): String {
    val targetId = targetMemberId ?: reportTargetId
    val targetName = reportTargetNickname?.takeIf { it.isNotBlank() } ?: targetId?.idValue()
    return when (type.uppercase()) {
        "MEMBER" -> "회원 " + (targetName ?: "대상")
        "AUCTION" -> auctionId?.let { "경매 ${it.idValue()}" } ?: "경매 신고"
        "ORDER" -> orderId?.let { "주문 ${it.idValue()}" } ?: "주문 신고"
        "CHATTING" -> chattingId?.let { "채팅 ${it.idValue()}" }
            ?: orderId?.let { "주문 ${it.idValue()}" }
            ?: targetName?.let { "채팅 $it" }
            ?: "채팅 신고"
        else -> targetName?.let { "신고 대상 $it" } ?: "신고 대상"
    }
}

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
