package com.ssafy.dib.domain.product

import com.ssafy.dib.core.network.ApiResult

data class ProductCategory(val categoryId: String, val name: String)

/** 서버 카테고리를 받지 못했을 때 사용하는 기본 카테고리 목록. ID와 이름은 CategoryScreen과 동일하다. */
val DefaultProductCategories: List<ProductCategory> = listOf(
    ProductCategory("1", "디지털기기"),
    ProductCategory("2", "생활가전"),
    ProductCategory("3", "가구·인테리어"),
    ProductCategory("4", "스포츠·레저"),
    ProductCategory("5", "패션·잡화"),
    ProductCategory("6", "뷰티"),
    ProductCategory("7", "취미·게임"),
    ProductCategory("8", "예술·창작")
)

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
    val modelName: String? = null,
    val releaseYear: Int? = null,
    val marketPrice: Long? = null,
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
    fun getSimilarProducts(productId: String, size: Int = 20): ApiResult<List<RegisteredProduct>>
    fun searchProducts(query: String, categoryId: String? = null, cursor: String? = null, size: Int = 100): ApiResult<RegisteredProductPage>
    fun registerProduct(registration: ProductRegistration, idempotencyKey: String): ApiResult<ProductRegistrationResult>
    fun updateProduct(productId: String, update: ProductUpdate): ApiResult<ProductUpdateResult>
    fun deleteProduct(productId: String, idempotencyKey: String): ApiResult<Unit>
}
