package com.ssafy.dib.domain.report

import com.ssafy.dib.core.network.ApiResult

data class ReportSummary(
    val reportId: String,
    val type: String,
    val content: String,
    val status: String,
    val targetLabel: String,
    val createdAt: String?
)

data class ReportPage(val items: List<ReportSummary>, val nextCursor: String?)

interface ReportRepository {
    fun getMyReports(cursor: String? = null, size: Int = 30): ApiResult<ReportPage>
    fun reportAuction(auctionId: String, content: String): ApiResult<String>
    fun reportMember(memberId: String, content: String): ApiResult<String>
    fun reportLiveParticipant(liveBroadcastId: String, memberId: String, content: String): ApiResult<String>
}
