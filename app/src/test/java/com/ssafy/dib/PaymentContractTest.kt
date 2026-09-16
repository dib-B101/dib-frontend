package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.payment.PaymentMethodResponse
import com.ssafy.dib.data.remote.payment.PaymentResponse
import com.ssafy.dib.data.repository.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentContractTest {
    @Test
    fun paymentMethodMapsMaskedCardDetails() {
        val response = DibJson.instance.decodeFromString(
            PaymentMethodResponse.serializer(),
            """{"paymentMethodId":7,"type":"CARD","cardCompany":"현대","cardNumber":"1234-****-****-5678","createdAt":"2026-09-16T08:00:00Z"}"""
        )

        val method = response.toDomain()

        assertEquals("7", method.paymentMethodId)
        assertEquals("CARD", method.type)
        assertEquals("현대", method.cardCompany)
        assertEquals("1234-****-****-5678", method.cardNumber)
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
