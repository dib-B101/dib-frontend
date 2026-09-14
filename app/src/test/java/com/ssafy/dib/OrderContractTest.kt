package com.ssafy.dib

import com.ssafy.dib.core.network.DibJson
import com.ssafy.dib.data.remote.order.OrderListResponse
import com.ssafy.dib.data.remote.order.ShipmentRegistrationRequest
import com.ssafy.dib.data.remote.order.ShipmentResponse
import com.ssafy.dib.data.remote.order.OrderMessageListResponse
import com.ssafy.dib.data.remote.order.OrderShippingAddressResponse
import com.ssafy.dib.data.repository.toDomain
import kotlinx.serialization.json.JsonPrimitive
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

    @Test
    fun keepsPaymentIdFromOrderDetailJoin() {
        val response = DibJson.instance.decodeFromString(
            com.ssafy.dib.data.remote.order.OrderSummaryDto.serializer(),
            """{"order":{"orderId":"order-1","finalPrice":35000,"status":"PAID"},"payment":{"paymentId":19,"orderId":"order-1","amount":35000,"type":"CARD"}}"""
        ).toDomain()

        assertEquals("19", response.paymentId)
    }

    @Test
    fun shipmentRegistrationPayloadContainsTrackingNumber() {
        val encoded = DibJson.instance.encodeToString(
            ShipmentRegistrationRequest.serializer(),
            ShipmentRegistrationRequest("123456789012")
        )

        assertEquals("{\"trackingNumber\":\"123456789012\"}", encoded)
    }

    @Test
    fun mapsShipmentStatusReturnedByDeliveryLookup() {
        val shipment = ShipmentResponse(
            orderId = JsonPrimitive(12),
            trackingNumber = "123456789012",
            status = "SHIPPED",
            carrierStatus = "IN_TRANSIT",
            lastCheckedAt = "2026-09-13T08:00:00Z",
            isStale = true
        ).toDomain()

        assertEquals("12", shipment.orderId)
        assertEquals("123456789012", shipment.trackingNumber)
        assertEquals("IN_TRANSIT", shipment.carrierStatus)
        assertEquals(true, shipment.isStale)
    }

    @Test
    fun orderMessageHistoryAcceptsChatContract() {
        val response = DibJson.instance.decodeFromString(
            OrderMessageListResponse.serializer(),
            """{"items":[{"chattingId":"chat-2","memberId":17,"content":"내일 발송할게요","time":"2026-09-13T08:00:00Z"}],"hasMore":false}"""
        )

        assertEquals("chat-2", response.items.single().chattingId.toString().trim('"'))
        assertEquals("17", response.items.single().memberId.toString())
        assertEquals("내일 발송할게요", response.items.single().content)
    }

    @Test
    fun shippingAddressSnapshotMapsDocumentedFields() {
        val response = DibJson.instance.decodeFromString(
            OrderShippingAddressResponse.serializer(),
            """{"address":{"addressId":4,"number":"06236","address":"서울특별시 강남구 테헤란로 212","name":"회사","apiAddressId":"road-8821"}}"""
        ).toDomain()

        assertEquals("회사", response.name)
        assertEquals("06236", response.postalCode)
        assertEquals("서울특별시 강남구 테헤란로 212", response.address)
    }
}
