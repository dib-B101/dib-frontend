package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.order.OrderListResponse
import com.ssafy.dib.data.repository.toDomain
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderContractTest {
    @Test
    fun mapsFlatOrderSummary() {
        val response = DibJson.instance.decodeFromString(
            OrderListResponse.serializer(),
            """{"items":[{"orderId":12,"auctionId":"auction-3","productId":7,"finalPrice":58000,"status":"SHIPPED","product":{"title":"필름 카메라"}}]}"""
        )

        val order = response.items.single().toDomain()

        assertEquals("12", order.orderId)
        assertEquals("auction-3", order.auctionId)
        assertEquals("7", order.productId)
        assertEquals("필름 카메라", order.title)
        assertEquals(58_000, order.finalPrice)
        assertEquals("SHIPPED", order.status)
    }

    @Test
    fun mapsNestedOrderSummaryReturnedByJoinedQuery() {
        val response = DibJson.instance.decodeFromString(
            OrderListResponse.serializer(),
            """{"items":[{"order":{"orderId":"order-1","auctionId":3,"finalPrice":35000,"status":"PAID","createdAt":"2026-09-11T00:00:00Z"},"auction":{"productId":9},"product":{"name":"빈티지 스니커즈"}}]}"""
        )

        val order = response.items.single().toDomain()

        assertEquals("order-1", order.orderId)
        assertEquals("3", order.auctionId)
        assertEquals("9", order.productId)
        assertEquals("빈티지 스니커즈", order.title)
        assertEquals("2026-09-11T00:00:00Z", order.updatedAt)
    }
}
