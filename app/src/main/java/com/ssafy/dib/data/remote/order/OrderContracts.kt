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
    val updatedAt: String? = null,
    val createdAt: String? = null,
    val order: OrderCoreDto? = null,
    val auction: OrderAuctionDto? = null,
    val product: OrderProductDto? = null
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
