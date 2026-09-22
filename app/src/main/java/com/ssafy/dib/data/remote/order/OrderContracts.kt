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
    val paymentDue: String? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null,
    val order: OrderCoreDto? = null,
    val auction: OrderAuctionDto? = null,
    val product: OrderProductDto? = null,
    val payment: OrderPaymentDto? = null,
    val settlement: OrderSettlementDto? = null,
    val chattingReadOnly: Boolean? = null,
    val heldAt: String? = null,
    val holdReportId: JsonElement? = null,
    // 이 거래에 남긴 별점(0~5). null 이면 아직 평가 전
    val myRating: Int? = null
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
    val paymentDue: String? = null,
    val updatedAt: String? = null,
    val createdAt: String? = null,
    val heldAt: String? = null,
    val holdReportId: JsonElement? = null,
    // 구매 거래 상세의 판매자 줄. 예전 서버 응답에는 없던 값이라 null 허용
    val sellerId: JsonElement? = null,
    val sellerNickname: String? = null,
    val sellerProfileImageUrl: String? = null
)

// 주문 상세 응답의 settlement 블록. 확정(CONFIRMED) 전에는 서버가 null 을 준다
@Serializable
data class OrderSettlementDto(
    val settlementId: JsonElement? = null,
    val netAmount: Long? = null,
    val payoutAt: String? = null
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
    val name: String? = null,
    val thumbnailUrl: String? = null
)

@Serializable
data class OrderConfirmationResponse(
    val orderId: JsonElement,
    val status: String
)

@Serializable
data class OrderOfferAcceptanceResponse(
    val order: OrderSummaryDto,
    val paymentResult: String
)

@Serializable
data class ShipmentRegistrationRequest(val carrier: String, val trackingNumber: String)

// POST /orders/{orderId}/review — 별점만. 코멘트 필드는 일부러 없다
@Serializable
data class WriteReviewRequest(val rating: Int)

// PATCH /orders/{orderId}/address - detail 을 뺀 나머지는 서버에서 @NotBlank
@Serializable
data class UpdateOrderAddressRequest(
    val zip: String,
    val address: String,
    val detail: String? = null,
    val receiverName: String,
    val receiverPhone: String
)

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
