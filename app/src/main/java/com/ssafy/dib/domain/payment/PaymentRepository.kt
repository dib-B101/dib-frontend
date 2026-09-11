package com.ssafy.dib.domain.payment

import com.ssafy.dib.core.network.ApiResult

data class PaymentPreparation(val orderId: String, val amount: Long, val paymentUrl: String?)

data class Payment(
    val paymentId: String,
    val orderId: String,
    val amount: Long,
    val type: String,
    val receiptUrl: String?,
    val paidAt: String?
)

interface PaymentRepository {
    fun prepare(orderId: String, paymentType: String, idempotencyKey: String): ApiResult<PaymentPreparation>
    fun confirm(orderId: String, paymentKey: String, amount: Long, type: String, idempotencyKey: String): ApiResult<Payment>
    fun getPayment(paymentId: String): ApiResult<Payment>
}
