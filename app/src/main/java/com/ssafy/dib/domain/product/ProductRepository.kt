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

data class ProductUpdate(
    val title: String,
    val description: String,
    val categoryId: String,
    val condition: String,
    val modelName: String?,
    val releaseYear: Int?,
    val marketPrice: Long?,
    val replacementImages: List<ProductImageUpload>? = null
)

data class ProductUpdateResult(val productId: String, val status: String, val thumbnailUrl: String?, val updatedAt: String)

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

data class RegisteredProduct(
    val productId: String,
    val title: String,
    val condition: String,
    val status: String,
    val thumbnailUrl: String?
)

data class RegisteredProductPage(
    val items: List<RegisteredProduct>,
    val nextCursor: String?,
    val hasNext: Boolean
)

interface ProductRepository {
    fun getCategories(): ApiResult<List<ProductCategory>>
    fun getMyProducts(status: String? = null, cursor: String? = null, size: Int = 30): ApiResult<RegisteredProductPage>
    fun getProduct(productId: String): ApiResult<ProductDetail>
    fun searchProducts(query: String, categoryId: String? = null, size: Int = 100): ApiResult<List<RegisteredProduct>>
    fun registerProduct(registration: ProductRegistration): ApiResult<ProductRegistrationResult>
    fun updateProduct(productId: String, update: ProductUpdate): ApiResult<ProductUpdateResult>
    fun deleteProduct(productId: String): ApiResult<Unit>
}
