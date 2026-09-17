package com.ssafy.dib.domain.order

import com.ssafy.dib.core.network.ApiResult

enum class OrderRole { BUYER, SELLER }

// 주문 상세 응답의 settlement 블록을 화면용으로 줄인 요약
data class OrderSettlementSummary(
    val settlementId: String? = null,
    val netAmount: Long? = null,
    val payoutAt: String? = null
)

data class OrderSummary(
    val orderId: String,
    val auctionId: String,
    val productId: String,
    val title: String,
    val finalPrice: Int,
    val status: String,
    val updatedAt: String?,
    val paymentId: String? = null,
    val chattingReadOnly: Boolean = false,
    val thumbnailUrl: String? = null,
    val paymentDue: String? = null,
    val settlement: OrderSettlementSummary? = null
)

fun isOrderChatWritable(status: String?, serverReadOnly: Boolean = false): Boolean =
    !serverReadOnly && status?.uppercase() !in setOf("CONFIRMED", "CANCELLED", "CANCELED", "REFUNDED")

data class OrderShipment(
    val orderId: String,
    val trackingNumber: String,
    val status: String,
    val carrierStatus: String?,
    val lastCheckedAt: String?,
    val isStale: Boolean,
    val updatedAt: String?,
    val carrier: String? = null
)

data class ShippingCarrier(val code: String, val name: String)

data class OrderMessage(
    val chattingId: String,
    val memberId: String,
    val content: String,
    val time: String,
    val memberNickname: String? = null
)

data class OrderMessagePage(
    val items: List<OrderMessage>,
    val hasMore: Boolean
)

data class OrderPage(
    val items: List<OrderSummary>,
    val nextCursor: String?,
    val hasNext: Boolean
)

data class OrderOfferAcceptance(val order: OrderSummary, val paymentResult: String)

data class OrderShippingAddress(
    val name: String,
    val postalCode: String,
    val address: String,
    val phoneNumber: String? = null
) {
    // 서버는 배송지 미입력 주문에 address: null 을 주고, 그걸 빈 값으로 매핑한다
    val isRegistered: Boolean get() = address.isNotBlank()
}

// 구매자가 결제(PAID) 후 한 번 입력하는 주문 배송지
data class OrderAddressInput(
    val zip: String,
    val address: String,
    val detail: String?,
    val receiverName: String,
    val receiverPhone: String
)

interface OrderRepository {
    fun getOrders(role: OrderRole, cursor: String? = null, size: Int = 30): ApiResult<OrderPage>
    fun getOrder(orderId: String): ApiResult<OrderSummary>
    fun getShipment(orderId: String): ApiResult<OrderShipment>
    fun getShippingAddress(orderId: String): ApiResult<OrderShippingAddress>
    fun updateShippingAddress(orderId: String, input: OrderAddressInput): ApiResult<OrderShippingAddress>
    fun getShippingCarriers(): ApiResult<List<ShippingCarrier>>
    fun registerShipment(orderId: String, carrier: String, trackingNumber: String, idempotencyKey: String): ApiResult<OrderShipment>
    fun getMessages(orderId: String, beforeChattingId: String? = null, size: Int = 50): ApiResult<OrderMessagePage>
    fun confirmPurchase(orderId: String): ApiResult<String>
    fun acceptRunnerUpOffer(auctionId: String): ApiResult<OrderOfferAcceptance>
}
