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

interface ProductRepository {
    fun getCategories(): ApiResult<List<ProductCategory>>
    fun registerProduct(registration: ProductRegistration): ApiResult<ProductRegistrationResult>
}
