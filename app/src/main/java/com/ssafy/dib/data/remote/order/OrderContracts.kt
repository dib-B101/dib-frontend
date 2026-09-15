package com.ssafy.dib.data.remote.order

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OrderListResponse(
    val items: List<OrderSummaryDto> = emptyList(),
    val nextCursor: String? = null,
    val hasNext: Boolean = false
)

@Serializable
data class OrderSummaryDto(
    val orderId: JsonElement? = null,
    val auctionId: JsonElement? = null,
    val productId: JsonElement? = null,
    val finalPrice: Long? = null,
    val amount: Long? = null,
    val status: String? = null,
    val productTitle: String? = null,
    val thumbnailUrl: String? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null,
    val order: OrderCoreDto? = null,
    val auction: OrderAuctionDto? = null,
    val product: OrderProductDto? = null,
    val payment: OrderPaymentDto? = null,
    val chattingReadOnly: Boolean? = null
)

@Serializable
data class OrderPaymentDto(
    val paymentId: JsonElement? = null,
    val orderId: JsonElement? = null,
    val amount: Long? = null,
    val type: String? = null,
    val receiptUrl: String? = null,
    val paidAt: String? = null
)

@Serializable
data class OrderCoreDto(
    val orderId: JsonElement? = null,
    val auctionId: JsonElement? = null,
    val productId: JsonElement? = null,
    val finalPrice: Long? = null,
    val status: String? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null
)

@Serializable
data class OrderAuctionDto(
    val auctionId: JsonElement? = null,
    val productId: JsonElement? = null,
    val title: String? = null
)

@Serializable
data class OrderProductDto(
    val productId: JsonElement? = null,
    val title: String? = null,
    val name: String? = null
)

@Serializable
data class OrderConfirmationResponse(
    val orderId: JsonElement,
    val status: String
)

@Serializable
data class ShipmentRegistrationRequest(val carrier: String, val trackingNumber: String)

@Serializable
data class CarrierDto(val code: String, val name: String? = null)

@Serializable
data class ShipmentResponse(
    val orderId: JsonElement,
    val trackingNumber: String,
    val carrier: String? = null,
    val status: String,
    val carrierStatus: String? = null,
    val lastCheckedAt: String? = null,
    val isStale: Boolean = false,
    val updatedAt: String? = null
)

@Serializable
data class OrderShippingAddressResponse(val address: JsonElement)

@Serializable
data class OrderMessageListResponse(val items: List<OrderMessageDto> = emptyList(), val hasMore: Boolean = false)

@Serializable
data class OrderMessageDto(
    val chattingId: JsonElement,
    val memberId: JsonElement,
    val memberNickname: String? = null,
    val orderId: JsonElement? = null,
    val content: String,
    val time: String
)
