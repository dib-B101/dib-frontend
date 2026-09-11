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

interface ReportRepository {
    fun getMyReports(size: Int = 30): ApiResult<List<ReportSummary>>
    fun reportAuction(auctionId: String, content: String): ApiResult<String>
    fun reportMember(memberId: String, content: String): ApiResult<String>
}
