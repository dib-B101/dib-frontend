package com.ssafy.dib.data.remote.payment

import com.ssafy.dib.core.network.ApiErrorCodes
import com.ssafy.dib.core.network.ApiFailure
import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.core.network.DibHttpClient
import com.ssafy.dib.data.remote.ApiRoutes

class PaymentRemoteDataSource(private val client: DibHttpClient) {
    fun prepare(orderId: String, paymentType: String, idempotencyKey: String): ApiResult<PaymentPreparationResponse> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/payments/prepare"
        val request = PreparePaymentRequest(paymentType)
        client.execute(client.requestBuilder(path).header("Idempotency-Key", idempotencyKey).post(client.jsonBody(request, PreparePaymentRequest.serializer())).build(), PaymentPreparationResponse.serializer())
    }

    fun confirm(orderId: String, request: ConfirmPaymentRequest, idempotencyKey: String): ApiResult<PaymentResponse> = configured {
        val path = "${ApiRoutes.ORDERS}/$orderId/payments/confirm"
        client.execute(client.requestBuilder(path).header("Idempotency-Key", idempotencyKey).post(client.jsonBody(request, ConfirmPaymentRequest.serializer())).build(), PaymentResponse.serializer())
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
