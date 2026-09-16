package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.payment.PaymentMethodResponse
import com.ssafy.dib.data.remote.payment.PaymentRemoteDataSource
import com.ssafy.dib.data.remote.payment.PaymentResponse
import com.ssafy.dib.domain.payment.Payment
import com.ssafy.dib.domain.payment.PaymentMethod
import com.ssafy.dib.domain.payment.PaymentRepository
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class PaymentRepositoryImpl(private val remote: PaymentRemoteDataSource) : PaymentRepository {
    override fun getPaymentMethod(): ApiResult<PaymentMethod> =
        when (val result = remote.getPaymentMethod()) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun registerPaymentMethod(authKey: String, customerKey: String): ApiResult<PaymentMethod> =
        when (val result = remote.registerPaymentMethod(authKey, customerKey)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun deletePaymentMethod(): ApiResult<Unit> = remote.deletePaymentMethod()

    override fun retryPayment(orderId: String): ApiResult<Payment> =
        when (val result = remote.retryPayment(orderId)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun getPayment(paymentId: String): ApiResult<Payment> = when (val result = remote.getPayment(paymentId)) {
        is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
        is ApiResult.Failure -> result
    }
}

internal fun PaymentMethodResponse.toDomain() = PaymentMethod(paymentMethodId.idValue(), type, cardCompany, cardNumber, createdAt)
internal fun PaymentResponse.toDomain() = Payment(paymentId.idValue(), orderId.idValue(), amount, type, receiptUrl, paidAt)

private fun JsonElement.idValue() = (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
