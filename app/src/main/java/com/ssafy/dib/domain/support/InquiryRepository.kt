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

interface InquiryRepository {
    fun getInquiries(size: Int = 30): ApiResult<List<InquirySummary>>
    fun getInquiry(questionId: String): ApiResult<InquiryDetail>
    fun createInquiry(title: String, content: String): ApiResult<String>
}
