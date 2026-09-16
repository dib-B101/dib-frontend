package com.ssafy.dib.domain.payment

import com.ssafy.dib.core.network.ApiResult

data class PaymentMethod(
    val paymentMethodId: String,
    val type: String,
    val cardCompany: String?,
    val cardNumber: String?,
    val createdAt: String?
)

data class Payment(
    val paymentId: String,
    val orderId: String,
    val amount: Long,
    val type: String,
    val receiptUrl: String?,
    val paidAt: String?
)

interface PaymentRepository {
    fun getPaymentMethod(): ApiResult<PaymentMethod>
    fun registerPaymentMethod(authKey: String, customerKey: String): ApiResult<PaymentMethod>
    fun deletePaymentMethod(): ApiResult<Unit>
    fun retryPayment(orderId: String): ApiResult<Payment>
    fun getPayment(paymentId: String): ApiResult<Payment>
}
