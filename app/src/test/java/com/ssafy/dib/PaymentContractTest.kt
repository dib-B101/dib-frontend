package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.payment.PaymentPreparationResponse
import com.ssafy.dib.data.remote.payment.PaymentResponse
import com.ssafy.dib.data.repository.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentContractTest {
    @Test
    fun preparationMapsNestedCheckoutUrl() {
        val response = DibJson.instance.decodeFromString(
            PaymentPreparationResponse.serializer(),
            """{"orderId":7,"amount":58000,"paymentRequest":{"checkoutUrl":"https://pay.example/orders/7"}}"""
        )

        val payment = response.toDomain()

        assertEquals("7", payment.orderId)
        assertEquals(58_000L, payment.amount)
        assertEquals("https://pay.example/orders/7", payment.paymentUrl)
    }

    @Test
    fun confirmedPaymentMapsReceipt() {
        val response = DibJson.instance.decodeFromString(
            PaymentResponse.serializer(),
            """{"paymentId":19,"orderId":7,"buyerId":2,"amount":58000,"type":"CARD","receiptUrl":"https://pay.example/receipt/19","paidAt":"2026-09-11T08:00:00Z"}"""
        )

        val payment = response.toDomain()

        assertEquals("19", payment.paymentId)
        assertEquals("7", payment.orderId)
        assertEquals("CARD", payment.type)
        assertEquals("https://pay.example/receipt/19", payment.receiptUrl)
    }
}
