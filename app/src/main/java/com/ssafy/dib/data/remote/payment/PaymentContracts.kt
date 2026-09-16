package com.ssafy.dib.data.remote.payment

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable data class RegisterPaymentMethodRequest(val authKey: String, val customerKey: String)

@Serializable
data class PaymentMethodResponse(
    val paymentMethodId: JsonElement,
    val type: String,
    val cardCompany: String? = null,
    val cardNumber: String? = null,
    val createdAt: String? = null
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
