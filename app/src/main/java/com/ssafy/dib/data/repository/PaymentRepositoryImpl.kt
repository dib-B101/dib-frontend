package com.ssafy.dib.data.repository

import com.ssafy.dib.core.network.ApiResult
import com.ssafy.dib.data.remote.payment.ConfirmPaymentRequest
import com.ssafy.dib.data.remote.payment.PaymentPreparationResponse
import com.ssafy.dib.data.remote.payment.PaymentRemoteDataSource
import com.ssafy.dib.data.remote.payment.PaymentResponse
import com.ssafy.dib.domain.payment.Payment
import com.ssafy.dib.domain.payment.PaymentPreparation
import com.ssafy.dib.domain.payment.PaymentRepository
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class PaymentRepositoryImpl(private val remote: PaymentRemoteDataSource) : PaymentRepository {
    override fun prepare(orderId: String, paymentType: String, idempotencyKey: String): ApiResult<PaymentPreparation> =
        when (val result = remote.prepare(orderId, paymentType, idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun confirm(orderId: String, paymentKey: String, amount: Long, type: String, idempotencyKey: String): ApiResult<Payment> =
        when (val result = remote.confirm(orderId, ConfirmPaymentRequest(paymentKey, amount, type), idempotencyKey)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
            is ApiResult.Failure -> result
        }

    override fun getPayment(paymentId: String): ApiResult<Payment> = when (val result = remote.getPayment(paymentId)) {
        is ApiResult.Success -> ApiResult.Success(result.value.toDomain(), result.status)
        is ApiResult.Failure -> result
    }
}

internal fun PaymentPreparationResponse.toDomain() = PaymentPreparation(orderId.idValue(), amount, paymentRequest.findUrl())
internal fun PaymentResponse.toDomain() = Payment(paymentId.idValue(), orderId.idValue(), amount, type, receiptUrl, paidAt)

private fun JsonElement.idValue() = (this as? JsonPrimitive)?.contentOrNull ?: toString().trim('"')
private fun JsonElement?.findUrl(): String? = when (this) {
    is JsonPrimitive -> contentOrNull?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    is JsonObject -> listOf("paymentUrl", "checkoutUrl", "redirectUrl", "url").firstNotNullOfOrNull { this[it].findUrl() } ?: values.firstNotNullOfOrNull(JsonElement?::findUrl)
    is JsonArray -> firstNotNullOfOrNull(JsonElement?::findUrl)
    else -> null
}
