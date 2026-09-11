package com.ssafy.dib.data.remote.report

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ReportListResponse(
    val items: List<ReportDto> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class ReportDto(
    val reportId: JsonElement,
    val content: String = "",
    val type: String = "",
    val status: String = "PENDING",
    val auctionId: JsonElement? = null,
    val orderId: JsonElement? = null,
    val chattingId: JsonElement? = null,
    val targetMemberId: JsonElement? = null,
    val createdAt: String? = null
)

@Serializable
data class CreateReportRequest(val content: String, val type: String)

@Serializable
data class CreateReportResponse(val reportId: JsonElement, val status: String)
