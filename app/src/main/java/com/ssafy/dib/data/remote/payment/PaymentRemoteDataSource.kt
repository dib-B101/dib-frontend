package com.ssafy.dib.data.remote.payment

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes
import okhttp3.RequestBody.Companion.toRequestBody

class PaymentRemoteDataSource(private val client: DibHttpClient) {
    fun getPaymentMethod(): ApiResult<PaymentMethodResponse> = configured {
        client.execute(client.requestBuilder(ApiRoutes.PAYMENT_METHODS).get().build(), PaymentMethodResponse.serializer())
    }

    fun registerPaymentMethod(authKey: String, customerKey: String): ApiResult<PaymentMethodResponse> = configured {
        val request = RegisterPaymentMethodRequest(authKey, customerKey)
        client.execute(
            client.requestBuilder(ApiRoutes.PAYMENT_METHODS)
                .post(client.jsonBody(request, RegisterPaymentMethodRequest.serializer()))
                .build(),
            PaymentMethodResponse.serializer()
        )
    }

    fun deletePaymentMethod(): ApiResult<Unit> = configured {
        client.executeUnit(client.requestBuilder(ApiRoutes.PAYMENT_METHODS).delete().build())
    }

    fun retryPayment(orderId: String): ApiResult<PaymentResponse> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/payments/retry"
        client.execute(client.requestBuilder(path).post("".toRequestBody(null)).build(), PaymentResponse.serializer())
    }

    fun getPayment(paymentId: String): ApiResult<PaymentResponse> = configured {
        val path = "${ApiRoutes.PAYMENTS}/$paymentId"
        client.execute(client.requestBuilder(path).get().build(), PaymentResponse.serializer())
    }

    private inline fun <T> configured(block: () -> ApiResult<T>): ApiResult<T> = try {
        block()
    } catch (error: RuntimeException) {
        ApiResult.Failure(ApiFailure(null, ApiErrorCodes.CLIENT_NOT_CONFIGURED, error.message ?: "네트워크 주소 설정을 확인해주세요.", cause = error))
    }
}
