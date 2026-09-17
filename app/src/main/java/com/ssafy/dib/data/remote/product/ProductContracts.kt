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
    val startPrice: Long,
    val auctionTime: Int
)

@Serializable
data class ProductCreateResponse(
    val productId: JsonElement,
    val status: String,
    val thumbnailUrl: String? = null,
    val createdAt: String
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
    val updatedAt: String? = null
)

@Serializable
data class ProductDetailResponse(
    val product: ProductDetailDto,
    val sellerSummary: ProductSellerSummaryDto? = null
)

@Serializable
data class ProductDetailDto(
    val productId: JsonElement,
    val memberId: JsonElement,
    val categoryId: JsonElement,
    val title: String,
    val description: String = "",
    val condition: String = "",
    val modelName: String? = null,
    val releaseYear: Int? = null,
    val marketPrice: Long? = null,
    val thumbnailUrl: String? = null,
    val status: String = "",
    val images: List<JsonElement> = emptyList(),
    val nickname: String? = null
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
    val productId: JsonElement,
    val categoryId: JsonElement? = null,
    val title: String? = null,
    val name: String? = null,
    val condition: String = "",
    val status: String = "",
    val productStatus: String? = null,
    val thumbnailUrl: String? = null,
    val auctionId: JsonElement? = null,
    val startPrice: Long? = null,
    val auctionTime: Long? = null,
    val auctionStatus: String? = null
)

@Serializable
data class ProductImageDto(
    val productImageId: JsonElement? = null,
    val productId: JsonElement? = null,
    val imageUrl: String,
    val sequence: Int = 0
)
