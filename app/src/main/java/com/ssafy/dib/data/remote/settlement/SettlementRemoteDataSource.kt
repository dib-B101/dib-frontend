package com.ssafy.dib.data.remote.settlement

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class SettlementRemoteDataSource(private val client: DibHttpClient) {
    fun getSettlements(cursor: String?, size: Int): ApiResult<SettlementListResponse> = configured {
        val urlBuilder = client.urlBuilder(ApiRoutes.SETTLEMENTS)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
        cursor?.takeIf(String::isNotBlank)?.let { urlBuilder.addQueryParameter("cursor", it) }
        client.execute(
            client.requestBuilder(ApiRoutes.SETTLEMENTS).url(urlBuilder.build()).get().build(),
            SettlementListResponse.serializer()
        )
    }

    fun getSettlement(settlementId: String): ApiResult<SettlementDetailResponse> = configured {
        val path = "${ApiRoutes.SETTLEMENTS}/$settlementId"
        client.execute(client.requestBuilder(path).get().build(), SettlementDetailResponse.serializer())
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (error: RuntimeException) {
            ApiResult.Failure(
                ApiFailure(
                    status = null,
                    code = ApiErrorCodes.CLIENT_NOT_CONFIGURED,
                    message = error.message ?: "네트워크 주소 설정을 확인해주세요.",
                    cause = error
                )
            )
        }
}
