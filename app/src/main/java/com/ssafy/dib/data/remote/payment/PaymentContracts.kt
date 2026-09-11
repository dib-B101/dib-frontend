package com.ssafy.dib.data.remote.payment

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable data class PreparePaymentRequest(val paymentType: String)
@Serializable data class ConfirmPaymentRequest(val paymentKey: String, val amount: Long, val type: String)

@Serializable
data class PaymentPreparationResponse(
    val orderId: JsonElement,
    val amount: Long,
    val paymentRequest: JsonElement? = null
)

@Serializable
data class PaymentResponse(
    val paymentId: JsonElement,
    val orderId: JsonElement,
    val buyerId: JsonElement? = null,
    val amount: Long,
    val type: String,
    val refundKey: String? = null,
    val receiptUrl: String? = null,
    val paidAt: String? = null
)
