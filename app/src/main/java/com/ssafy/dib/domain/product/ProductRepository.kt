package com.ssafy.dib.domain.product

import com.ssafy.dib.core.network.ApiResult

data class ProductCategory(val categoryId: String, val name: String)

data class ProductImageUpload(
    val fileName: String,
    val mediaType: String,
    val bytes: ByteArray,
    val type: String
)

data class ProductRegistration(
    val title: String,
    val description: String,
    val categoryId: String,
    val condition: String,
    val images: List<ProductImageUpload>
)

data class ProductRegistrationResult(
    val productId: String,
    val status: String,
    val thumbnailUrl: String?,
    val createdAt: String
)

data class ProductDetail(
    val productId: String,
    val memberId: String,
    val categoryId: String,
    val title: String,
    val description: String,
    val condition: String,
    val modelName: String?,
    val releaseYear: Int?,
    val marketPrice: Long?,
    val thumbnailUrl: String?,
    val status: String,
    val imageUrls: List<String>,
    val sellerNickname: String?,
    val sellerRating: Double?,
    val sellerTradeCount: Int?
)

interface ProductRepository {
    fun getCategories(): ApiResult<List<ProductCategory>>
    fun getProduct(productId: String): ApiResult<ProductDetail>
    fun registerProduct(registration: ProductRegistration): ApiResult<ProductRegistrationResult>
}
