package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.settlement.SettlementDetailResponse
import com.ssafy.dib.data.remote.settlement.SettlementListResponse
import com.ssafy.dib.data.repository.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SettlementContractTest {
    @Test
    fun listContractKeepsBackendCommissionSpellingAndCursor() {
        val response = DibJson.instance.decodeFromString(
            SettlementListResponse.serializer(),
            """{"items":[{"settlementId":31,"orderId":"order-8","sellerId":7,"grossAmount":35000,"commisionFee":1750,"netAmount":33250,"payoutAt":null}],"nextCursor":"next-31","hasNext":true}"""
        )

        val settlement = response.items.single().toDomain()
        assertEquals("31", settlement.settlementId)
        assertEquals("order-8", settlement.orderId)
        assertEquals(1_750L, settlement.commissionFee)
        assertNull(settlement.payoutAt)
        assertEquals("next-31", response.nextCursor)
    }

    @Test
    fun detailContractMapsMaskedAccountOnly() {
        val response = DibJson.instance.decodeFromString(
            SettlementDetailResponse.serializer(),
            """{"settlementId":"settlement-1","orderId":20,"sellerId":7,"grossAmount":50000,"commisionFee":2500,"netAmount":47500,"bankName":"우리은행","maskedAccountNumber":"1002-***-123456","payoutAt":"2026-09-13T11:20:00"}"""
        ).toDomain()

        assertEquals("20", response.orderId)
        assertEquals("1002-***-123456", response.maskedAccountNumber)
        assertEquals(47_500L, response.netAmount)
        assertFalse(response.maskedAccountNumber.contains("000000"))
    }
}
