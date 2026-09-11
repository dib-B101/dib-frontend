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
    val condition: String
)

@Serializable
data class ProductCreateResponse(
    val productId: JsonElement,
    val status: String,
    val thumbnailUrl: String? = null,
    val createdAt: String
)

@Serializable
data class ProductDetailResponse(val product: ProductDetailDto, val sellerSummary: ProductSellerSummaryDto? = null)

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
    val images: List<JsonElement> = emptyList()
)

@Serializable
data class ProductSellerSummaryDto(
    val nickname: String? = null,
    val rating: Double? = null,
    val tradeCount: Int? = null,
    val completedTradeCount: Int? = null
)
