package com.ssafy.dib.data.remote.product

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CategoryListResponse(val items: List<CategoryDto> = emptyList())

@Serializable
data class CategoryDto(val categoryId: JsonElement, val name: String)

@Serializable
data class ProductCreatePayload(
    val title: String,
    val description: String,
    val categoryId: JsonElement,
    val condition: String,
    val modelName: String? = null,
    val releaseYear: Int? = null,
    val marketPrice: Long? = null,
    // 가격과 경매 시간은 경매를 시작할 때 정한다. 보내지 않으면 서버가 비워둔다
    val startPrice: Long? = null,
    val auctionTime: Int? = null
)

@Serializable
data class ProductCreateResponse(
    // AI 검수를 켜면 등록 응답은 PENDING 이고 경매(auctionId/startPrice/auctionTime) 가 아예 없다
    val productId: JsonElement? = null,
    val status: String? = null,
    val thumbnailUrl: String? = null,
    val createdAt: String? = null
)

@Serializable
data class ProductUpdatePayload(
    val title: String? = null,
    val description: String? = null,
    val categoryId: JsonElement? = null,
    val condition: String? = null,
    val modelName: String? = null,
    val releaseYear: Int? = null,
    val marketPrice: Long? = null,
    val startPrice: Long? = null,
    val auctionTime: Int? = null
)

@Serializable
data class ProductUpdateResponse(
    // 바뀐 필드가 없으면 서버가 updatedAt 을 채우지 않는다. non-null 로 받으면 "아무것도 안 바꾸고 저장" 이 파싱 실패로 죽는다
    val productId: JsonElement? = null,
    val status: String? = null,
    val thumbnailUrl: String? = null,
    val updatedAt: String? = null,
    val moderationReason: String? = null,
    val moderationStage: String? = null,
    val moderatedAt: String? = null
)

@Serializable
data class ProductDetailResponse(
    val product: ProductDetailDto,
    val sellerSummary: ProductSellerSummaryDto? = null
)

@Serializable
data class ProductDetailDto(
    val productId: JsonElement? = null,
    val memberId: JsonElement? = null,
    val categoryId: JsonElement? = null,
    val title: String,
    val description: String = "",
    val condition: String = "",
    val modelName: String? = null,
    val releaseYear: Int? = null,
    val marketPrice: Long? = null,
    val thumbnailUrl: String? = null,
    val status: String = "",
    val images: List<JsonElement> = emptyList(),
    val nickname: String? = null,
    // AI 검수 결과. moderationReason 은 사용자에게 그대로 보여줄 수 있는 한국어 문장이다
    val moderationReason: String? = null,
    val moderationStage: String? = null,
    val moderatedAt: String? = null
)

@Serializable
data class ProductSellerSummaryDto(
    val nickname: String? = null,
    val rating: Double? = null,
    val tradeCount: Int? = null,
    val completedTradeCount: Int? = null
)

@Serializable data class ProductListResponse(val items: List<ProductCardDto> = emptyList(), val nextCursor: String? = null, val hasNext: Boolean = false)

@Serializable
data class ProductCardDto(
    val productId: JsonElement? = null,
    val categoryId: JsonElement? = null,
    val title: String? = null,
    val name: String? = null,
    val condition: String? = null,
    val status: String? = null,
    val productStatus: String? = null,
    val thumbnailUrl: String? = null,
    // 내 상품 목록은 LEFT JOIN auction 이라 검수 전(PENDING)·거절(REJECTED) 행의 경매 필드가 모두 null 로 내려온다
    val auctionId: JsonElement? = null,
    val startPrice: Long? = null,
    val currentPrice: Long? = null,
    val auctionTime: Long? = null,
    val auctionStatus: String? = null,
    val bidCount: Int? = null,
    val bidderCount: Int? = null
)

@Serializable
data class ProductImageDto(
    val productImageId: JsonElement? = null,
    val productId: JsonElement? = null,
    val imageUrl: String,
    val sequence: Int = 0
)
