package com.ssafy.dib.data.remote.support

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class InquiryListResponse(
    val items: List<InquirySummaryDto> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class InquirySummaryDto(
    val questionId: JsonElement,
    val title: String,
    val createdAt: String,
    val answeredAt: String? = null
)

@Serializable
data class InquiryDetailDto(
    val questionId: JsonElement,
    val memberId: JsonElement? = null,
    val title: String,
    val content: String,
    val createdAt: String,
    val answer: String? = null,
    val answeredAt: String? = null
)

@Serializable
data class CreateInquiryRequest(val title: String, val content: String)

@Serializable
data class CreateInquiryResponse(val questionId: JsonElement, val createdAt: String)
