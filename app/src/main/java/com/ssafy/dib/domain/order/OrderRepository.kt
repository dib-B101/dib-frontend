package com.ssafy.dib.domain.order

import com.ssafy.dib.core.network.ApiResult

enum class OrderRole { BUYER, SELLER }

data class OrderSummary(
    val orderId: String,
    val auctionId: String,
    val productId: String,
    val title: String,
    val finalPrice: Int,
    val status: String,
    val updatedAt: String?
)

data class OrderShipment(
    val orderId: String,
    val trackingNumber: String,
    val status: String,
    val carrierStatus: String?,
    val lastCheckedAt: String?,
    val isStale: Boolean,
    val updatedAt: String?
)

data class OrderMessage(val chattingId: String, val memberId: String, val content: String, val time: String)

data class OrderShippingAddress(
    val name: String,
    val postalCode: String,
    val address: String
)

interface OrderRepository {
    fun getOrders(role: OrderRole, size: Int = 30): ApiResult<List<OrderSummary>>
    fun getOrder(orderId: String): ApiResult<OrderSummary>
    fun getShipment(orderId: String): ApiResult<OrderShipment>
    fun getShippingAddress(orderId: String): ApiResult<OrderShippingAddress>
    fun registerShipment(orderId: String, trackingNumber: String, idempotencyKey: String): ApiResult<OrderShipment>
    fun getMessages(orderId: String, beforeChattingId: String? = null, size: Int = 50): ApiResult<List<OrderMessage>>
    fun confirmPurchase(orderId: String): ApiResult<String>
}
