package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.support.CreateInquiryRequest
import com.ssafy.dib.data.remote.support.InquiryDetailDto
import com.ssafy.dib.data.remote.support.InquiryRemoteDataSource
import com.ssafy.dib.data.remote.support.InquirySummaryDto
import com.ssafy.dib.domain.support.InquiryDetail
import com.ssafy.dib.domain.support.InquiryRepository
import com.ssafy.dib.domain.support.InquirySummary
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class InquiryRepositoryImpl(private val remote: InquiryRemoteDataSource) : InquiryRepository {
    override fun getInquiries(size: Int): ApiResult<List<InquirySummary>> =
        when (val result = remote.getInquiries(size)) {
            is ApiResult.Success -> ApiResult.Success(result.value.items.map(InquirySummaryDto::toDomain), result.status)
            is ApiResult.Failure -> result
        }

    override fun getInquiry(questionId: String): ApiResult<InquiryDetail> =
        when (val result = remote.getInquiry(questionId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun createInquiry(title: String, content: String): ApiResult<String> =
        when (val result = remote.createInquiry(CreateInquiryRequest(title, content))) {
            is ApiResult.Success -> ApiResult.Success(result.value.questionId.idValue(), result.status)
            is ApiResult.Failure -> result
        }
}

internal fun InquirySummaryDto.toDomain() = InquirySummary(questionId.idValue(), title, createdAt, answeredAt)

internal fun InquiryDetailDto.toDomain() = InquiryDetail(
    questionId = questionId.idValue(),
    title = title,
    content = content,
    createdAt = createdAt,
    answer = answer,
    answeredAt = answeredAt
)

private fun kotlinx.serialization.json.JsonElement.idValue(): String =
    (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
