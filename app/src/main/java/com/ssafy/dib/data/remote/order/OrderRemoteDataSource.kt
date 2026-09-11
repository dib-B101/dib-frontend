package com.ssafy.dib.data.remote.order

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes
import com.ssafy.dib.domain.order.OrderRole

class OrderRemoteDataSource(private val client: DibHttpClient) {
    fun getOrders(role: OrderRole, size: Int): ApiResult<OrderListResponse> = configured {
        val url = client.urlBuilder(ApiRoutes.ORDERS)
            .addQueryParameter("role", role.name)
            .addQueryParameter("size", size.coerceIn(1, 100).toString())
            .build()
        client.execute(
            client.requestBuilder(ApiRoutes.ORDERS).url(url).get().build(),
            OrderListResponse.serializer()
        )
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
