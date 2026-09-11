package com.ssafy.dib.data.remote.support

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class InquiryRemoteDataSource(private val client: DibHttpClient) {
    fun getInquiries(size: Int): ApiResult<InquiryListResponse> = configured {
        val url = client.urlBuilder(ApiRoutes.QUESTIONS)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
            .build()
        client.execute(
            client.requestBuilder(ApiRoutes.QUESTIONS).url(url).get().build(),
            InquiryListResponse.serializer()
        )
    }

    fun getInquiry(questionId: String): ApiResult<InquiryDetailDto> = configured {
        val path = "${ApiRoutes.QUESTIONS}/$questionId"
        client.execute(client.requestBuilder(path).get().build(), InquiryDetailDto.serializer())
    }

    fun createInquiry(request: CreateInquiryRequest): ApiResult<CreateInquiryResponse> = configured {
        client.execute(
            client.requestBuilder(ApiRoutes.QUESTIONS)
                .post(client.jsonBody(request, CreateInquiryRequest.serializer()))
                .build(),
            CreateInquiryResponse.serializer()
        )
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (error: RuntimeException) {
            ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message.orEmpty(), cause = error))
        }
}
