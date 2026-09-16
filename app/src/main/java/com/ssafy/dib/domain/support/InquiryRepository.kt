package com.ssafy.dib.domain.support

import com.ssafy.dib.core.network.ApiResult

data class InquirySummary(
    val questionId: String,
    val title: String,
    val createdAt: String,
    val answeredAt: String?
)

data class InquiryDetail(
    val questionId: String,
    val title: String,
    val content: String,
    val createdAt: String,
    val answer: String?,
    val answeredAt: String?
)

data class InquiryPage(val items: List<InquirySummary>, val nextCursor: String?, val hasNext: Boolean)

interface InquiryRepository {
    fun getInquiries(cursor: String? = null, size: Int = 30): ApiResult<InquiryPage>
    fun getInquiry(questionId: String): ApiResult<InquiryDetail>
    fun createInquiry(title: String, content: String): ApiResult<String>
}
